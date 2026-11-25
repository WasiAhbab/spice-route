"use client";
import { useState, type FormEvent } from "react";
import { Download, Pencil, QrCode, RefreshCw, Search } from "lucide-react";
import { request, download } from "@/lib/api";
import type { Row } from "../types";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";

type AddHandler = (
  path: string,
  extra?: object,
) => (event: FormEvent<HTMLFormElement>) => Promise<void>;
type ActionHandler = (work: () => Promise<unknown>) => Promise<void>;

export function TablePanel({
  tables,
  branch,
  token,
  busy,
  add,
  action,
  refresh,
}: {
  tables: Row[];
  branch: string;
  token: string;
  busy: boolean;
  add: AddHandler;
  action: ActionHandler;
  refresh: () => Promise<void>;
}) {
  const [search, setSearch] = useState("");
  const [dialog, setDialog] = useState<DialogSpec | null>(null);
  const filtered = tables.filter((t) =>
    (t.label || "").toLowerCase().includes(search.trim().toLowerCase()),
  );
  function edit(t: Row) {
    setDialog({
      title: "Edit table",
      description: "Renaming preserves the QR code and all existing responses.",
      submitLabel: "Save changes",
      fields: (
        <label>
          Table label
          <input name="name" defaultValue={t.label} maxLength={120} required />
        </label>
      ),
      submit: async (data) => {
        await request(`/manage/tables/${t.id}`, token, "PATCH", { name: data.get("name") });
        await refresh();
      },
    });
  }
  function confirm(t: Row, rotate: boolean) {
    setDialog({
      title: rotate
        ? `Regenerate QR for ${t.label}?`
        : `${t.active ? "Disable" : "Enable"} ${t.label}?`,
      description: rotate
        ? "The existing printed QR code will stop working immediately. Download and replace every old copy. Historical feedback will be preserved."
        : t.active
          ? "Guests will no longer be able to submit feedback from this table. Existing responses will be retained."
          : "The existing QR code will accept new feedback again.",
      destructive: rotate || !!t.active,
      submitLabel: rotate ? "Regenerate QR" : t.active ? "Disable table" : "Enable table",
      submit: async () => {
        if (rotate) await request(`/manage/tables/${t.id}/rotate-qr`, token, "POST");
        else await request(`/manage/tables/${t.id}`, token, "PATCH", { active: !t.active });
        await refresh();
      },
    });
  }
  return (
    <section className="panel directory-panel">
      <div className="section-head">
        <div>
          <h3>Table directory</h3>
          <p className="muted">Manage labels and printable guest feedback links.</p>
        </div>
        <button
          className="secondary"
          disabled={!branch || busy || !tables.some((t) => t.active)}
          onClick={() => action(() => download(`/qr/branch/${branch}`, token, "table-codes.pdf"))}
        >
          <Download size={16} /> Bulk PDF
        </button>
      </div>
      <form className="inline-form" onSubmit={add("tables", { branchId: branch })}>
        <label>
          Table label
          <input name="name" placeholder="T-01" required maxLength={120} />
        </label>
        <button disabled={!branch || busy}>Add table</button>
      </form>
      <label className="search-field">
        <Search size={17} />
        <input
          aria-label="Search tables"
          placeholder="Search tables"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </label>
      {!filtered.length && (
        <div className="directory-empty">
          <QrCode size={28} />
          <strong>{branch ? "No matching tables" : "Choose a branch"}</strong>
          <p>Add a table to generate its unique QR code.</p>
        </div>
      )}
      {filtered.map((t) => (
        <article className="directory-row" key={t.id}>
          <div className="directory-main">
            <span className="directory-icon">
              <QrCode size={21} />
            </span>
            <span>
              <strong>{t.label}</strong>
              <small>{t.active ? "Accepting feedback" : "Disabled"}</small>
            </span>
          </div>
          <div className="directory-actions">
            {t.active && (
              <a
                className="resource-link"
                href={`/feedback/${t.token}`}
                target="_blank"
                rel="noreferrer"
              >
                Preview
              </a>
            )}
            <button
              className="secondary"
              disabled={busy || !t.active}
              onClick={() =>
                action(() => download(`/qr/table/${t.id}`, token, `table-${t.label}.png`))
              }
            >
              <Download size={15} /> PNG
            </button>
            <button className="secondary" disabled={busy} onClick={() => edit(t)}>
              <Pencil size={15} /> Edit
            </button>
            <button className="secondary" disabled={busy} onClick={() => confirm(t, false)}>
              {t.active ? "Disable" : "Enable"}
            </button>
            <button className="secondary" disabled={busy} onClick={() => confirm(t, true)}>
              <RefreshCw size={15} /> New QR
            </button>
          </div>
        </article>
      ))}
      {dialog && <ResourceDialog spec={dialog} close={() => setDialog(null)} />}
    </section>
  );
}
