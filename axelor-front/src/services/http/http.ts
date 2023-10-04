import { RequestOptions } from "../client/client";
import fetch from "./http-fetch";

export type HttpInterceptorArgs = {
  input: RequestInfo | URL;
  init?: RequestInit;
  options?: RequestOptions;
};

export type HttpInterceptor = (
  args: HttpInterceptorArgs,
  next: () => Promise<any>,
) => Promise<any>;

const interceptors: HttpInterceptor[] = [];

async function intercept(
  args: HttpInterceptorArgs,
  cb: () => Promise<Response>,
) {
  let stack: HttpInterceptor[] = [...interceptors];
  let index = -1;
  let next = async () => {
    let func = stack[++index];
    if (func) {
      return await func(args, next);
    }
    return await cb();
  };
  return await next();
}

export async function $request(
  input: RequestInfo | URL,
  init?: RequestInit,
  options?: RequestOptions,
) {
  const args: HttpInterceptorArgs = { input, init, options };
  return intercept(args, () => fetch(args.input, args.init));
}

export function $use(interceptor: HttpInterceptor) {
  interceptors.push(interceptor);
  return () => {
    const index = interceptors.findIndex((x) => x === interceptor);
    if (index > -1) {
      interceptors.splice(index, 1);
    }
  };
}

export interface HttpClient {
  get(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
  put(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
  post(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
  patch(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
  delete(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
  head(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
  options(input: RequestInfo | URL, init?: RequestInit): Promise<Response>;
}

const methodNames: (keyof HttpClient)[] = [
  "get",
  "put",
  "post",
  "patch",
  "delete",
  "head",
  "options",
];

export const http = methodNames.reduce((prev, name) => {
  prev[name] = (input, init) =>
    $request(input, { ...init, method: name.toUpperCase() });
  return prev;
}, {} as HttpClient);
