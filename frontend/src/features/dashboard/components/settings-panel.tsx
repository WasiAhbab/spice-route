"use client";
import { useState } from "react";
import { ShieldCheck, UserRound } from "lucide-react";
import { request, type Session } from "@/lib/api";
import { ResourceDialog, type DialogSpec } from "./resource-dialog";

export function SettingsPanel({
  session,
  onProfile,
  onSignedOut,
}: {
  session: Session;
  onProfile: (session: Session) => void;
  onSignedOut: () => void;
}) {
  const [dialog, setDialog] = useState<DialogSpec | null>(null);
  function profile() {
    setDialog({
      title: "Edit profile",
      description: "Your display name is visible in your workspace.",
      submitLabel: "Save profile",
      fields: (
        <label>
          Full name
          <input
            name="name"
            defaultValue={session.name}
            required
            maxLength={120}
            autoComplete="name"
          />
        </label>
      ),
      submit: async (data) =>
        onProfile(
          await request<Session>("/auth/profile", undefined, "PATCH", { name: data.get("name") }),
        ),
    });
  }
  function password() {
    setDialog({
      title: "Change password",
      description:
        "Changing your password signs you out on all devices. Sign in again using the new password.",
      submitLabel: "Change password",
      fields: (
        <>
          <label>
            Current password
            <input
              name="currentPassword"
              type="password"
              autoComplete="current-password"
              required
            />
          </label>
          <label>
            New password
            <input
              name="newPassword"
              type="password"
              autoComplete="new-password"
              minLength={8}
              maxLength={72}
              required
            />
          </label>
          <label>
            Confirm new password
            <input
              name="confirm"
              type="password"
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>
        </>
      ),
      submit: async (data) => {
        if (data.get("newPassword") !== data.get("confirm"))
          throw new Error("The new passwords do not match.");
        await request("/auth/password", undefined, "POST", {
          currentPassword: data.get("currentPassword"),
          newPassword: data.get("newPassword"),
        });
        onSignedOut();
      },
    });
  }
  return (
    <div className="charts">
      <section className="panel directory-panel">
        <div className="directory-main">
          <span className="directory-icon">
            <UserRound size={22} />
          </span>
          <span>
            <h3>Personal profile</h3>
            <small>Manage your workspace identity</small>
          </span>
        </div>
        <div className="directory-row">
          <div>
            <strong>{session.name}</strong>
            <p className="muted">{session.role.toLowerCase()}</p>
          </div>
          <button className="secondary" onClick={profile}>
            Edit profile
          </button>
        </div>
      </section>
      <section className="panel directory-panel">
        <div className="directory-main">
          <span className="directory-icon">
            <ShieldCheck size={22} />
          </span>
          <span>
            <h3>Security</h3>
            <small>Keep your account protected</small>
          </span>
        </div>
        <div className="directory-row">
          <div>
            <strong>Password</strong>
            <p className="muted">Change your sign-in credentials.</p>
          </div>
          <button className="secondary" onClick={password}>
            Change password
          </button>
        </div>
      </section>
      {dialog && <ResourceDialog spec={dialog} close={() => setDialog(null)} />}
    </div>
  );
}
