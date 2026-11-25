const API = "/api/spice";

export type Session = { role: "OWNER" | "MANAGER" | "ADMIN"; name: string };

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function ensureSuccess(response: Response): Promise<void> {
  if (response.ok) return;
  let message = `Request failed (${response.status})`;
  try {
    const body: unknown = await response.json();
    if (
      typeof body === "object" &&
      body !== null &&
      "message" in body &&
      typeof body.message === "string"
    ) {
      message = body.message;
    }
  } catch {
    // Non-JSON gateway errors retain the HTTP status message.
  }
  throw new ApiError(message, response.status);
}

export async function request<T>(
  path: string,
  _session?: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const response = await fetch(API + path, {
    method,
    credentials: "same-origin",
    headers: body === undefined ? {} : { "Content-Type": "application/json" },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    cache: "no-store",
  });
  await ensureSuccess(response);
  return response.json() as Promise<T>;
}

export async function download(path: string, _session: string, filename: string) {
  const response = await fetch(API + path, { credentials: "same-origin", cache: "no-store" });
  await ensureSuccess(response);
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
