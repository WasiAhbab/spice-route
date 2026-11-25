"use client";

import { useState, type FormEvent } from "react";

import {
  Download,
  Pencil,
  QrCode,
  RefreshCw,
  Search,
  Eye,
  ExternalLink,
} from "lucide-react";

import { request, download } from "@/lib/api";
import type { Row } from "../types";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";

type AddHandler = (
  path: string,
  extra?: object,
) => (event: FormEvent<HTMLFormElement>) => Promise<void>;

type ActionHandler = (
  work: () => Promise<unknown>,
) => Promise<void>;

type TablePanelProps = {
  tables: Row[];
  branch: string;
  token: string;
  busy: boolean;
  add: AddHandler;
  action: ActionHandler;
  refresh: () => Promise<void>;
};

export function TablePanel({
  tables,
  branch,
  token,
  busy,
  add,
  action,
  refresh,
}: TablePanelProps) {
  const [search, setSearch] = useState("");
  const [dialog, setDialog] = useState<DialogSpec | null>(null);

  const searchQuery = search.trim().toLowerCase();

  const filtered = tables.filter((table) =>
    (table.label || "").toLowerCase().includes(searchQuery),
  );

  const hasActiveTables = tables.some(
    (table) => table.active,
  );

  function edit(table: Row) {
    setDialog({
      title: "Edit table",

      description:
        "Renaming this table preserves its existing QR code and all customer feedback.",

      submitLabel: "Save changes",

      fields: (
        <label>
          Table label

          <input
            name="name"
            defaultValue={table.label}
            maxLength={120}
            autoComplete="off"
            required
          />
        </label>
      ),

      submit: async (data) => {
        const name = String(data.get("name") || "").trim();

        if (!name) {
          throw new Error("Table label cannot be empty.");
        }

        await request(
          `/manage/tables/${table.id}`,
          token,
          "PATCH",
          { name },
        );

        await refresh();
      },
    });
  }

  function confirm(table: Row, rotate: boolean) {
    setDialog({
      title: rotate
        ? `Regenerate QR for ${table.label}?`
        : `${table.active ? "Disable" : "Enable"} ${table.label}?`,

      description: rotate
        ? "The existing printed QR code will stop working immediately. Download and replace every old copy. Historical feedback will be preserved."
        : table.active
          ? "Guests will no longer be able to submit feedback from this table. Existing responses will be retained."
          : "The existing QR code will accept new customer feedback again.",

      destructive: rotate || !!table.active,

      submitLabel: rotate
        ? "Regenerate QR"
        : table.active
          ? "Disable table"
          : "Enable table",

      submit: async () => {
        if (rotate) {
          await request(
            `/manage/tables/${table.id}/rotate-qr`,
            token,
            "POST",
          );
        } else {
          await request(
            `/manage/tables/${table.id}`,
            token,
            "PATCH",
            {
              active: !table.active,
            },
          );
        }

        await refresh();
      },
    });
  }

  return (
    <section className="panel directory-panel">
      <div className="section-head">
        <div>
          <h3>Table directory</h3>

          <p className="muted">
            Manage tables, preview customer forms and download
            printable QR codes.
          </p>
        </div>

        <button
          type="button"
          className="secondary"
          disabled={!branch || busy || !hasActiveTables}
          onClick={() =>
            action(() =>
              download(
                `/qr/branch/${branch}`,
                token,
                "table-codes.pdf",
              ),
            )
          }
        >
          <Download size={16} />
          Bulk PDF
        </button>
      </div>

      <form
        className="inline-form"
        onSubmit={add("tables", {
          branchId: branch,
        })}
      >
        <label>
          Table label

          <input
            name="name"
            placeholder="T-01"
            required
            maxLength={120}
            autoComplete="off"
            disabled={!branch || busy}
          />
        </label>

        <button
          type="submit"
          disabled={!branch || busy}
        >
          Add table
        </button>
      </form>

      <label className="search-field">
        <Search size={17} aria-hidden="true" />

        <input
          type="search"
          aria-label="Search tables"
          placeholder="Search tables"
          value={search}
          onChange={(event) =>
            setSearch(event.target.value)
          }
        />
      </label>

      {!filtered.length && (
        <div className="directory-empty">
          <QrCode size={28} />

          <strong>
            {!branch
              ? "Choose a branch"
              : searchQuery
                ? "No matching tables"
                : "No tables yet"}
          </strong>

          <p>
            {!branch
              ? "Select a restaurant branch to manage its tables."
              : searchQuery
                ? "Try searching with a different table label."
                : "Add a table to generate its unique customer feedback QR code."}
          </p>
        </div>
      )}

      {filtered.map((table) => (
        <article
          className="directory-row"
          key={table.id}
        >
          <div className="directory-main">
            <span className="directory-icon">
              <QrCode size={21} />
            </span>

            <span>
              <strong>{table.label}</strong>

              <small>
                {table.active
                  ? "Accepting feedback"
                  : "Disabled"}
              </small>
            </span>
          </div>

          <div className="directory-actions">
            {table.active && table.token && (
              <>
                <a
                  className="table-preview-link"
                  href={`/feedback/${table.token}/preview`}
                  target="_blank"
                  rel="noopener noreferrer"
                  title="Preview the customer form without recording feedback"
                  aria-label={`Preview feedback form for ${table.label}`}
                >
                  <Eye size={15} />
                  Preview
                </a>

                <a
                  className="table-preview-link"
                  href={`/feedback/${table.token}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  title="Open the live customer feedback form"
                  aria-label={`Open live feedback form for ${table.label}`}
                >
                  <ExternalLink size={14} />
                  Live link
                </a>
              </>
            )}

            <button
              type="button"
              className="secondary"
              disabled={busy || !table.active}
              title="Download table QR code"
              onClick={() =>
                action(() =>
                  download(
                    `/qr/table/${table.id}`,
                    token,
                    `table-${table.label}.png`,
                  ),
                )
              }
            >
              <Download size={15} />
              PNG
            </button>

            <button
              type="button"
              className="secondary"
              disabled={busy}
              onClick={() => edit(table)}
            >
              <Pencil size={15} />
              Edit
            </button>

            <button
              type="button"
              className="secondary"
              disabled={busy}
              onClick={() => confirm(table, false)}
            >
              {table.active ? "Disable" : "Enable"}
            </button>

            <button
              type="button"
              className="secondary"
              disabled={busy}
              title="Regenerate the table QR code"
              onClick={() => confirm(table, true)}
            >
              <RefreshCw size={15} />
              New QR
            </button>
          </div>
        </article>
      ))}

      {dialog && (
        <ResourceDialog
          spec={dialog}
          close={() => setDialog(null)}
        />
      )}
    </section>
  );
}