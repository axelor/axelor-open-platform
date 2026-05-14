import { describe, it, expect } from "vitest";
import { PlusData, PlotData, getDataZoom } from "./utils";

/**
 * Helper to build a bar-style dataset similar to:
 *   SELECT SUM(value) AS _value, year AS _year, product AS _product
 *   GROUP BY _year, _product
 *
 * Returns `count` products across 2 years, each with a deterministic value.
 */
function makeBarData(count: number, maxSeries?: number) {
  const dataset = [];
  for (let i = 0; i < count; i++) {
    dataset.push({
      _value: (i + 1) * 100,
      _year: "2024",
      _product: `Product_${i}`,
    });
    dataset.push({
      _value: (i + 1) * 50,
      _year: "2025",
      _product: `Product_${i}`,
    });
  }
  return {
    xAxis: "_year",
    series: [{ key: "_value", groupBy: "_product" }],
    scale: 2,
    dataset,
    config: maxSeries !== undefined ? { maxSeries: String(maxSeries) } : {},
  };
}

/**
 * Helper to build a pie-style dataset (xAxis = groupField, no separate groupBy).
 */
function makePieData(count: number, maxSeries?: number) {
  const dataset = [];
  for (let i = 0; i < count; i++) {
    dataset.push({
      _value: (i + 1) * 10,
      _product: `Product_${i}`,
    });
  }
  return {
    xAxis: "_product",
    series: [{ key: "_value" }],
    scale: 2,
    dataset,
    config: maxSeries !== undefined ? { maxSeries: String(maxSeries) } : {},
  };
}

/**
 * Helper to build a line-style dataset with many groupBy values.
 */
function makeLineData(count: number, maxSeries?: number) {
  const dataset = [];
  for (let i = 0; i < count; i++) {
    dataset.push({
      _value: (i + 1) * 10,
      _month: "Jan",
      _product: `Product_${i}`,
    });
    dataset.push({
      _value: (i + 1) * 20,
      _month: "Feb",
      _product: `Product_${i}`,
    });
  }
  return {
    xAxis: "_month",
    series: [{ key: "_value", groupBy: "_product" }],
    scale: 2,
    dataset,
    config: maxSeries !== undefined ? { maxSeries: String(maxSeries) } : {},
  };
}

