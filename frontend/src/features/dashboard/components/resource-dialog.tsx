"use client";

import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { AlertTriangle, X, LoaderCircle } from "lucide-react";

export type DialogSpec = {
  title: string;
  description: string;
  submitLabel: string;
  destructive?: boolean;
  fields?: ReactNode;
  submit: (data: FormData) => Promise<void>;
};

export function ResourceDialog({ spec, close }: { spec: DialogSpec; close: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const submitting = useRef(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    const element = dialog.current;
    if (element && !element.open) element.showModal();
    return () => {
      if (element?.open) element.close();
    };
  }, []);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    const data = new FormData(event.currentTarget);
    submitting.current = true;
    setBusy(true);
    setError("");
    try {
      await spec.submit(data);
      close();
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Unable to save. Try again.");
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <dialog
      ref={dialog}
      className="resource-dialog"
      aria-labelledby="resource-dialog-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!submitting.current) close();
      }}
    >
      <form onSubmit={save}>
        <div className="dialog-heading">
          {spec.destructive && (
            <span className="warning-symbol">
              <AlertTriangle size={21} />
            </span>
          )}
          <button
            type="button"
            className="icon-btn"
            aria-label="Close dialog"
            disabled={busy}
            onClick={close}
          >
            <X size={20} />
          </button>
        </div>
        <h2 id="resource-dialog-title">{spec.title}</h2>
        <p className="muted">{spec.description}</p>
        {spec.fields && (
          <fieldset disabled={busy} className="dialog-fields">
            {spec.fields}
          </fieldset>
        )}
        {error && (
          <p className="dialog-error" role="alert">
            {error}
          </p>
        )}
        <div className="dialog-actions">
          <button type="button" className="secondary" disabled={busy} onClick={close}>
            Cancel
          </button>
          <button className={spec.destructive ? "danger-button" : ""} disabled={busy}>
            {busy && <LoaderCircle className="spin" size={16} />}{" "}
            {busy ? "Saving…" : spec.submitLabel}
          </button>
        </div>
      </form>
    </dialog>
  );
}
