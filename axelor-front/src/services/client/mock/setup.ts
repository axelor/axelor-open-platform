import { afterEach, vi } from "vitest";
import { fetcher } from "./fetcher";

export default function setupMock() {
  vi.mock("../../http/http-fetch", () => ({ default: fetcher }));
  afterEach(() => {
    vi.resetAllMocks();
  });
}
