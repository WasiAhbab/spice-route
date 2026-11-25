"use client";
import { useEffect, useRef, useState } from "react";
import { request } from "@/lib/api";
import { Leaf, CheckCircle } from "lucide-react";
const copy = {
  en: {
    title: "How was your visit?",
    comment: "Tell us more (optional)",
    name: "Name (optional)",
    phone: "Phone (optional)",
    submit: "Send feedback",
    thanks: "Thank you for sharing.",
  },
  bn: {
    title: "আপনার অভিজ্ঞতা কেমন ছিল?",
    comment: "মতামত (ঐচ্ছিক)",
    name: "নাম (ঐচ্ছিক)",
    phone: "ফোন (ঐচ্ছিক)",
    submit: "মতামত পাঠান",
    thanks: "আপনার মতামতের জন্য ধন্যবাদ।",
  },
  mixed: {
    title: "Apnar experience kemon chilo?",
    comment: "Aro kichu bolun (optional)",
    name: "Naam (optional)",
    phone: "Phone (optional)",
    submit: "Feedback pathan",
    thanks: "Apnar feedback er jonno dhonnobad.",
  },
};
export default function Feedback({ token }: { token: string }) {
  const [info, I] = useState<{
      restaurant: string;
      branch: string;
      label: string;
      welcome_message?: string;
      thank_you_message?: string;
      collect_contact?: boolean;
    } | null>(null),
    [lang, L] = useState<keyof typeof copy>("en"),
    [rating, R] = useState(0),
    [error, E] = useState(""),
    [busy, B] = useState(false),
    [done, D] = useState(false);
  const submitting = useRef(false);
  useEffect(() => {
    request<typeof info>(`/public/feedback/${token}`)
      .then(I)
      .catch((e) => E(e.message));
  }, [token]);
  const c = copy[lang];
  return (
    <main className="guest">
      <section className="guest-card">
        <div className="brand">
          <Leaf /> spice route
        </div>
        <select
          aria-label="Language"
          value={lang}
          onChange={(e) => L(e.target.value as keyof typeof copy)}
        >
          <option value="en">English</option>
          <option value="bn">বাংলা</option>
          <option value="mixed">Banglish</option>
        </select>
        {done ? (
          <div className="thanks">
            <CheckCircle size={48} />
            <h1>{info?.thank_you_message || c.thanks}</h1>
          </div>
        ) : info ? (
          <>
            <p className="eyebrow">
              {info.restaurant} / {info.branch} / {info.label}
            </p>
            <h1>{info.welcome_message || c.title}</h1>
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                if (submitting.current) return;
                submitting.current = true;
                B(true);
                E("");
                try {
                  await request(`/public/feedback/${token}`, undefined, "POST", {
                    ...Object.fromEntries(new FormData(e.currentTarget)),
                    rating,
                    language: lang,
                  });
                  D(true);
                } catch (e) {
                  E((e as Error).message);
                } finally {
                  submitting.current = false;
                  B(false);
                }
              }}
            >
              <div className="rating-picker">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button
                    type="button"
                    key={n}
                    aria-label={`${n} stars`}
                    aria-pressed={n === rating}
                    onClick={() => R(n)}
                    className={n <= rating ? "lit" : ""}
                  >
                    ★
                  </button>
                ))}
              </div>
              <label>
                {c.comment}
                <textarea name="comment" rows={4} maxLength={2000} />
              </label>
              {info.collect_contact !== false && (
                <>
                  <label>
                    {c.name}
                    <input name="name" maxLength={120} />
                  </label>
                  <label>
                    {c.phone}
                    <input name="phone" type="tel" maxLength={40} />
                  </label>
                </>
              )}
              <button disabled={!rating || busy}>{busy ? "…" : c.submit}</button>
            </form>
          </>
        ) : !error ? (
          <p>Loading feedback form…</p>
        ) : (
          <div className="directory-empty">
            <Leaf size={28} />
            <strong>Feedback link unavailable</strong>
            <p>Ask the restaurant team for a current QR code.</p>
          </div>
        )}
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        <small>Powered by Spice Route · No account required</small>
      </section>
    </main>
  );
}