describe("PlusData maxSeries capping", () => {
  describe("bar/hbar (groupField !== xAxis)", () => {
    it("should not cap when types <= maxSeries", () => {
      const data = makeBarData(5);
      const result = PlusData(data);
      expect(result.types).toHaveLength(5);
      expect(result.data).toHaveLength(2); // 2 years
    });

    it("should cap 500 products to default 200 (no Others for bar)", () => {
      const data = makeBarData(500);
      const result = PlusData(data);

      // Bar/hbar drops tail dimensions without "Others" to preserve Y-axis scale
      expect(result.types).toHaveLength(200);
      expect(result.types).not.toContain("Others");

      // Top products should be the highest-value ones
      expect(result.types).toContain("Product_499");
      expect(result.types).toContain("Product_498");

      // Each source row (year) should only have top dimensions
      for (const row of result.data) {
        const keys = Object.keys(row).filter(
          (k) => !["raw", "x", "y"].includes(k),
        );
        expect(keys.length).toBeLessThanOrEqual(200);
      }
    });

    it("should cap to custom maxSeries value", () => {
      const data = makeBarData(100, 10);
      const result = PlusData(data);

      expect(result.types).toHaveLength(10);
      expect(result.types).not.toContain("Others");
    });

    it("should disable capping when maxSeries is 0", () => {
      const data = makeBarData(100, 0);
      const result = PlusData(data);

      expect(result.types).toHaveLength(100);
      expect(result.types).not.toContain("Others");
    });

    it("should drop tail dimensions and keep only top types", () => {
      const data = makeBarData(5, 2);
      const result = PlusData(data);

      // Top 2 by value: Product_4 (500+250=750), Product_3 (400+200=600)
      expect(result.types).toContain("Product_4");
      expect(result.types).toContain("Product_3");
      expect(result.types).toHaveLength(2);
      expect(result.types).not.toContain("Others");

      // Tail dimensions should not be present in rows
      const row2024 = result.data.find((r: any) => r.x === "2024");
      expect(row2024?.["Product_0"]).toBeUndefined();
      expect(row2024?.["Product_1"]).toBeUndefined();
      expect(row2024?.["Product_2"]).toBeUndefined();
    });

    it("should keep top types sorted by total absolute value", () => {
      const data = makeBarData(10, 3);
      const result = PlusData(data);

      // Product_9 (total = 10*100 + 10*50 = 1500) should be in top
      // Product_8 (total = 9*100 + 9*50 = 1350) should be in top
      // Product_7 (total = 8*100 + 8*50 = 1200) should be in top
      expect(result.types).toContain("Product_9");
      expect(result.types).toContain("Product_8");
      expect(result.types).toContain("Product_7");
      expect(result.types).not.toContain("Product_0");
    });

    it("should handle invalid maxSeries config gracefully", () => {
      const data = makeBarData(100);
      data.config = { maxSeries: "abc" };
      const result = PlusData(data);

      // Should fall back to DEFAULT_MAX_SERIES (200)
      expect(result.types.length).toBeLessThanOrEqual(201);
    });
  });

  describe("bar/hbar with many xAxis categories (few types, many rows)", () => {
    /**
     * Simulates: few groupBy values (2 years) but many xAxis values (products).
     * xAxis categories are NOT capped — chart widgets use dataZoom instead.
     */
    function makeBarManyCategories(count: number) {
      const dataset = [];
      for (let i = 0; i < count; i++) {
        dataset.push({
          _value: (i + 1) * 100,
          _product: `Product_${i}`,
          _year: "2024",
        });
        dataset.push({
          _value: (i + 1) * 50,
          _product: `Product_${i}`,
          _year: "2025",
        });
      }
      return {
        xAxis: "_product",
        series: [{ key: "_value", groupBy: "_year" }],
        scale: 2,
        dataset,
        config: {},
      };
    }

    it("should preserve all xAxis categories (dataZoom handles scrolling)", () => {
      const data = makeBarManyCategories(200);
      const result = PlusData(data);

      // types = 2 (years), not capped
      expect(result.types).toContain("2024");
      expect(result.types).toContain("2025");

      // All 200 categories preserved — dataZoom will handle display
      expect(result.data).toHaveLength(200);
    });
  });

  describe("pie/donut/radar (groupField === xAxis)", () => {
    it("should cap 500 items to default 200 + Others", () => {
      const data = makePieData(500);
      const result = PlusData(data);

      expect(result.types.length).toBeLessThanOrEqual(201);
      expect(result.data.length).toBeLessThanOrEqual(201);
      expect(result.types).toContain("Others");

      // Highest-value product should be kept
      expect(result.types).toContain("Product_499");
    });

    it("should merge tail rows into Others with correct value", () => {
      const data = makePieData(5, 2);
      const result = PlusData(data);

      // Top 2: Product_4 (50), Product_3 (40)
      expect(result.types).toContain("Product_4");
      expect(result.types).toContain("Product_3");
      expect(result.types).toContain("Others");
      expect(result.data).toHaveLength(3);

      const othersRow = result.data.find((r: any) => r.x === "Others");
      // Others = 10 + 20 + 30 = 60
      expect(othersRow?.y).toBe("60");
    });

    it("should disable capping when maxSeries is 0", () => {
      const data = makePieData(100, 0);
      const result = PlusData(data);

      expect(result.types).toHaveLength(100);
      expect(result.data).toHaveLength(100);
    });

    it("should handle numeric xAxis values (string coercion)", () => {
      const dataset = [];
      for (let i = 0; i < 10; i++) {
        dataset.push({ _value: (i + 1) * 10, _year: 2020 + i });
      }
      const data = {
        xAxis: "_year",
        series: [{ key: "_value" }],
        scale: 2,
        dataset,
        config: { maxSeries: "3" },
      };
      const result = PlusData(data);

      // Should keep top 3 years + Others despite numeric keys
      expect(result.types).toHaveLength(4);
      expect(result.types).toContain("Others");
      // 2029 (100), 2028 (90), 2027 (80) should be top 3
      expect(
        result.data.find((r: any) => String(r.x) === "2029"),
      ).toBeTruthy();
      expect(
        result.data.find((r: any) => String(r.x) === "2028"),
      ).toBeTruthy();
      expect(
        result.data.find((r: any) => String(r.x) === "2027"),
      ).toBeTruthy();
    });
  });
});

/**
 * Count the total number of dimension keys across all data rows,
 * excluding internal fields (raw, x, y). This directly maps to the
 * number of ECharts series — the main driver of memory consumption.
 */
function countDimensionKeys(data: any[]): number {
  return data.reduce(
    (sum, row) =>
      sum +
      Object.keys(row).filter((k) => !["raw", "x", "y"].includes(k)).length,
    0,
  );
}

