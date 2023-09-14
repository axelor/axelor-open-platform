import { vi } from "vitest";
import { $use, http } from "./http";
import fetch from "./http-fetch";

vi.mock("./http-fetch", () => {
  const fetch = vi.fn();
  return { default: fetch };
});

afterEach(() => vi.resetAllMocks());

describe("http tests", async () => {
  it("should make GET request", async () => {
    await http.get("some/resource/1", {
      headers: {
        Accept: "application/json",
      },
    });
    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource/1", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });
  });

  it("should make PUT request", async () => {
    await http.put("some/resource/1", {
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        some: "value",
      }),
    });
    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource/1", {
      method: "PUT",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        some: "value",
      }),
    });
  });
  it("should make POST request", async () => {
    await http.post("some/resource", {
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        some: "value",
      }),
    });
    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        some: "value",
      }),
    });
  });
  it("should make PATCH request", async () => {
    await http.patch("some/resource/1", {
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        some: "value",
      }),
    });
    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource/1", {
      method: "PATCH",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        some: "value",
      }),
    });
  });
  it("should make DELETE request", async () => {
    await http.delete("some/resource/1", {
      headers: {
        Accept: "application/json",
      },
    });
    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource/1", {
      method: "DELETE",
      headers: {
        Accept: "application/json",
      },
    });
  });
  it("should make HEAD request", async () => {
    await http.head("some/resource/1");
    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource/1", {
      method: "HEAD",
    });
  });
  it("should make OPTIONS request", async () => {
    await http.options("some/resource/1");
    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource/1", {
      method: "OPTIONS",
    });
  });

  let unregister = () => {};

  afterEach(() => {
    unregister();
  });

  it("should use interceptors", async () => {
    unregister = $use(async (args, next) => {
      const { init = {} } = args;

      // modify headers
      init.headers = {
        ...init.headers,
        Accept: "application/json",
      };

      return next();
    });

    await http.get("some/resource/1");

    expect(fetch).toBeCalledTimes(1);
    expect(fetch).toBeCalledWith("some/resource/1", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });
  });

  it("should use block a request using an interceptor", async () => {
    unregister = $use(async (args, next) => {
      // do nothing and doesn't call next
    });
    await http.get("some/resource/1");
    expect(fetch).not.toBeCalled();
  });
});
