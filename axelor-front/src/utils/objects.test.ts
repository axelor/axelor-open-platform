import { deepClone, deepEqual, deepGet, deepMerge, deepSet } from "./objects";
import { isPlainObject } from "./types";

describe("deepGet tests", () => {
  it("should get deep value", () => {
    const obj = {
      some: {
        nested: {
          value: 1,
          props: [{ name: "Some" }, { name: "Thing" }],
        },
        values: [1, 2, 3],
      },
    };

    expect(deepGet(obj, "some.thing")).toBeUndefined();
    expect(deepGet(obj, "some")).toBe(obj.some);
    expect(deepGet(obj, "some.nested")).toBe(obj.some.nested);
    expect(deepGet(obj, "some.nested.value")).toBe(obj.some.nested.value);
    expect(deepGet(obj, "some.nested.props[0]")).toBe(obj.some.nested.props[0]);
    expect(deepGet(obj, "some.nested.props[1].name")).toBe(
      obj.some.nested.props[1].name,
    );

    const arr = [{ name: "Some" }, { name: "Thing" }];
    expect(deepGet(arr, "[0]")).toBe(arr[0]);
    expect(deepGet(arr, "[1].name")).toBe(arr[1].name);
    expect(deepGet(arr, [1, "name"])).toBe(deepGet(arr, "[1].name"));
  });
});

describe("deepSet tests", () => {
  it("should set deep value", () => {
    const obj = {
      some: {
        nested: {
          value: 1,
          props: [{ name: "Some" }, { name: "Thing" }],
        },
        values: [1, 2, 3],
      },
    };

    deepSet(obj, "some.nested.value", 2);
    deepSet(obj, "some.nested.props.0.name", "SOME");
    deepSet(obj, "some.nested.props[2].name", "Else");
    deepSet(obj, "some.nested.values.0", 100);
    deepSet(obj, "some.values.3", 4);

    expect(obj).toEqual({
      some: {
        nested: {
          value: 2,
          props: [{ name: "SOME" }, { name: "Thing" }, { name: "Else" }],
          values: [100],
        },
        values: [1, 2, 3, 4],
      },
    });
  });
});

describe("deepEqual tests", () => {
  it("should check deep equality", () => {
    const obj = {
      some: {
        nested: {
          value: 1,
          props: [{ name: "Some" }, { name: "Thing" }],
        },
        values: [1, 2, 3],
      },
    };
    const same = {
      some: {
        nested: {
          value: 1,
          props: [{ name: "Some" }, { name: "Thing" }],
        },
        values: [1, 2, 3],
      },
    };
    const diff = {
      some: {
        nested: {
          value: 1,
          props: [{ name: "Some" }, { name: "Thing" }],
        },
        values: [1, 3, 3],
      },
    };
    expect(deepEqual(obj, same)).toBeTruthy();
    expect(deepEqual(obj, diff)).toBeFalsy();
  });

  it("should check deep equality with customisations", () => {
    const obj = {
      $some: 1,
      other: { value: 1 },
    };
    const other = {
      $some: 2,
      other: { value: 1 },
    };

    expect(
      deepEqual(obj, other, {
        ignore: (key) => typeof key === "string" && key.startsWith("$"),
      }),
    ).toBeTruthy();

    expect(
      deepEqual(obj, other, {
        ignore: (key) => String(key).startsWith("$"),
        equals: (a, b) => JSON.stringify(a) === JSON.stringify(b),
      }),
    ).toBeTruthy();

    const a = { items: [{ name: "Some" }, { name: "Thing" }] };
    const b = { items: [{ name: "Thing" }, { name: "Some" }] };
    expect(deepEqual(a, b)).toBeFalsy();
    expect(deepEqual(a, b, { strict: false })).toBeTruthy();
  });
});

describe("deepMerge tests", () => {
  it("should merge plain objects", () => {
    const source = {
      value: 1,
      nested: {
        name: "Some",
        items: [{ id: 1, name: "Item 1", qty: 1 }],
      },
      items: [1],
    };
    const target = {
      value: 3,
      active: true,
      nested: {
        value: 1,
        items: [
          { id: 2, name: "Item Two", price: 2.0 },
          { id: 2, name: "Item 2" },
        ],
      },
      items: [1, 2, 3],
    };

    const expected = {
      value: 3,
      nested: {
        name: "Some",
        items: [
          { id: 2, name: "Item Two", price: 2 },
          { id: 2, name: "Item 2" },
        ],
        value: 1,
      },
      items: [1, 2, 3],
      active: true,
    };

    expect(deepMerge(source, target)).toEqual(expected);
  });
  it("should merge with custom equal", () => {
    const source = {
      id: 1,
      name: "Some",
      customer: { id: 1, name: "Some NAME" },
      items: [
        { id: 1, product: { id: 1 }, qty: 1, price: 10.1, discount: 0.15 },
      ],
    };
    const target = {
      id: 1,
      customer: { id: 1, name: "Some NAME" },
      items: [
        { id: 1, product: { id: 1 }, qty: 2, price: 10.0 },
        { id: 2, product: { id: 1 }, qty: 1, price: 10.0 },
      ],
    };

    const expected = {
      id: 1,
      name: "Some",
      customer: { id: 1, name: "Some NAME" },
      items: [
        { id: 1, product: { id: 1 }, qty: 2, price: 10, discount: 0.15 },
        { id: 2, product: { id: 1 }, qty: 1, price: 10 },
      ],
    };

    const res = deepMerge(source, target, {
      equal: (a, b) => {
        if (isPlainObject(a) && isPlainObject(b)) {
          return a.id === b.id;
        }
        return a === b;
      },
    });

    expect(res).toEqual(expected);
    expect(res.customer).toStrictEqual(expected.customer);
  });
});

describe("deepClone tests", () => {
  it("should clone plain objects", () => {
    class Some {
      name = "";
      constructor(name: string) {
        this.name = name;
      }
      clone() {
        return new Some(this.name);
      }
    }

    const obj = {
      name: "Some",
      value: 1,
      date: new Date(),
      some: new Some("name"),
      items: [
        {
          id: 1,
          ref: {},
        },
      ],
    };

    obj.items[0].ref = obj;

    const cloned = deepClone(obj);

    expect(cloned).not.toBe(obj);

    // date
    expect(cloned.date).not.toBe(obj.date);
    expect(cloned.date).toEqual(obj.date);

    // clonable
    expect(cloned.some).not.toBe(obj.some);
    expect(cloned.some).toEqual(obj.some);

    // array
    expect(cloned.items).not.toBe(obj.items);
    expect(cloned.items).toEqual(obj.items);

    // circular
    expect(cloned.items[0].ref).not.toBe(obj.items[0].ref);
    expect(cloned.items[0].ref).toBe(cloned);
  });
});
