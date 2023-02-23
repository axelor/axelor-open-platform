import { render, waitFor } from "@testing-library/react";
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
    });
  });
};

describe("use-async tests", () => {
  it("should load async data", async () => {
    const res = render(<App loader={() => loader("Hi!")} />);
    expect(res.getByTestId("loading")).toHaveTextContent("Loading...");
    const data = await waitFor(() => res.getByTestId("data"));
    expect(data).toHaveTextContent("Hi!");
  });

  it("should catch async error", async () => {
    const res = render(<App loader={() => loader("failed!")} />);
    const error = await waitFor(() => res.getByTestId("error"));
    expect(error).toHaveTextContent("failed!");
  });
});
