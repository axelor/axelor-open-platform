import { describe, expect, it, vi } from "vitest";

import { mergeValues } from "./executor";

vi.mock("@/components/alerts", () => ({}));
vi.mock("@/components/dialogs", () => ({}));
vi.mock("@/components/http-watch/use-block", () => ({}));
vi.mock("@/hooks/use-tabs", () => ({}));
vi.mock("@/layout/nav-tabs/utils", () => ({}));
vi.mock("@/services/client/meta", () => ({}));
vi.mock("@/utils/download", () => ({}));

describe("mergeValues", () => {
  it("merges root values whether or not an action returns the record id", () => {
    let values = mergeValues({}, { total: 42 });
    values = mergeValues(values, { id: 17, version: 1, total: 42 });
    values = mergeValues(values, { other: "x" });
    expect(values).toEqual({ id: 17, version: 1, total: 42, other: "x" });
  });

  it("replaces a relational value when its id changes", () => {
    const values = mergeValues(
      { partner: { id: 1, name: "A", code: "a" } },
      { partner: { id: 2, name: "B" } },
    );
    expect(values).toEqual({ partner: { id: 2, name: "B" } });
  });

  it("merges a relational value when its id is unchanged", () => {
    const values = mergeValues(
      { partner: { id: 1, name: "A", code: "a" } },
      { partner: { id: 1, name: "B" } },
    );
    expect(values).toEqual({ partner: { id: 1, name: "B", code: "a" } });
  });

  it("replaces a relational value set to a new record", () => {
    const values = mergeValues(
      { partner: { id: 1, name: "A", code: "a" } },
      { partner: { name: "B" } },
    );
    expect(values).toEqual({ partner: { name: "B" } });
  });

  it("merges collection rows matched by id below a root with an id", () => {
    const values = mergeValues(
      { items: [{ id: 1, price: 10, qty: 2 }] },
      {
        id: 17,
        items: [
          { id: 1, price: 20 },
          { id: 2, price: 5 },
        ],
      },
    );
    expect(values).toEqual({
      id: 17,
      items: [
        { id: 1, price: 20, qty: 2 },
        { id: 2, price: 5 },
      ],
    });
  });
});
