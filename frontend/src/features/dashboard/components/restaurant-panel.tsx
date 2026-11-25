"use client";

import { useState, type FormEvent } from "react";
import { Archive, Building2, MapPin, Pencil, Plus, RotateCcw, Search } from "lucide-react";
import { request } from "@/lib/api";
import type { Overview, Row } from "../types";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";

type AddHandler = (
  path: string,
  extra?: object,
) => (event: FormEvent<HTMLFormElement>) => Promise<void>;

export function RestaurantPanel({
  overview,
  restaurant,
  owner,
  busy,
  add,
  refresh,
}: {
  overview: Overview;
  restaurant: string;
  owner: boolean;
  busy: boolean;
  add: AddHandler;
  refresh: () => Promise<void>;
}) {
  const [selected, setSelected] = useState("");
  const [search, setSearch] = useState("");
  const [archived, setArchived] = useState(false);
  const [dialog, setDialog] = useState<DialogSpec | null>(null);
  const matches = (row: Row) => row.name.toLowerCase().includes(search.trim().toLowerCase());
  const visible = (row: Row) => archived || !row.archived;
  const restaurants = overview.restaurants.filter((r) => visible(r) && matches(r));
  const chosen =
    restaurants.find((r) => r.id === selected) ||
    restaurants.find((r) => r.id === restaurant) ||
    restaurants[0];

  function edit(row: Row, type: "restaurants" | "branches") {
    const branch = type === "branches";
    setDialog({
      title: `Edit ${branch ? "branch" : "restaurant"}`,
      description: "Update the details shown in your workspace and guest feedback form.",
      submitLabel: "Save changes",
      fields: (
        <>
          <label>
            Name
            <input name="name" required maxLength={120} defaultValue={row.name} />
          </label>
          {branch && (
            <label>
              Address
              <input name="address" maxLength={300} defaultValue={row.address || ""} />
            </label>
          )}
        </>
      ),
      submit: async (data) => {
        await request(`/manage/${type}/${row.id}`, undefined, "PATCH", Object.fromEntries(data));
        await refresh();
      },
    });
  }
  function branding(row: Row) {
    setDialog({
      title: "Guest feedback settings",
      description:
        "Customize messages shown to guests in every language. Leave them empty to use the built-in translated text. Contact collection remains optional for guests.",
      submitLabel: "Save feedback settings",
      fields: (
        <>
          <label>
            Welcome message
            <textarea
              name="welcome"
              rows={3}
              maxLength={240}
              defaultValue={row.welcome_message || ""}
            />
          </label>
          <label>
            Thank-you message
            <textarea
              name="thanks"
              rows={3}
              maxLength={240}
              defaultValue={row.thank_you_message || ""}
            />
          </label>
          <label className="check-inline">
            <input name="contact" type="checkbox" defaultChecked={row.collect_contact !== false} />{" "}
            Allow optional name and phone fields
          </label>
        </>
      ),
      submit: async (data) => {
        await request(`/manage/restaurants/${row.id}/branding`, undefined, "PATCH", {
          welcomeMessage: data.get("welcome"),
          thankYouMessage: data.get("thanks"),
          collectContact: data.has("contact"),
        });
        await refresh();
      },
    });
  }
  function archive(row: Row, type: "restaurants" | "branches") {
    const restoring = !!row.archived;
    const kind = type === "branches" ? "branch" : "restaurant";
    setDialog({
      title: `${restoring ? "Restore" : "Archive"} ${row.name}?`,
      description: restoring
        ? `Return this ${kind} to the active workspace. Previously disabled tables remain disabled. A suspended restaurant must be reactivated by the platform administrator.`
        : `This ${kind} will leave the active workspace and its QR links will stop accepting feedback. Existing feedback, tables, reports and manager assignments will be retained. You can restore it later.`,
      submitLabel: restoring ? "Restore" : "Archive",
      destructive: !restoring,
      submit: async () => {
        await request(`/manage/${type}/${row.id}`, undefined, "PATCH", { archived: !restoring });
        await refresh();
      },
    });
  }
  const branches = overview.branches.filter((b) => b.restaurant_id === chosen?.id && visible(b));
  return (
    <div className="management-area">
      <div className="directory-toolbar">
        <label className="search-field">
          <Search size={17} />
          <input
            aria-label="Search restaurants"
            placeholder="Search restaurants"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
        {owner && (
          <label className="check-inline">
            <input
              type="checkbox"
              checked={archived}
              onChange={(e) => setArchived(e.target.checked)}
            />{" "}
            Include archived
          </label>
        )}
      </div>
      <div className="charts">
        <section className="panel directory-panel">
          <div className="section-head">
            <h3>Restaurants</h3>
            <span className="tag">{restaurants.length}</span>
          </div>
          {restaurants.length === 0 && (
            <div className="directory-empty">
              <Building2 size={28} />
              <strong>No restaurants found</strong>
              <p>Try another search or add a restaurant to begin.</p>
            </div>
          )}
          {restaurants.map((r) => (
            <article
              className={`directory-row ${chosen?.id === r.id ? "directory-selected" : ""}`}
              key={r.id}
            >
              <button
                type="button"
                className="directory-main"
                onClick={() => setSelected(r.id)}
                aria-pressed={chosen?.id === r.id}
              >
                <span className="directory-icon">
                  <Building2 size={20} />
                </span>
                <span>
                  <strong>{r.name}</strong>
                  <small>
                    {
                      overview.branches.filter((b) => b.restaurant_id === r.id && !b.archived)
                        .length
                    }{" "}
                    branches ·{" "}
                    {r.archived ? "Archived" : r.active === false ? "Suspended" : "Active"}
                  </small>
                </span>
              </button>
              {owner && (
                <div className="directory-actions">
                  <button
                    type="button"
                    className="secondary"
                    disabled={busy}
                    onClick={() => edit(r, "restaurants")}
                  >
                    <Pencil size={15} /> Edit
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    disabled={busy}
                    onClick={() => branding(r)}
                  >
                    Guest form
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    disabled={busy}
                    onClick={() => archive(r, "restaurants")}
                  >
                    {r.archived ? <RotateCcw size={15} /> : <Archive size={15} />}{" "}
                    {r.archived ? "Restore" : "Archive"}
                  </button>
                </div>
              )}
            </article>
          ))}
          {owner && (
            <form className="directory-add" onSubmit={add("restaurants")}>
              <label>
                Restaurant name
                <input name="name" required maxLength={120} />
              </label>
              <button disabled={busy}>
                <Plus size={16} /> Add restaurant
              </button>
            </form>
          )}
        </section>
        <section className="panel directory-panel">
          <div className="section-head">
            <div>
              <h3>Branches</h3>
              <p className="muted">{chosen?.name || "Choose a restaurant"}</p>
            </div>
            <span className="tag">{branches.length}</span>
          </div>
          {!branches.length && (
            <div className="directory-empty">
              <MapPin size={28} />
              <strong>No branches to display</strong>
              <p>Select a restaurant or create its first branch.</p>
            </div>
          )}
          {branches.map((b) => (
            <article className="directory-row" key={b.id}>
              <div className="directory-main">
                <span className="directory-icon">
                  <MapPin size={20} />
                </span>
                <span>
                  <strong>{b.name}</strong>
                  <small>
                    {b.archived ? "Archived · " : ""}
                    {b.address || "Address not added"}
                  </small>
                </span>
              </div>
              {owner && (
                <div className="directory-actions">
                  <button className="secondary" disabled={busy} onClick={() => edit(b, "branches")}>
                    <Pencil size={15} /> Edit
                  </button>
                  <button
                    className="secondary"
                    disabled={busy}
                    onClick={() => archive(b, "branches")}
                  >
                    {b.archived ? <RotateCcw size={15} /> : <Archive size={15} />}{" "}
                    {b.archived ? "Restore" : "Archive"}
                  </button>
                </div>
              )}
            </article>
          ))}
          {owner && chosen && !chosen.archived && chosen.active !== false && (
            <form
              key={chosen.id}
              className="directory-add"
              onSubmit={add("branches", { restaurantId: chosen.id })}
            >
              <label>
                Branch name
                <input name="name" required maxLength={120} />
              </label>
              <label>
                Address
                <input name="address" maxLength={300} />
              </label>
              <button disabled={busy}>
                <Plus size={16} /> Add branch
              </button>
            </form>
          )}
        </section>
      </div>
      {dialog && <ResourceDialog spec={dialog} close={() => setDialog(null)} />}
    </div>
  );
}
