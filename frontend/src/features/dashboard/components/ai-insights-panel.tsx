"use client";
import type { Report } from "../types";
import { Sparkles } from "lucide-react";

export function AiInsightsPanel({
  report,
  busy,
  branch,
  onGenerate,
}: {
  report: Report | null;
  busy: boolean;
  branch: string;
  onGenerate: () => void;
}) {
  return (
    <section className="panel">
      <h3>Turn feedback into action</h3>
      <p className="muted">
        Analyze the selected branch and date range. Requires at least five meaningful comments.
        Ratings alone are excluded.
      </p>
      <button disabled={busy || !branch} onClick={onGenerate}>
        <Sparkles size={18} />
        {busy ? "Analyzing…" : "Generate AI summary"}
      </button>
      {report && (
        <div className="ai-report">
          <div className="ai-report-header">
            <div>
              <span className="ai-eyebrow">FEEDBACK ANALYSIS</span>
              <h2>Customer sentiment</h2>
              <p>Insights from your customer feedback</p>
            </div>
            <span className={`ai-sentiment ${report.sentiment.toLowerCase()}`}>
              {report.sentiment}
            </span>
          </div>

          <div className="ai-metrics">
            <div className="ai-metric">
              <span>Comments analyzed</span>
              <strong>{report.commentCount}</strong>
            </div>
            <div className="ai-metric praise">
              <span>Praise mentions</span>
              <strong>
                {report.topics.filter((t) => t.kind === "praise").reduce((n, t) => n + t.count, 0)}
              </strong>
            </div>
            <div className="ai-metric complaint">
              <span>Complaint mentions</span>
              <strong>
                {report.topics
                  .filter((t) => t.kind === "complaint")
                  .reduce((n, t) => n + t.count, 0)}
              </strong>
            </div>
          </div>

          <h3 className="ai-section-title">Key findings</h3>

          <div className="ai-topics">
            {report.topics.map((t, i) => (
              <article className={`ai-topic ${t.kind}`} key={i}>
                <div className="ai-topic-header">
                  <div>
                    <span className={`ai-topic-label ${t.kind}`}>
                      {t.kind === "praise" ? "Positive feedback" : "Needs attention"}
                    </span>
                    <h3>{t.title}</h3>
                  </div>
                  <span className="ai-mention">
                    {t.count} {t.count === 1 ? "mention" : "mentions"}
                  </span>
                </div>

                <div className="ai-evidence">
                  <span className="ai-eyebrow">CUSTOMER COMMENTS</span>
                  {t.examples.map((x, j) => (
                    <blockquote key={j}>{x}</blockquote>
                  ))}
                </div>

                <div className="ai-recommendation">
                  <span className="ai-eyebrow">RECOMMENDED ACTION</span>
                  <p>{t.suggestion}</p>
                </div>
              </article>
            ))}
          </div>

          <p className="ai-report-footer">
            Based on submitted customer comments. A comment may contribute to multiple findings.
          </p>
        </div>
      )}
    </section>
  );
}
