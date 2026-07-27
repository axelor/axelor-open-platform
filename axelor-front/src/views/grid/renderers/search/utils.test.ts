import { describe, expect, it } from "vitest";

import { Field } from "@/services/client/meta.types";
import { moment } from "@/services/client/l10n";
import { getSearchFilter } from "./utils";

describe("getSearchFilter date parsing", () => {
  // explicit dateFormat so results don't depend on the test runner's locale
  const dateField = {
    name: "orderDate",
    type: "DATE",
    dateFormat: "DD/MM/YYYY",
  } as unknown as Field;
  const fields = { orderDate: dateField };

  it("filters by single-digit month and year (M/YYYY)", () => {
    const filter = getSearchFilter(fields, [], { orderDate: "6/2026" });
    expect(filter?.criteria?.[0]).toEqual({
      operator: "and",
      criteria: [
        { fieldName: "orderDate", operator: ">=", value: "2026-06-01" },
        { fieldName: "orderDate", operator: "<", value: "2026-07-01" },
      ],
    });
  });

  it("filters by single-digit year and month (YYYY/M)", () => {
    const filter = getSearchFilter(fields, [], { orderDate: "2026/6" });
    expect(filter?.criteria?.[0]).toEqual({
      operator: "and",
      criteria: [
        { fieldName: "orderDate", operator: ">=", value: "2026-06-01" },
        { fieldName: "orderDate", operator: "<", value: "2026-07-01" },
      ],
    });
  });

  it("filters by single-digit day and month (D/M, default DD/MM order)", () => {
    const currentYear = moment().year();
    const filter = getSearchFilter(fields, [], { orderDate: "6/5" });
    // default locale order is DD/MM, so "6/5" means day=6, month=5
    expect(filter?.criteria?.[0]).toEqual({
      operator: "and",
      criteria: [
        { fieldName: "orderDate", operator: ">=", value: `${currentYear}-05-06` },
        { fieldName: "orderDate", operator: "<", value: `${currentYear}-05-07` },
      ],
    });
  });

  it("filters by full single-digit date (D/M/YYYY, default DD/MM/YYYY order)", () => {
    const filter = getSearchFilter(fields, [], { orderDate: "6/5/2026" });
    expect(filter?.criteria?.[0]).toEqual({
      operator: "and",
      criteria: [
        { fieldName: "orderDate", operator: ">=", value: "2026-05-06" },
        { fieldName: "orderDate", operator: "<", value: "2026-05-07" },
      ],
    });
  });

  it("still filters by zero-padded month and year (MM/YYYY)", () => {
    const filter = getSearchFilter(fields, [], { orderDate: "06/2026" });
    expect(filter?.criteria?.[0]).toEqual({
      operator: "and",
      criteria: [
        { fieldName: "orderDate", operator: ">=", value: "2026-06-01" },
        { fieldName: "orderDate", operator: "<", value: "2026-07-01" },
      ],
    });
  });

  it("still filters by zero-padded full date (DD/MM/YYYY)", () => {
    const filter = getSearchFilter(fields, [], { orderDate: "06/05/2026" });
    expect(filter?.criteria?.[0]).toEqual({
      operator: "and",
      criteria: [
        { fieldName: "orderDate", operator: ">=", value: "2026-05-06" },
        { fieldName: "orderDate", operator: "<", value: "2026-05-07" },
      ],
    });
  });
});
