import { afterEach, vi } from "vitest";
import { fetcher } from "./fetcher";

vi.mock("../../http/http-fetch", () => ({ default: fetcher }));

export default function setupMock() {
  afterEach(() => {
    vi.resetAllMocks();
  });
}
