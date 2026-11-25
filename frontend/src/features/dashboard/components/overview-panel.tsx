"use client";
import type { Row } from "../types";

import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  LineChart,
  Line,
} from "recharts";

export function OverviewPanel({ feedback, compare }: { feedback: Row[]; compare: Row[] }) {
  const avg = feedback.length
    ? feedback.reduce((sum, f) => sum + (f.rating || 0), 0) / feedback.length
    : 0;
  const distribution = [1, 2, 3, 4, 5].map((n) => ({
    rating: n,
    count: feedback.filter((f) => f.rating === n).length,
  }));
  const days = Object.entries(
    feedback.reduce<Record<string, number>>((acc, f) => {
      const day = new Intl.DateTimeFormat("en-CA", {
        timeZone: "Asia/Dhaka",
      }).format(new Date(f.created_at!));
      acc[day] = (acc[day] || 0) + 1;
      return acc;
    }, {}),
  )
    .sort()
    .map(([date, count]) => ({ date, count }));
  return (
    <>
      <div className="metrics">
        {[
          ["Average rating", avg ? avg.toFixed(2) + " / 5" : "—"],
          ["Total responses", feedback.length],
          ["Positive ratings", feedback.filter((f) => (f.rating || 0) >= 4).length],
          [
            "Needs attention",
            feedback.filter((f) => (f.rating || 0) <= 2 && f.review_status !== "RESOLVED").length,
          ],
        ].map(([n, v]) => (
          <section className="panel metric" key={n}>
            <span>{n}</span>
            <strong>{v}</strong>
            <small>Selected period</small>
          </section>
        ))}
      </div>
      <div className="charts">
        <section className="panel">
          <h3>Rating distribution</h3>
          <ResponsiveContainer width="100%" height={235}>
            <BarChart data={distribution}>
              <CartesianGrid vertical={false} stroke="#e6e4dd" />
              <XAxis dataKey="rating" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#747d54" radius={[5, 5, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </section>
        <section className="panel">
          <h3>Feedback volume</h3>
          <ResponsiveContainer width="100%" height={235}>
            <LineChart data={days}>
              <CartesianGrid vertical={false} stroke="#e6e4dd" />
              <XAxis dataKey="date" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Line type="monotone" dataKey="count" stroke="#a75b42" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </section>
      </div>
      <section className="panel">
        <h3>Branch performance · all time</h3>
        {compare.length ? (
          compare.map((b) => (
            <div className="record" key={b.id}>
              <strong>{b.name}</strong>
              <span>{b.volume} responses</span>
              <span>{b.average ?? "—"} / 5</span>
            </div>
          ))
        ) : (
          <p className="muted">Create your first branch to begin.</p>
        )}
      </section>
    </>
  );
}