describe("maxSeries output size reduction", () => {
  it("bar: capped output should have far fewer dimension keys for 500 products", () => {
    const uncapped = PlusData(makeBarData(500, 0));
    const capped = PlusData(makeBarData(500)); // default 200

    // Uncapped: 500 types → 500 dimension keys per row × 2 rows = 1000
    expect(uncapped.types).toHaveLength(500);
    const uncappedKeys = countDimensionKeys(uncapped.data);

    // Capped: 200 types → 200 keys per row × 2 rows = 400
    expect(capped.types).toHaveLength(200);
    const cappedKeys = countDimensionKeys(capped.data);

    expect(cappedKeys).toBeLessThan(uncappedKeys / 2);
  });

  it("pie: capped output should have far fewer rows for 500 items", () => {
    const uncapped = PlusData(makePieData(500, 0));
    const capped = PlusData(makePieData(500)); // default 200

    expect(uncapped.data).toHaveLength(500);
    expect(capped.data.length).toBeLessThanOrEqual(201);
    // At least 2x fewer rows
    expect(capped.data.length).toBeLessThan(uncapped.data.length / 2);
  });

  it("line: capped output should have far fewer series for 10000 groups", () => {
    const uncapped = PlotData(makeLineData(10000, 0));
    const capped = PlotData(makeLineData(10000)); // default 200

    expect(uncapped.data).toHaveLength(10000);
    expect(capped.data.length).toBeLessThanOrEqual(201);
    // 10000 → 201 = ~49x fewer series
    expect(capped.data.length).toBeLessThan(uncapped.data.length / 20);
  });
});

describe("PlotData maxSeries capping", () => {
  it("should cap 10000 series to default 200 + Others", () => {
    const data = makeLineData(10000);
    const result = PlotData(data);

    expect(result.data.length).toBeLessThanOrEqual(201);
    expect(result.data.some((s: any) => s.key === "Others")).toBe(true);

    // Highest-value series should be kept
    expect(result.data.some((s: any) => s.key === "Product_9999")).toBe(true);
  });

  it("should cap to custom maxSeries value", () => {
    const data = makeLineData(100, 5);
    const result = PlotData(data);

    expect(result.data.length).toBeLessThanOrEqual(6); // 5 + Others
    expect(result.data.some((s: any) => s.key === "Others")).toBe(true);
  });

  it("should disable capping when maxSeries is 0", () => {
    const data = makeLineData(100, 0);
    const result = PlotData(data);

    expect(result.data).toHaveLength(100);
    expect(result.data.some((s: any) => s.key === "Others")).toBe(false);
  });

  it("should aggregate Others values correctly", () => {
    const data = makeLineData(5, 2);
    const result = PlotData(data);

    // Top 2: Product_4 (total=50+100=150), Product_3 (total=40+80=120)
    expect(result.data.some((s: any) => s.key === "Product_4")).toBe(true);
    expect(result.data.some((s: any) => s.key === "Product_3")).toBe(true);

    const others = result.data.find((s: any) => s.key === "Others");
    expect(others).toBeTruthy();
    // Others has 2 data points (Jan, Feb) aggregating Product_0, _1, _2
    expect(others.values).toHaveLength(2);

    // Jan: 10 + 20 + 30 = 60
    // Feb: 20 + 40 + 60 = 120
    const janVal = others.values.find((v: any) => v.x === "Jan");
    const febVal = others.values.find((v: any) => v.x === "Feb");
    expect(janVal?.y).toBe(60);
    expect(febVal?.y).toBe(120);
  });

  it("should not cap when series count <= maxSeries", () => {
    const data = makeLineData(5);
    const result = PlotData(data);

    expect(result.data).toHaveLength(5);
    expect(result.data.some((s: any) => s.key === "Others")).toBe(false);
  });

  it("should handle invalid maxSeries config gracefully", () => {
    const data = makeLineData(100);
    data.config = { maxSeries: "invalid" };
    const result = PlotData(data);

    // Falls back to DEFAULT_MAX_SERIES (200)
    expect(result.data.length).toBeLessThanOrEqual(201);
  });
});

describe("getDataZoom", () => {
  it("should return undefined when data fits within threshold", () => {
    expect(getDataZoom(10)).toBeUndefined();
    expect(getDataZoom(50)).toBeUndefined();
  });

  it("should return slider + inside zoom for large data", () => {
    const zoom = getDataZoom(100);
    expect(zoom).toHaveLength(2);
    expect(zoom![0]).toMatchObject({ type: "slider", xAxisIndex: 0 });
    expect(zoom![1]).toMatchObject({ type: "inside", xAxisIndex: 0 });
  });

  it("should calculate correct end percentage", () => {
    const zoom = getDataZoom(200);
    // 50/200 = 25%
    expect(zoom![0].end).toBe(25);
  });

  it("should support yAxis for horizontal bar charts", () => {
    const zoom = getDataZoom(100, "yAxis");
    expect(zoom![0]).toMatchObject({
      type: "slider",
      yAxisIndex: 0,
      orient: "vertical",
      right: 5,
      width: 20,
    });
    expect(zoom![1]).toMatchObject({ type: "inside", yAxisIndex: 0 });
  });

  it("should support custom maxVisible", () => {
    const zoom = getDataZoom(100, "xAxis", 20);
    // 20/100 = 20%
    expect(zoom![0].end).toBe(20);
  });
});
