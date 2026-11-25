"use client";

import { useEffect, useRef, useState } from "react";
import { request } from "@/lib/api";
import { Leaf, CheckCircle } from "lucide-react";

import {
  getCountries,
  getCountryCallingCode,
  parsePhoneNumberFromString,
} from "libphonenumber-js/max";

import type { CountryCode } from "libphonenumber-js";

const countryNames = new Intl.DisplayNames(["en"], {
  type: "region",
});

const countryOptions = getCountries()
  .map((country) => ({
    country,
    name: countryNames.of(country) || country,
    code: `+${getCountryCallingCode(country)}`,
  }))
  .sort((a, b) => a.name.localeCompare(b.name));

function normalizeDigits(value: string): string {
  return value
    .replace(/[০-৯]/g, (digit) =>
      String("০১২৩৪৫৬৭৮৯".indexOf(digit)),
    )
    .replace(/[^0-9]/g, "");
}

function normalizeName(value: string): string {
  return value.replace(/[^\p{L}\p{M} ]/gu, "");
}

function isValidName(value: string): boolean {
  return /^\p{L}[\p{L}\p{M}]*(?: +\p{L}[\p{L}\p{M}]*)*$/u.test(
    value,
  );
}

const copy = {
  en: {
    title: "How was your visit?",
    comment: "Tell us more (optional)",
    name: "Name (optional)",
    namePlaceholder: "Enter your full name",
    phone: "Phone (optional)",
    phonePlaceholder: "Enter phone number",
    country: "Country calling code",
    phoneHint: "Select your country and enter your local phone number.",
    invalidName: "Name must contain only letters and spaces.",
    invalidPhone: "Enter a valid phone number for the selected country.",
    submit: "Send feedback",
    submitting: "Sending feedback...",
    thanks: "Thank you for sharing.",
    loading: "Loading feedback form...",
    unavailable: "Feedback link unavailable",
    unavailableHint: "Ask the restaurant team for a current QR code.",
    footer: "Powered by Spice Route · No account required",
  },

  bn: {
    title: "আপনার অভিজ্ঞতা কেমন ছিল?",
    comment: "মতামত (ঐচ্ছিক)",
    name: "নাম (ঐচ্ছিক)",
    namePlaceholder: "আপনার সম্পূর্ণ নাম লিখুন",
    phone: "ফোন (ঐচ্ছিক)",
    phonePlaceholder: "ফোন নম্বর লিখুন",
    country: "দেশের কোড",
    phoneHint: "দেশের কোড নির্বাচন করে ফোন নম্বর লিখুন।",
    invalidName: "নামে শুধু অক্ষর ও স্পেস ব্যবহার করুন।",
    invalidPhone: "সঠিক ফোন নম্বর লিখুন।",
    submit: "মতামত পাঠান",
    submitting: "মতামত পাঠানো হচ্ছে...",
    thanks: "আপনার মতামতের জন্য ধন্যবাদ।",
    loading: "ফর্ম লোড হচ্ছে...",
    unavailable: "মতামতের লিংকটি সক্রিয় নয়",
    unavailableHint: "রেস্টুরেন্ট থেকে নতুন QR কোড সংগ্রহ করুন।",
    footer: "Powered by Spice Route · অ্যাকাউন্ট প্রয়োজন নেই",
  },

  mixed: {
    title: "Apnar experience kemon chilo?",
    comment: "Aro kichu bolun (optional)",
    name: "Naam (optional)",
    namePlaceholder: "Apnar full name likhun",
    phone: "Phone (optional)",
    phonePlaceholder: "Phone number din",
    country: "Country code",
    phoneHint: "Country code select kore phone number din.",
    invalidName: "Naam e shudhu letters ebong space use korun.",
    invalidPhone: "Selected country er valid phone number din.",
    submit: "Feedback pathan",
    submitting: "Feedback pathano hocche...",
    thanks: "Apnar feedback er jonno dhonnobad.",
    loading: "Feedback form load hocche...",
    unavailable: "Feedback link available nei",
    unavailableHint: "Restaurant theke notun QR code nin.",
    footer: "Powered by Spice Route · Account lagbe na",
  },
};

type FeedbackInfo = {
  restaurant: string;
  branch: string;
  label: string;
  welcome_message?: string;
  thank_you_message?: string;
  collect_contact?: boolean;
};

type Language = keyof typeof copy;

