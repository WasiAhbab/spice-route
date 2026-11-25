"use client";
import { useState } from "react";
import { Download, MessageSquare, Pencil } from "lucide-react";
import { request } from "@/lib/api";
import type { Row } from "../types";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";

const statuses = {
  NEW: "New",
  IN_REVIEW: "In review",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
} as const;
function statusName(status?: string) {
  return statuses[status as keyof typeof statuses] || "New";
}
function csvCell(value: unknown) {
  let text = String(value ?? "");
  if (/^[\s]*[=+@\-]/.test(text) || /^[\t\r\n]/.test(text)) text = "'" + text;
  return '"' + text.replaceAll('"', '""') + '"';
}
function exportFeedback(rows: Row[]) {
  const lines = [
    ["Date", "Table", "Rating", "Language", "Comment", "Review status"],
    ...rows.map((f) => [
      f.created_at,
      f.table_label,
      f.rating,
      f.language,
      f.comment,
      statusName(f.review_status),
    ]),
  ];
  const file = new Blob(
    ["\uFEFF" + lines.map((line) => line.map(csvCell).join(",")).join("\r\n")],
    { type: "text/csv;charset=utf-8" },
  );
  const url = URL.createObjectURL(file);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = "feedback.csv";
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function FeedbackPanel({
  feedback,
  branch,
  refresh,
}: {
  feedback: Row[];
  branch: string;
  refresh: () => Promise<void>;
}) {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(1);
  const [dialog, setDialog] = useState<DialogSpec | null>(null);
  const filtered = feedback.filter(
    (f) =>
      (!status || (f.review_status || "NEW") === status) &&
      `${f.comment || ""} ${f.table_label || ""}`
        .toLowerCase()
        .includes(search.trim().toLowerCase()),
  );
  const total = Math.max(1, Math.ceil(filtered.length / 15));
  const current = Math.min(page, total);
  function review(f: Row) {
    setDialog({
      title: "Review guest feedback",
      description:
        "Track follow-up without changing the guest’s original rating or comment. Internal notes are visible to authorized workspace staff.",
      submitLabel: "Save review",
      fields: (
        <>
          <label>
            Review status
            <select name="status" defaultValue={f.review_status || "NEW"}>
              {Object.entries(statuses).map(([key, value]) => (
                <option key={key} value={key}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label>
            Internal note
            <textarea name="note" rows={5} maxLength={2000} defaultValue={f.internal_note || ""} />
          </label>
        </>
      ),
      submit: async (data) => {
        await request(
          `/analytics/feedback/${f.id}/review?${new URLSearchParams({ branch })}`,
          undefined,
          "PATCH",
          { status: data.get("status"), note: data.get("note"), version: f.version },
        );
        await refresh();
      },
    });
  }
  return (
    <section className="panel directory-panel">
      <div className="section-head">
        <div>
          <h3>Guest responses</h3>
          <p className="muted">Review and resolve customer concerns.</p>
        </div>
        <button
          className="secondary"
          disabled={!filtered.length}
          onClick={() => exportFeedback(filtered)}
        >
          <Download size={16} /> Export CSV
        </button>
      </div>
      <div className="directory-toolbar">
        <label className="search-field">
          <input
            aria-label="Search feedback"
            placeholder="Search feedback or table"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
          />
        </label>
        <label>
          Review status
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(1);
            }}
          >
            <option value="">All statuses</option>
            {Object.entries(statuses).map(([key, value]) => (
              <option key={key} value={key}>
                {value}
              </option>
            ))}
          </select>
        </label>
      </div>
      {!filtered.length && (
        <div className="directory-empty">
          <MessageSquare size={28} />
          <strong>No matching responses</strong>
          <p>Try adjusting the selected dates or filters.</p>
        </div>
      )}
      {filtered.slice((current - 1) * 15, current * 15).map((f) => (
        <article className="feedback-row" key={f.id}>
          <div>
            <span className="stars" aria-label={`${f.rating} out of 5 stars`}>
              {"★".repeat(f.rating || 0)}
              {"☆".repeat(5 - (f.rating || 0))}
            </span>
            <small>
              {f.table_label} · {f.language} ·{" "}
              {new Date(f.created_at!).toLocaleString("en-BD", { timeZone: "Asia/Dhaka" })}
            </small>
          </div>
          <p>{f.comment || "Rating only"}</p>
          <div className="directory-toolbar">
            <span className="tag">{statusName(f.review_status)}</span>
            <button className="secondary" onClick={() => review(f)}>
              <Pencil size={15} /> Review
            </button>
          </div>
          {f.internal_note && (
            <p className="internal-note">
              <strong>Internal note:</strong> {f.internal_note}
            </p>
          )}
        </article>
      ))}
      {filtered.length > 0 && (
        <div className="directory-toolbar">
          <small>
            {filtered.length} matching responses · Page {current} of {total}
          </small>
          <div className="directory-actions">
            <button
              className="secondary"
              disabled={current <= 1}
              onClick={() => setPage(current - 1)}
            >
              Previous
            </button>
            <button
              className="secondary"
              disabled={current >= total}
              onClick={() => setPage(current + 1)}
            >
              Next
            </button>
          </div>
        </div>
      )}
      {dialog && <ResourceDialog spec={dialog} close={() => setDialog(null)} />}
    </section>
  );
}
