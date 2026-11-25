"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { request, Session } from "@/lib/api";
import { ArrowUpRight, Leaf } from "lucide-react";
export default function Auth() {
  const [register, setRegister] = useState(false),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const router = useRouter();
  return (
    <main className="auth-layout">
      <section className="auth-form">
        <div className="brand">
          <Leaf size={25} /> spice route<span> / workspace</span>
        </div>
        <div className="auth-inner">
          <p className="eyebrow">GUEST EXPERIENCE PLATFORM</p>
          <h1>{register ? "Create your workspace." : "Welcome back."}</h1>
          <p className="muted">
            {register
              ? "Start listening to your guests today."
              : "Sign in to see what your guests are saying."}
          </p>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              const data = Object.fromEntries(new FormData(e.currentTarget));
              try {
                await request<Session>(
                  `/auth/${register ? "register" : "login"}`,
                  undefined,
                  "POST",
                  data,
                );
                router.push("/dashboard");
              } catch (e) {
                setError((e as Error).message);
              } finally {
                setBusy(false);
              }
            }}
          >
            {register && (
              <label>
                Full name
                <input name="name" required autoComplete="name" />
              </label>
            )}
            <label>
              Email address
              <input
                name="email"
                type="email"
                required
                autoComplete="email"
                placeholder="you@restaurant.com"
              />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                required
                minLength={8}
                autoComplete={register ? "new-password" : "current-password"}
              />
            </label>
            {error && (
              <p role="alert" className="error">
                {error}
              </p>
            )}
            <button disabled={busy}>
              {busy ? "Please wait…" : register ? "Create account" : "Sign in"}{" "}
              <ArrowUpRight size={18} />
            </button>
          </form>
          <p className="muted">
            {register ? "Already registered?" : "New to Spice Route?"}{" "}
            <button
              className="link"
              onClick={() => {
                setRegister(!register);
                setError("");
              }}
            >
              {register ? "Sign in" : "Create an account"}
            </button>
          </p>
        </div>
        <small>Built for hospitality. Designed for people.</small>
      </section>
      <section className="auth-art">
        <div className="orb" />
        <div className="art-content">
          <p className="eyebrow">01 / THE BIG PICTURE</p>
          <h2>
            Good food.
            <br />
            Better listening.
          </h2>
          <p>Every table has a story. Bring them together.</p>
          <div className="art-card">
            <span>Guest experience</span>
            <strong>Listen. Learn. Improve.</strong>
            <div className="art-bars">
              {[36, 52, 41, 72, 59, 85, 68, 94].map((h, i) => (
                <i key={i} style={{ height: h }} />
              ))}
            </div>
            <small>Illustrative analytics preview</small>
          </div>
        </div>
      </section>
    </main>
  );
}
