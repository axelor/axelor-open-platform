import { isArrayLikeObject, isPlainObject } from "lodash";
import { $request, $use } from "../http";

// interceptors

$use(async (args, next) => {
  const { init = {} } = args;
  args.init = {
    ...init,
    credentials: "include",
    headers: {
      ...init.headers,
      Accept: "application/json",
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
