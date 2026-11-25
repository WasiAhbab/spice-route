"use client";

import { useState } from "react";
import {
  Download,
  MessageSquare,
  Pencil,
  UserRound,
  Phone,
} from "lucide-react";

import { request } from "@/lib/api";
import type { Row } from "../types";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";

const statuses = {
  NEW: "New",
  IN_REVIEW: "In review",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
} as const;

const PAGE_SIZE = 15;

function statusName(status?: string) {
  return statuses[status as keyof typeof statuses] || "New";
}

function csvCell(value: unknown) {
  let text = String(value ?? "");

  if (/^[\s]*[=+@\-]/.test(text) || /^[\t\r\n]/.test(text)) {
    text = "'" + text;
  }

  return '"' + text.replaceAll('"', '""') + '"';
}

function exportFeedback(rows: Row[]) {
  const lines = [
    [
      "Date",
      "Table",
      "Rating",
      "Language",
      "Comment",
      "Review status",
    ],

    ...rows.map((feedback) => [
      feedback.created_at,
      feedback.table_label,
      feedback.rating,
      feedback.language,
      feedback.comment,
      statusName(feedback.review_status),
    ]),
  ];

  const file = new Blob(
    [
      "\uFEFF" +
        lines
          .map((line) => line.map(csvCell).join(","))
          .join("\r\n"),
    ],
    {
      type: "text/csv;charset=utf-8",
    },
  );

  const url = URL.createObjectURL(file);
  const anchor = document.createElement("a");

  anchor.href = url;
  anchor.download = "feedback.csv";

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function formatDate(value?: string) {
  if (!value) {
    return "Date unavailable";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Date unavailable";
  }

  return date.toLocaleString("en-BD", {
    timeZone: "Asia/Dhaka",
  });
}

function isInternationalPhone(value: string) {
  return /^\+[1-9][0-9]{1,14}$/.test(value);
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

  const searchQuery = search.trim().toLowerCase();

  const filtered = feedback.filter((item) => {
    const matchesStatus =
      !status || (item.review_status || "NEW") === status;

    const searchableContent = [
      item.comment,
      item.table_label,
      item.customer_name,
      item.phone,
      item.language,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();

    const matchesSearch =
      searchableContent.includes(searchQuery);

    return matchesStatus && matchesSearch;
  });

  const total = Math.max(
    1,
    Math.ceil(filtered.length / PAGE_SIZE),
  );

  const current = Math.min(page, total);

  const visibleFeedback = filtered.slice(
    (current - 1) * PAGE_SIZE,
    current * PAGE_SIZE,
  );

  function review(item: Row) {
    setDialog({
      title: "Review guest feedback",

      description:
        "Track follow-up without changing the guest’s original rating or comment. Internal notes are visible to authorized workspace staff.",

      submitLabel: "Save review",

      fields: (
        <>
          <label>
            Review status

            <select
              name="status"
              defaultValue={item.review_status || "NEW"}
            >
              {Object.entries(statuses).map(([key, value]) => (
                <option key={key} value={key}>
                  {value}
                </option>
              ))}
            </select>
          </label>

          <label>
            Internal note

            <textarea
              name="note"
              rows={5}
              maxLength={2000}
              defaultValue={item.internal_note || ""}
            />
          </label>
        </>
      ),

      submit: async (data) => {
        await request(
          `/analytics/feedback/${item.id}/review?${new URLSearchParams({
            branch,
          })}`,

          undefined,

          "PATCH",

          {
            status: data.get("status"),
            note: data.get("note"),
            version: item.version,
          },
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

          <p className="muted">
            Review customer experiences and resolve concerns.
          </p>
        </div>

        <button
          type="button"
          className="secondary"
          disabled={!filtered.length}
          onClick={() => exportFeedback(filtered)}
        >
          <Download size={16} />
          Export CSV
        </button>
      </div>

      <div className="directory-toolbar">
        <label className="search-field">
          <input
            type="search"
            aria-label="Search feedback"
            placeholder="Search name, phone, table or feedback"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(1);
            }}
          />
        </label>

        <label>
          Review status

          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
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

      {visibleFeedback.map((item) => {
        const rating = Math.min(
          5,
          Math.max(0, Math.trunc(item.rating || 0)),
        );

        const customerName = item.customer_name?.trim();
        const customerPhone = item.phone?.trim();

        return (
          <article
            className="feedback-row"
            key={item.id}
          >
            <div>
              <span
                className="stars"
                aria-label={`${rating} out of 5 stars`}
              >
                {"★".repeat(rating)}
                {"☆".repeat(5 - rating)}
              </span>

              <small>
                {item.table_label} · {item.language} ·{" "}
                {formatDate(item.created_at)}
              </small>
            </div>

            <p>{item.comment || "Rating only"}</p>

            <div className="feedback-contact">
              <div className="feedback-contact-item">
                <UserRound
                  size={16}
                  aria-hidden="true"
                />

                <div className="feedback-contact-details">
                  <span className="feedback-contact-label">
                    Customer name
                  </span>

                  <span className="feedback-contact-value">
                    {customerName || "Anonymous guest"}
                  </span>
                </div>
              </div>

              <div className="feedback-contact-item">
                <Phone
                  size={16}
                  aria-hidden="true"
                />

                <div className="feedback-contact-details">
                  <span className="feedback-contact-label">
                    Phone number
                  </span>

                  {customerPhone ? (
                    isInternationalPhone(customerPhone) ? (
                      <a
                        className="feedback-contact-value"
                        href={`tel:${customerPhone}`}
                        aria-label={`Call ${customerPhone}`}
                      >
                        {customerPhone}
                      </a>
                    ) : (
                      <span className="feedback-contact-value">
                        {customerPhone}
                      </span>
                    )
                  ) : (
                    <span className="feedback-contact-unavailable">
                      Not provided
                    </span>
                  )}
                </div>
              </div>
            </div>

            <div className="directory-toolbar">
              <span className="tag">
                {statusName(item.review_status)}
              </span>

              <button
                type="button"
                className="secondary"
                onClick={() => review(item)}
              >
                <Pencil size={15} />

                Review
              </button>
            </div>

            {item.internal_note && (
              <p className="internal-note">
                <strong>Internal note:</strong>{" "}
                {item.internal_note}
              </p>
            )}
          </article>
        );
      })}

      {filtered.length > 0 && (
        <div className="directory-toolbar">
          <small>
            {filtered.length} matching responses · Page{" "}
            {current} of {total}
          </small>

          <div className="directory-actions">
            <button
              type="button"
              className="secondary"
              disabled={current <= 1}
              onClick={() => setPage(current - 1)}
            >
              Previous
            </button>

            <button
              type="button"
              className="secondary"
              disabled={current >= total}
              onClick={() => setPage(current + 1)}
            >
              Next
            </button>
          </div>
        </div>
      )}

      {dialog && (
        <ResourceDialog
          spec={dialog}
          close={() => setDialog(null)}
        />
      )}
    </section>
  );
}