
import axios, { type AxiosResponse } from "axios";

const TOKEN_KEY = "pratos_access_token";
const ADMIN_NAME_KEY = "pratos_admin_name";

const api = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 30000,
});

// Attach the JWT to every protected API request.
api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem(TOKEN_KEY);

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    } else {
      delete config.headers.Authorization;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Report authentication errors without silently logging out.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.error(
        "Authentication failed. Check the JWT and backend security logs."
      );
    }

    return Promise.reject(error);
  }
);

// Backend API endpoints.
const routes = {
  login: "/v1/auth/login",
  transactions: "/v1/admin/payment-transactions",
  credentials: "/v1/admin/credentials",
  attempts: "/v1/admin/payment-attempts",
  providers: "/v1/admin/providers",
  insights: "/v1/admin/reports/insights",
};

// Log in and save the JWT returned by the backend.
async function login(
  username: string,
  password: string
): Promise<unknown> {
  const response = await api.post(routes.login, {
    username,
    password,
  });

  const body = response.data;
  const data = body?.data ?? body;

  const token =
    data?.accessToken ??
    data?.access_token ??
    data?.token ??
    data?.jwt;

  if (typeof token !== "string" || token.trim() === "") {
    throw new Error(
      "Login response did not contain a JWT. Check the backend login response format."
    );
  }

  sessionStorage.setItem(TOKEN_KEY, token);

  const adminName =
    data?.username ??
    data?.name ??
    data?.adminName ??
    username;

  sessionStorage.setItem(ADMIN_NAME_KEY, String(adminName));

  return data;
}

// Clear the authentication session.
function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(ADMIN_NAME_KEY);
}

// Return a readable error message from Axios or JavaScript.
function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data;

    if (typeof responseData === "string" && responseData.length > 0) {
      return responseData;
    }

    if (responseData && typeof responseData === "object") {
      const data = responseData as {
        message?: string;
        error?: string;
        detail?: string;
      };

      if (data.message) return data.message;
      if (data.detail) return data.detail;
      if (data.error) return data.error;
    }

    return error.message || "The API request failed.";
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "An unexpected error occurred.";
}

// Extract data from Axios responses or backend response envelopes.
function unwrap<T = unknown>(
  input: AxiosResponse<T> | T
): T | unknown {
  let result: unknown = input;

  // Handle AxiosResponse objects.
  if (
    result !== null &&
    typeof result === "object" &&
    "status" in result &&
    "data" in result &&
    "config" in result
  ) {
    result = (result as AxiosResponse<T>).data;
  }

  // Handle backend responses wrapped in { data: ... }.
  if (
    result !== null &&
    typeof result === "object" &&
    "data" in result
  ) {
    return (result as { data: unknown }).data;
  }

  return result;
}

export {
  api,
  clearSession,
  errorMessage,
  login,
  routes,
  unwrap,
};

export default api;
