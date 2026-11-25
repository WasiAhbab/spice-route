"use client";
import "../styles/ai-insights.css";
import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { request, Session, ApiError } from "@/lib/api";
import {
  Leaf,
  LayoutDashboard,
  Store,
  QrCode,
  MessageSquare,
  Sparkles,
  Users,
  LogOut,
  Menu,
  Settings,
} from "lucide-react";
import { OverviewPanel } from "./overview-panel";
import { RestaurantPanel } from "./restaurant-panel";
import { TablePanel } from "./table-panel";
import { FeedbackPanel } from "./feedback-panel";
import { AiInsightsPanel } from "./ai-insights-panel";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";
import { SettingsPanel } from "./settings-panel";
import { ManagerPanel } from "./manager-panel";
import type { Row, Overview, Report } from "../types";
const today = () => new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Dhaka" }).format(new Date());
const start = () => {
  const d = new Date();
  d.setDate(d.getDate() - 30);
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Dhaka" }).format(d);
};
export default function Dashboard() {
  const router = useRouter();
  const [session, setSession] = useState<Session | null>(null);
  const [overview, setOverview] = useState<Overview>({ restaurants: [], branches: [] });
  const [directory, setDirectory] = useState<Overview>({ restaurants: [], branches: [] });
  const [branch, setBranch] = useState("");
  const [restaurant, setRestaurant] = useState("");
  const [tab, setTab] = useState("Overview");
  const [tables, setTables] = useState<Row[]>([]);
  const [feedback, setFeedback] = useState<Row[]>([]);
  const [compare, setComparison] = useState<Row[]>([]);
  const [managers, setManagers] = useState<Row[]>([]);
  const [admin, setAdmin] = useState<{ restaurants: Row[]; users: Row[] }>({
    restaurants: [],
    users: [],
  });
  const [from, setFrom] = useState(start);
  const [to, setTo] = useState(today);
  const [rating, setRating] = useState("");
  const [tableFilter, setTableFilter] = useState("");
  const [confirm, setConfirm] = useState<DialogSpec | null>(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [report, setReport] = useState<Report | null>(null);
  const [mobile, setMobile] = useState(false);
  const token = session ? "authenticated" : undefined;
  const fail = useCallback(
    (error: unknown) => {
      if (error instanceof ApiError && error.status === 401) {
        router.replace("/");
        return;
      }
      setError(error instanceof Error ? error.message : "Unexpected request failure");
    },
    [router],
  );
  useEffect(() => {
    let mounted = true;
    request<Session>("/auth/me")
      .then((currentSession) => {
        if (mounted) setSession(currentSession);
      })
      .catch((error: unknown) => {
        if (!mounted) return;
        if (error instanceof ApiError && error.status === 401) router.replace("/");
        else setError(error instanceof Error ? error.message : "Could not open workspace");
      });
    return () => {
      mounted = false;
    };
  }, [router]);
  const load = useCallback(async () => {
    if (!token) return;
    if (session?.role === "ADMIN") {
      setAdmin(await request("/manage/admin", token));
      return;
    }
    const data = await request<Overview>("/manage", token);
    setOverview(data);
    if (session?.role === "OWNER")
      setDirectory(await request<Overview>("/manage/directory", token));
    setRestaurant((old) =>
      data.restaurants.some((x) => x.id === old) ? old : data.restaurants[0]?.id || "",
    );
    setBranch((old) =>
      data.branches.some((x) => x.id === old) ? old : data.branches[0]?.id || "",
    );
    setComparison(await request("/analytics/comparison", token));
  }, [token, session?.role]);
  useEffect(() => {
    load().catch(fail);
  }, [load, fail]);
  useEffect(() => {
    if (!restaurant) {
      setBranch("");
      setTableFilter("");
      return;
    }
    const availableBranches = overview.branches.filter((item) => item.restaurant_id === restaurant);
    if (!availableBranches.some((item) => item.id === branch)) {
      setBranch(availableBranches[0]?.id || "");
      setTableFilter("");
    }
  }, [overview.branches, restaurant, branch]);
  const query = new URLSearchParams({
    branch,
    from,
    to,
    ...(rating ? { rating } : {}),
    ...(tableFilter ? { table: tableFilter } : {}),
  }).toString();
  useEffect(() => {
    if (!token || !branch || session?.role === "ADMIN") {
      setTables([]);
      setManagers([]);
      return;
    }
    let live = true;
    setTables([]);
    setManagers([]);
    request<Row[]>(`/manage/tables/${branch}`, token)
      .then((x) => {
        if (live) setTables(x);
      })
      .catch(fail);
    if (session?.role === "OWNER")
      request<Row[]>(`/manage/managers/${branch}`, token)
        .then((x) => {
          if (live) setManagers(x);
        })
        .catch(fail);
    return () => {
      live = false;
    };
  }, [token, branch, session?.role, fail]);
  useEffect(() => {
    setReport(null);
    setFeedback([]);
    if (!token || !branch) return;
    let live = true;
    request<Row[]>(`/analytics/feedback?${query}`, token)
      .then((x) => {
        if (live) setFeedback(x);
      })
      .catch(fail);
    return () => {
      live = false;
    };
  }, [token, branch, query, fail]);
  const action = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    setError("");
    try {
      await fn();
      setNotice("Operation completed successfully.");
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  };
  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => setNotice(""), 4500);
    return () => clearTimeout(timer);
  }, [notice]);
  const refreshFeedback = async () => {
    if (token && branch) setFeedback(await request<Row[]>(`/analytics/feedback?${query}`, token));
    setNotice("Feedback review saved.");
  };
  const refresh = async () => {
    await load();
    if (branch && token) {
      setTables(await request(`/manage/tables/${branch}`, token));
      if (session?.role === "OWNER")
        setManagers(await request(`/manage/managers/${branch}`, token));
    }
    setNotice("Workspace updated.");
  };
  const add =
    (path: string, extra: object = {}) =>
    async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      const form = e.currentTarget;
      const data = Object.fromEntries(new FormData(form));
      await action(async () => {
        await request("/manage/" + path, token, "POST", { ...data, ...extra });
        form.reset();
        if (path === "restaurants" || path === "branches") await load();
        else await refresh();
      });
    };
  if (!session)
    return (
      <main className="center">
        {error ? (
          <div role="alert">
            {error} <button onClick={() => window.location.reload()}>Retry</button>
          </div>
        ) : (
          "Opening workspace…"
        )}
      </main>
    );
  const owner = session.role === "OWNER",
    isAdmin = session.role === "ADMIN";
  const nav = isAdmin
    ? ([
        ["Platform", Store],
        ["Settings", Settings],
      ] as const)
    : ([
        ["Overview", LayoutDashboard],
        ["Restaurants", Store],
        ["Tables & QR", QrCode],
        ["Feedback", MessageSquare],
        ["AI insights", Sparkles],
        ...(owner ? [["Managers", Users] as const] : []),
        ["Settings", Settings],
      ] as const);
  const active = isAdmin && tab !== "Settings" ? "Platform" : tab;
  return (
    <div className="shell">
      <aside className={mobile ? "sidebar open" : "sidebar"}>
        <div className="brand">
          <Leaf /> spice route
        </div>
        <p className="eyebrow">WORKSPACE</p>
        {nav.map(([n, Icon]) => (
          <button
            key={n}
            className={active === n ? "nav selected" : "nav"}
            onClick={() => {
              setTab(n);
              setMobile(false);
              setError("");
            }}
          >
            <Icon size={19} />
            {n}
          </button>
        ))}
        <div className="side-bottom">
          <div className="avatar">{session.name.slice(0, 1)}</div>
          <div>
            <strong>{session.name}</strong>
            <small>{session.role.toLowerCase()}</small>
          </div>
          <button
            className="icon-btn"
            aria-label="Sign out"
            onClick={() =>
              action(async () => {
                try {
                  await request("/auth/logout", token, "POST");
                } finally {
                  router.replace("/");
                }
              })
            }
          >
            <LogOut size={18} />
          </button>
        </div>
      </aside>
      <main className="workspace">
        <header className="top">
          <button className="icon-btn mobile" onClick={() => setMobile(!mobile)} aria-label="Menu">
            <Menu />
          </button>
          <span>Workspace / {active}</span>
          <span className="tag">{session.role}</span>
        </header>
        <div className="content">
          <p className="eyebrow">GUEST EXPERIENCE / {active.toUpperCase()}</p>
          <h1>{active === "Overview" ? "Your guest experience." : active}</h1>
          <p className="muted">
            {active === "Overview"
              ? "A closer look at how your guests feel."
              : "Manage your workspace with confidence."}
          </p>
          {notice && (
            <div className="success-notice" role="status">
              {notice}
              <button className="link" onClick={() => setNotice("")}>
                Dismiss
              </button>
            </div>
          )}
          {error && (
            <div role="alert" className="error">
              {error}
              <button className="link" onClick={() => setError("")}>
                Dismiss
              </button>
            </div>
          )}
          {!isAdmin && active !== "Settings" && active !== "Restaurants" && (
            <div className="filters">
              <label>
                Restaurant
                <select
                  value={restaurant}
                  disabled={busy}
                  onChange={(e) => {
                    setRestaurant(e.target.value);
                    setBranch(
                      overview.branches.find((x) => x.restaurant_id === e.target.value)?.id || "",
                    );
                    setTableFilter("");
                  }}
                >
                  <option value="">Select restaurant</option>
                  {overview.restaurants.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Branch
                <select
                  value={branch}
                  disabled={busy}
                  onChange={(e) => {
                    setBranch(e.target.value);
                    setTableFilter("");
                  }}
                >
                  <option value="">Select branch</option>
                  {overview.branches
                    .filter((b) => b.restaurant_id === restaurant)
                    .map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name}
                      </option>
                    ))}
                </select>
              </label>
              {["Overview", "Feedback", "AI insights"].includes(active) && (
                <>
                  <label>
                    From
                    <input
                      type="date"
                      disabled={busy}
                      value={from}
                      onChange={(e) => setFrom(e.target.value)}
                    />
                  </label>
                  <label>
                    To
                    <input
                      type="date"
                      disabled={busy}
                      value={to}
                      onChange={(e) => setTo(e.target.value)}
                    />
                  </label>
                </>
              )}
              {["Overview", "Feedback"].includes(active) && (
                <>
                  <label>
                    Rating
                    <select value={rating} onChange={(e) => setRating(e.target.value)}>
                      <option value="">All ratings</option>
                      {[1, 2, 3, 4, 5].map((x) => (
                        <option key={x}>{x}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Table
                    <select value={tableFilter} onChange={(e) => setTableFilter(e.target.value)}>
                      <option value="">All tables</option>
                      {tables.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.label}
                        </option>
                      ))}
                    </select>
                  </label>
                </>
              )}
            </div>
          )}
          {active === "Overview" && <OverviewPanel feedback={feedback} compare={compare} />}
          {active === "Restaurants" && (
            <RestaurantPanel
              overview={owner ? directory : overview}
              refresh={async () => {
                await load();
                setNotice("Changes saved.");
              }}
              restaurant={restaurant}
              owner={owner}
              busy={busy}
              add={add}
            />
          )}
          {active === "Tables & QR" && (
            <TablePanel
              tables={tables}
              branch={branch}
              token={token!}
              busy={busy}
              add={add}
              action={action}
              refresh={refresh}
            />
          )}
          {active === "Feedback" && (
            <FeedbackPanel
              key={branch}
              feedback={feedback}
              branch={branch}
              refresh={refreshFeedback}
            />
          )}
          {active === "AI insights" && (
            <AiInsightsPanel
              report={report}
              busy={busy}
              branch={branch}
              onGenerate={() => {
                void action(async () => {
                  setReport(
                    await request<Report>(
                      `/analytics/summary?${new URLSearchParams({ branch, from, to })}`,
                      token,
                      "POST",
                    ),
                  );
                });
              }}
            />
          )}
          {active === "Managers" && (
            <ManagerPanel
              managers={managers}
              branches={overview.branches}
              branch={branch}
              busy={busy}
              add={add}
              refresh={refresh}
            />
          )}
          {active === "Settings" && (
            <SettingsPanel
              session={session}
              onProfile={(updated) => {
                setSession(updated);
                setNotice("Profile saved.");
              }}
              onSignedOut={() => router.replace("/")}
            />
          )}
          {isAdmin && active === "Platform" && (
            <>
              {(["restaurants", "users"] as const).map((kind) => (
                <section className="panel" key={kind}>
                  <h3>
                    {kind} · {admin[kind].length}
                  </h3>
                  {admin[kind].map((r) => (
                    <div className="record" key={r.id}>
                      <strong>{r.name}</strong>
                      <span>{r.email}</span>
                      <span className="tag">{r.active ? "Active" : "Suspended"}</span>
                      <button
                        className="secondary"
                        disabled={busy}
                        onClick={() =>
                          setConfirm({
                            title: `${r.active ? "Suspend" : "Activate"} ${r.name}?`,
                            description: r.active
                              ? "Access will be blocked. Existing records will be retained."
                              : "Restore platform access for this record.",
                            destructive: !!r.active,
                            submitLabel: r.active ? "Suspend" : "Activate",
                            submit: async () => {
                              await request(`/manage/admin/${kind}/${r.id}`, token, "PATCH");
                              await load();
                              setNotice("Platform status updated.");
                            },
                          })
                        }
                      >
                        {r.active ? "Suspend" : "Activate"}
                      </button>
                    </div>
                  ))}
                </section>
              ))}
            </>
          )}
        </div>
      </main>
      {confirm && <ResourceDialog spec={confirm} close={() => setConfirm(null)} />}
    </div>
  );
}