export default function Feedback({ token }: { token: string }) {
  const [info, setInfo] = useState<FeedbackInfo | null>(null);
  const [lang, setLang] = useState<Language>("en");
  const [rating, setRating] = useState(0);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  const [country, setCountry] = useState<CountryCode>("BD");
  const [customerName, setCustomerName] = useState("");
  const [phone, setPhone] = useState("");

  const submitting = useRef(false);

  const c = copy[lang];

  useEffect(() => {
    let active = true;

    request<FeedbackInfo>(`/public/feedback/${token}`)
      .then((data) => {
        if (active) {
          setInfo(data);
        }
      })
      .catch((exception) => {
        if (active) {
          setError(
            exception instanceof Error
              ? exception.message
              : "Unable to load feedback form.",
          );
        }
      });

    return () => {
      active = false;
    };
  }, [token]);

  async function handleSubmit(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    if (submitting.current || !rating || !info) {
      return;
    }

    const formData = new FormData(event.currentTarget);

    submitting.current = true;
    setBusy(true);
    setError("");

    try {
      const collectContact = info.collect_contact !== false;

      const name = collectContact
        ? customerName.trim().replace(/ +/g, " ")
        : "";

      const rawPhone = collectContact ? phone.trim() : "";

      let internationalPhone = "";

      if (name && !isValidName(name)) {
        throw new Error(c.invalidName);
      }

      if (rawPhone) {
        const parsedPhone = parsePhoneNumberFromString(rawPhone, {
          defaultCountry: country,
          extract: false,
        });

        if (
          !parsedPhone ||
          !parsedPhone.isValid() ||
          parsedPhone.countryCallingCode !==
            getCountryCallingCode(country) ||
          (parsedPhone.country &&
            parsedPhone.country !== country)
        ) {
          throw new Error(c.invalidPhone);
        }

        internationalPhone = parsedPhone.number;
      }

      await request(
        `/public/feedback/${token}`,
        undefined,
        "POST",
        {
          comment: String(formData.get("comment") || "").trim(),
          name,
          phone: internationalPhone,
          rating,
          language: lang,
        },
      );

      setDone(true);
    } catch (exception) {
      setError(
        exception instanceof Error
          ? exception.message
          : "Unable to submit feedback. Please try again.",
      );
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <main className="guest">
      <section className="guest-card">
        <div className="brand">
          <Leaf />
          spice route
        </div>

        <select
          aria-label="Language"
          value={lang}
          disabled={busy}
          onChange={(event) =>
            setLang(event.target.value as Language)
          }
        >
          <option value="en">English</option>
          <option value="bn">বাংলা</option>
          <option value="mixed">Banglish</option>
        </select>

        {done ? (
          <div className="thanks" role="status">
            <CheckCircle size={48} />

            <h1>{info?.thank_you_message || c.thanks}</h1>
          </div>
        ) : info ? (
          <>
            <p className="eyebrow">
              {info.restaurant} / {info.branch} / {info.label}
            </p>

            <h1>{info.welcome_message || c.title}</h1>

            <form onSubmit={handleSubmit}>
              <div
                className="rating-picker"
                role="group"
                aria-label="Rating"
              >
                {[1, 2, 3, 4, 5].map((number) => (
                  <button
                    type="button"
                    key={number}
                    aria-label={`${number} stars`}
                    aria-pressed={number === rating}
                    disabled={busy}
                    onClick={() => setRating(number)}
                    className={number <= rating ? "lit" : ""}
                  >
                    ★
                  </button>
                ))}
              </div>

              <label>
                {c.comment}

                <textarea
                  name="comment"
                  rows={4}
                  maxLength={2000}
                  disabled={busy}
                />
              </label>

              {info.collect_contact !== false && (
                <>
                  <label>
                    {c.name}

                    <input
                      name="name"
                      type="text"
                      autoComplete="name"
                      placeholder={c.namePlaceholder}
                      maxLength={120}
                      value={customerName}
                      disabled={busy}
                      onChange={(event) =>
                        setCustomerName(
                          normalizeName(event.target.value),
                        )
                      }
                    />
                  </label>

                  <div className="phone-group">
                    <label htmlFor="customer-phone">
                      {c.phone}
                    </label>

                    <div className="phone-field">
                      <select
                        name="country"
                        aria-label={c.country}
                        autoComplete="tel-country-code"
                        value={country}
                        disabled={busy}
                        onChange={(event) =>
                          setCountry(
                            event.target.value as CountryCode,
                          )
                        }
                      >
                        {countryOptions.map((option) => (
                          <option
                            key={option.country}
                            value={option.country}
                          >
                            {option.name} ({option.code})
                          </option>
                        ))}
                      </select>

                      <input
                        id="customer-phone"
                        name="phone"
                        type="tel"
                        inputMode="numeric"
                        autoComplete="tel-national"
                        placeholder={c.phonePlaceholder}
                        aria-label={c.phone}
                        aria-describedby="customer-phone-hint"
                        maxLength={15}
                        value={phone}
                        disabled={busy}
                        onChange={(event) =>
                          setPhone(
                            normalizeDigits(event.target.value),
                          )
                        }
                      />
                    </div>

                    <small
                      id="customer-phone-hint"
                      className="contact-hint"
                    >
                      {c.phoneHint}
                    </small>
                  </div>
                </>
              )}

              <button
                type="submit"
                disabled={!rating || busy}
                aria-busy={busy}
              >
                {busy ? c.submitting : c.submit}
              </button>
            </form>
          </>
        ) : !error ? (
          <p role="status">{c.loading}</p>
        ) : (
          <div className="directory-empty">
            <Leaf size={28} />
            <strong>{c.unavailable}</strong>
            <p>{c.unavailableHint}</p>
          </div>
        )}

        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}

        <small>{c.footer}</small>
      </section>
    </main>
  );
}