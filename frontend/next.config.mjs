const backendUrl = (process.env.BACKEND_URL || "http://localhost:8080").replace(/\/$/, "");

/** @type {import("next").NextConfig} */
const config = {
  poweredByHeader: false,
  async rewrites() {
    return [{ source: "/api/spice/:path*", destination: `${backendUrl}/api/:path*` }];
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
    ];
  },
};

export default config;
