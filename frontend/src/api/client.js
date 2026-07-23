const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// Custom error class carrying HTTP status code and server details
export class ApiError extends Error {
  constructor(message, status, details) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

async function request(path, { method = "GET", body, token } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    // Handle network errors (e.g. backend unreachable)
    throw new ApiError("Cannot reach the backend server.", 0);
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const message = data?.message || res.statusText || `Request failed (${res.status})`;
    throw new ApiError(message, res.status, data?.details);
  }
  return data;
}

export const api = {
  register: (username, password) =>
    request("/api/auth/register", { method: "POST", body: { username, password } }),
  login: (username, password) =>
    request("/api/auth/login", { method: "POST", body: { username, password } }),
  refresh: (refreshToken) =>
    request("/api/auth/refresh", { method: "POST", body: { refreshToken } }),
  logout: (refreshToken) =>
    request("/api/auth/logout", { method: "POST", body: { refreshToken } }),

  myAccounts: (token) => request("/api/accounts/mine", { token }),
  createAccount: (token) => request("/api/accounts", { method: "POST", token }),
  accountBalance: (token, accountId) =>
    request(`/api/accounts/${accountId}/balance`, { token }),

  deposit: (token, accountId, amount, idempotencyKey) =>
    request("/api/transactions/deposit", {
      method: "POST",
      token,
      body: { accountId, amount, idempotencyKey },
    }),
  withdraw: (token, accountId, amount, idempotencyKey) =>
    request("/api/transactions/withdraw", {
      method: "POST",
      token,
      body: { accountId, amount, idempotencyKey },
    }),
  transfer: (token, sourceAccountId, destinationAccountId, amount, idempotencyKey) =>
    request("/api/transactions/transfer", {
      method: "POST",
      token,
      body: { sourceAccountId, destinationAccountId, amount, idempotencyKey },
    }),
  history: (token, accountId, page = 0, size = 20) =>
    request(`/api/transactions/account/${accountId}?page=${page}&size=${size}`, { token }),

  adminAccounts: (token) => request("/api/admin/accounts", { token }),
  adminCreateUser: (token, username, password, role) =>
    request("/api/admin/users", { method: "POST", token, body: { username, password, role } }),

  request, // exposed so AuthContext can build the generic authFetch wrapper
};

export { API_BASE_URL };
