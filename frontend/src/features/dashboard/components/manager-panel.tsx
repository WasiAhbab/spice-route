"use client";
import { useState, type FormEvent } from "react";
import { Pencil, Users, UserPlus } from "lucide-react";
import { request } from "@/lib/api";
import type { Row } from "../types";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";

type AddHandler = (
  path: string,
  extra?: object,
) => (event: FormEvent<HTMLFormElement>) => Promise<void>;
export function ManagerPanel({
  managers,
  branches,
  branch,
  busy,
  add,
  refresh,
}: {
  managers: Row[];
  branches: Row[];
  branch: string;
  busy: boolean;
  add: AddHandler;
  refresh: () => Promise<void>;
}) {
  const [dialog, setDialog] = useState<DialogSpec | null>(null);
  function edit(manager: Row) {
    setDialog({
      title: "Edit manager",
      description: "Update the team member’s display name.",
      submitLabel: "Save changes",
      fields: (
        <label>
          Name
          <input name="name" required maxLength={120} defaultValue={manager.name} />
        </label>
      ),
      submit: async (data) => {
        await request(`/manage/managers/${manager.id}`, undefined, "PATCH", {
          name: data.get("name"),
        });
        await refresh();
      },
    });
  }
  function status(manager: Row) {
    setDialog({
      title: `${manager.active ? "Suspend" : "Reactivate"} ${manager.name}?`,
      description: manager.active
        ? "This manager will lose access to all assigned branches and existing sessions will be revoked. You can reactivate them later."
        : "Allow this manager to sign in again with their existing credentials.",
      destructive: !!manager.active,
      submitLabel: manager.active ? "Suspend" : "Reactivate",
      submit: async () => {
        await request(`/manage/managers/${manager.id}`, undefined, "PATCH", {
          active: !manager.active,
        });
        await refresh();
      },
    });
  }
  function assignment(manager: Row, remove: boolean) {
    setDialog({
      title: remove ? "Remove branch access?" : "Assign another branch",
      description: remove
        ? "This manager will lose access to the selected branch. Assign another branch first if this is their only assignment, or suspend the account."
        : "This grants the manager access to an additional active branch in your workspace.",
      destructive: remove,
      submitLabel: remove ? "Remove access" : "Assign branch",
      fields: remove ? undefined : (
        <label>
          Branch
          <select name="branch" required>
            {branches
              .filter((b) => b.id !== branch)
              .map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
          </select>
        </label>
      ),
      submit: async (data) => {
        await request(
          `/manage/managers/${manager.id}/branches/${remove ? branch : data.get("branch")}`,
          undefined,
          remove ? "DELETE" : "POST",
        );
        await refresh();
      },
    });
  }
  return (
    <section className="panel directory-panel">
      <div className="section-head">
        <div>
          <h3>Branch managers</h3>
          <p className="muted">Manage your team's access and assigned branches.</p>
        </div>
        <span className="tag">{managers.length}</span>
      </div>
      {!managers.length && (
        <div className="directory-empty">
          <Users size={28} />
          <strong>No managers in this branch</strong>
          <p>Create a manager to delegate daily operations.</p>
        </div>
      )}
      {managers.map((m) => (
        <article className="directory-row" key={m.id}>
          <div className="directory-main">
            <span className="directory-icon">{m.name.slice(0, 1).toUpperCase()}</span>
            <span>
              <strong>{m.name}</strong>
              <small>
                {m.email} ·{" "}
                {m.platform_active === false
                  ? "Platform suspended"
                  : m.active
                    ? "Active"
                    : "Suspended"}
              </small>
            </span>
          </div>
          <div className="directory-actions">
            <button className="secondary" disabled={busy} onClick={() => edit(m)}>
              <Pencil size={15} /> Edit
            </button>
            <button className="secondary" disabled={busy} onClick={() => status(m)}>
              {m.active ? "Suspend" : "Reactivate"}
            </button>
            <button
              className="secondary"
              disabled={busy || branches.length < 2}
              onClick={() => assignment(m, false)}
            >
              Assign branch
            </button>
            <button className="secondary" disabled={busy} onClick={() => assignment(m, true)}>
              Remove access
            </button>
          </div>
        </article>
      ))}
      <form className="directory-add" onSubmit={add("managers", { branchId: branch })}>
        <label>
          Name
          <input name="name" required maxLength={120} />
        </label>
        <label>
          Email
          <input name="email" type="email" required maxLength={254} />
        </label>
        <label>
          Temporary password
          <input
            name="password"
            type="password"
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />
        </label>
        <button disabled={busy || !branch}>
          <UserPlus size={16} /> Create manager
        </button>
      </form>
      {dialog && <ResourceDialog spec={dialog} close={() => setDialog(null)} />}
    </section>
  );
}
