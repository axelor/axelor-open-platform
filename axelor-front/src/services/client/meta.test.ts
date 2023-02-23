// setup mock
import setupMock from "./mock/setup";
setupMock();

import * as meta from "./meta";

describe("meta tests", async () => {
  it("should load all menus", async () => {
    const menus = await meta.menus("all");
    expect(menus).toBeDefined();
    expect(menus.length).toBeGreaterThan(0);
    expect(menus[0].name).toBeDefined();
  });

  it("should load quick menus", async () => {
    const menus = await meta.menus("quick");
    expect(menus).toBeDefined();
    expect(menus.length).toBeGreaterThan(0);
    expect(menus[0].title).toBeDefined();
  });

  it("should load favorite menus", async () => {
    const menus = await meta.menus("fav");
    expect(menus).toBeDefined();
    expect(menus.length).toBeGreaterThan(0);
    expect(menus[0].name).toBeDefined();
  });

  it("should load action view", async () => {
    const view = await meta.actionView("sale.orders");
    expect(view).toBeDefined();
    expect(view?.viewType).toBe("grid");
    expect(view?.model).toBe("com.axelor.sale.db.Order");
    expect(view?.views?.length).toBeGreaterThan(0);
  });

  it("should load search filters", async () => {
    const filters = await meta.filters("filter-sales");
    expect(filters).toBeDefined();
    expect(filters.length).toBeGreaterThan(0);
    expect(filters[0].filterView).toBe("filter-sales");
    expect(filters[0].filterCustom).toBeDefined();
  });

  it("should load model meta", async () => {
    const res = await meta.fields("com.axelor.sale.order.Order");
    expect(res).toBeDefined();
    expect(res.fields).toBeDefined();
  });

  it("should load views", async () => {
    const res = await meta.view("com.axelor.sale.order.Order", {
      type: "form",
    });
    expect(res).toBeDefined();
    expect(res.fields).toBeDefined();
    expect(res.view).toBeDefined();
  });

  it("should execute an action", async () => {
    const res = await meta.action({
      action: "calculate-amount",
      model: "com.axelor.sale.db.Order",
    });
    expect(res).toBeDefined();
    expect(res.length).toBeGreaterThan(0);
    expect(res[0]).toHaveProperty("values");
  });
});
