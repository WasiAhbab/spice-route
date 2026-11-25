import "@/styles/globals.css";
import type { Metadata } from "next";
export const metadata: Metadata = {
  title: "Spice Route | Guest experience",
  description: "Make every guest experience count.",
};
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
