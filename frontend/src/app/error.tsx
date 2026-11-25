"use client";
export default function ErrorPage({ reset }: { reset: () => void }) {
  return (
    <main className="center">
      <div>
        <h1>Something went wrong.</h1>
        <button onClick={reset}>Try again</button>
      </div>
    </main>
  );
}
