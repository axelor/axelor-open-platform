// setup mock
import setupMock from "./mock/setup";
setupMock();

import { DataSource } from "./data";

describe("data tests", async () => {
  it("should search records", async () => {
    const ds = new DataSource("com.axelor.sale.db.Order");
    const res = await ds.search({
      limit: 5,
      offset: 0,
      fields: ["id", "version", "totalAmount", "customer", "orderDate"],
      sortBy: ["-totalAmount"],
      filter: {
        operator: "and",
        criteria: [
          {
            operator: ">=",
            fieldName: "totalAmount",
            value: "1000",
          },
        ],
      },
    });

    // the mock loader doesn't filter/sort/page/select
    expect(res).toBeDefined();
    expect(res.page?.totalCount).toBeGreaterThan(0);
    expect(res.records).toBeDefined();
    expect(res.records.length).toBeGreaterThan(0);
    expect(res.records[0].id).toBeGreaterThan(0);
  });

  it("should read a record", async () => {
    const ds = new DataSource("com.axelor.contact.db.Contact");
    const res = await ds.read(1, {
      fields: ["id", "version", "firstName", "lastName", "fullName"],
      related: {
        addresses: ["street", "area", "city"],
      },
    });
    expect(res).toBeDefined();
    expect(res.id).toBeDefined();
  });

  it("should save a record", async () => {
    const ds = new DataSource("com.axelor.contact.db.Contact");
    const res = await ds.save({
      firstName: "Some",
      lastName: "NAME",
    });
    expect(res).toBeDefined();
    expect(res.id).toBeDefined();
  });

  it("should delete records", async () => {
    const ds = new DataSource("com.axelor.contact.db.Contact");
    const res = await ds.delete({
      id: 1,
      version: 0,
    });
    expect(res).toBe(1);
  });

  it("should copy a record", async () => {
    const ds = new DataSource("com.axelor.contact.db.Contact");
    const res = await ds.copy(1);
    expect(res).toBeDefined();
    expect(res.id).toBeUndefined();
    expect(res.fullName).toBeDefined();
  });

  it("should export records", async () => {
    const ds = new DataSource("com.axelor.contact.db.Contact");
    const res = await ds.export({
      fields: ["firstName", "lastName", "email"],
      filter: {
        criteria: [
          {
            fieldName: "title.code",
            operator: "=",
            value: "mr",
          },
        ],
      },
    });
    expect(res).toBeDefined();
    expect(res.fileName).toBeDefined();
  });
});
