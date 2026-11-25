import Link from "next/link";
export default function NotFound() {
  return (
    <main className="center">
      <div>
        <h1>Page not found.</h1>
        <Link href="/">Return home</Link>
      </div>
    </main>
  );
}
