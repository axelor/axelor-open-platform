import { act, render, screen } from "@testing-library/react";
import { afterEach, vi } from "vitest";
import { useAsync } from "./use-async";

const App = ({ loader }: { loader: () => Promise<any> }) => {
  const { state, data, error } = useAsync(loader);
  if (state === "loading") {
    return <div data-testid="loading">Loading...</div>;
  }
  if (state === "hasError") {
    return <div data-testid="error">Error: {error}</div>;
  }
  return <div data-testid="data">{data}</div>;
};

const loader = async (value: string) => {
  return new Promise<string>((resolve, reject) => {
    setTimeout(() => {
      if (value.includes("failed")) {
        reject(value);
      } else {
        resolve(value);
      }
    }, 100);
  });
};

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.runOnlyPendingTimers();
  vi.useRealTimers();
});

describe("use-async tests", () => {
  it("should load async data", async () => {
    render(<App loader={() => loader("Hi!")} />);
    expect(screen.getByTestId("loading")).toHaveTextContent("Loading...");
    await act(() => vi.advanceTimersByTime(100));
    const data = screen.getByTestId("data");
    expect(data).toHaveTextContent("Hi!");
  });

  it("should catch async error", async () => {
    render(<App loader={() => loader("failed!")} />);
    expect(screen.getByTestId("loading")).toHaveTextContent("Loading...");
    await act(() => vi.advanceTimersByTime(100));
    const error = screen.getByTestId("error");
    expect(error).toHaveTextContent("failed!");
  });
});
