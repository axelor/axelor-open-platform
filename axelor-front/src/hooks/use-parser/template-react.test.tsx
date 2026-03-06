import { render, screen, waitFor } from "@testing-library/react";

import { parseSafe } from "./parser";

// Minimal stand-in for @axelor/ui Box, mirroring how processReactTemplate
// provides COMPONENTS via a Proxy context wrapper.
function Box(props: any) {
  const { as: Tag = "div", children, ...rest } = props;
  return <Tag {...rest}>{children}</Tag>;
}

const COMPONENTS: Record<string, unknown> = { Box };

function createContext(data: Record<string, unknown>) {
  return new Proxy(data, {
    get(target, p, receiver) {
      return p in COMPONENTS
        ? COMPONENTS[p as string]
        : Reflect.get(target, p, receiver);
    },
  });
}

function renderTemplate(template: string, data: Record<string, unknown>) {
  const fn = parseSafe(template);
  return fn(createContext(data));
}

describe("react template with safe globals", () => {
  it("should render Object.entries + Array.isArray", async () => {
    const data = {
      activity: {
        tasks: [
          { id: 1, title: "Task A" },
          { id: 2, title: "Task B" },
        ],
        notes: "some string",
      },
    };

    render(
      <div data-testid="root">
        {renderTemplate(
          `
          <>
            {Object.entries(activity).map(([key, value], index) => (
              Array.isArray(value) &&
              <Box key={index} data-testid={"section-" + key}>
                <Box as="dt">{key}</Box>
                {value.map((item, i) => (
                  <Box key={i} data-testid={key + "-" + i}>{item.title}</Box>
                ))}
              </Box>
            ))}
          </>
          `,
          data
        )}
      </div>
    );

    const root = await waitFor(() => screen.getByTestId("root"));
    expect(root).toBeInTheDocument();
    expect(screen.getByTestId("section-tasks")).toBeInTheDocument();
    expect(screen.getByTestId("tasks-0")).toHaveTextContent("Task A");
    expect(screen.getByTestId("tasks-1")).toHaveTextContent("Task B");
    expect(screen.queryByTestId("section-notes")).toBeNull();
  });

  it("should render parseFloat with toFixed", async () => {
    const data = {
      currency: { symbol: "€", numberOfDecimals: 2 },
      amount: "1234.5",
    };

    render(
      <div>
        {renderTemplate(
          `
          <Box data-testid="amount">
            {currency.symbol} {parseFloat(amount).toFixed(currency.numberOfDecimals)}
          </Box>
          `,
          data
        )}
      </div>
    );

    const el = await waitFor(() => screen.getByTestId("amount"));
    expect(el).toHaveTextContent("€ 1234.50");
  });

  it("should render Array.from with map", async () => {
    render(
      <div>
        {renderTemplate(
          `
          <Box data-testid="array-from">
            {Array.from(["a","b"]).map((v, i) => <Box key={i}>{v}</Box>)}
          </Box>
          `,
          {}
        )}
      </div>
    );

    const el = await waitFor(() => screen.getByTestId("array-from"));
    expect(el).toHaveTextContent("ab");
  });

  it("should render Math and Number in templates", async () => {
    render(
      <div>
        {renderTemplate(
          `
          <Box data-testid="math">
            {Math.abs(value)} | {Math.round(value)} | {Number.isFinite(value) ? "finite" : "not"}
          </Box>
          `,
          { value: -42.7 }
        )}
      </div>
    );

    const el = await waitFor(() => screen.getByTestId("math"));
    expect(el).toHaveTextContent("42.7 | -43 | finite");
  });

  it("should handle undefined from object lookup with nullish coalescing", async () => {
    const data = { status: "DRAFT" };

    render(
      <div>
        {renderTemplate(
          `
          <Box data-testid="badge">
            {
              {
                "OPEN": "warning",
                "CLOSED": "success",
              }[status] ?? "primary"
            }
          </Box>
          `,
          data
        )}
      </div>
    );

    const el = await waitFor(() => screen.getByTestId("badge"));
    // "DRAFT" is not in the map, so it should fall back to "primary"
    expect(el).toHaveTextContent("primary");
  });

  it("should render JSON.parse in templates", async () => {
    render(
      <div>
        {renderTemplate(
          `
          <Box data-testid="json">
            {JSON.parse(data).name} is {JSON.parse(data).age}
          </Box>
          `,
          { data: '{"name":"Alice","age":30}' }
        )}
      </div>
    );

    const el = await waitFor(() => screen.getByTestId("json"));
    expect(el).toHaveTextContent("Alice is 30");
  });
});
