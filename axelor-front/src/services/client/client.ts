import { isArrayLikeObject, isPlainObject } from "lodash";
import { $request, $use } from "../http";

export const readCookie = (name: string) => {
  const match = document.cookie.match(
    new RegExp("(^|;\\s*)(" + name + ")=([^;]*)")
  );
  return match ? decodeURIComponent(match[3]) : null;
};

// interceptors

$use(async (args, next) => {
  const { init = {} } = args;
  const token = readCookie("CSRF-TOKEN");
  const headers: Record<string, string> = {
    Accept: "application/json",
  };

  if (token) headers["X-CSRF-Token"] = token;

  args.init = {
    ...init,
    credentials: "include",
    headers: {
      ...init.headers,
      ...headers,
    },
  };
  return next();
});

$use(async (args, next) => {
  const { init = {} } = args;
  const { body } = init;

  // convert plain object or array to JSON string
  if (isPlainObject(body) || isArrayLikeObject(body)) {
    init.headers = {
      ...init.headers,
      "Content-Type": "application/json",
    };
    init.body = JSON.stringify(body);
  }

  return next();
});

export type RequestArgs = {
  url: string;
  method?: "GET" | "PUT" | "POST" | "PATCH" | "DELETE" | "HEAD" | "OPTIONS";
  headers?: HeadersInit;
  signal?: AbortSignal;
  body?: any;
};

const baseURL = "./";

export function makeURL(path: string | string[]) {
  const parts = [path].flat();
  const url = parts.join("/");
  if (url.startsWith(baseURL) || url.startsWith("/")) {
    return url;
  }
  return baseURL + url;
}

export async function request(args: RequestArgs): Promise<Response> {
  const { url, method, headers, body } = args;
  const input = makeURL(url);
  const init = {
    method,
    headers,
    body,
  };
  return $request(input, init);
}
