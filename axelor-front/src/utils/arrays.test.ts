import { groupBy, unique } from "./arrays";
import { deepEqual } from "./objects";

describe("array util tests", () => {
  it("unique", () => {
    const arr1 = [1, 2, 2, 3, 5, 4, 5];
    const arr2 = [{ a: 1 }, { b: 2 }, { a: 1 }, { b: 3 }, { a: 2 }];
    const arr3 = [{ a: 1 }, { b: 2 }, { c: 3 }];
    expect(unique(arr1)).toEqual([1, 2, 3, 5, 4]);
    expect(unique(arr2, deepEqual)).toEqual([
      { a: 1 },
      { b: 2 },
      { b: 3 },
      { a: 2 },
    ]);
    expect(unique(arr3)).toBe(arr3);
  });

  it("groupBy", () => {
    const data = [
      { group: "a", value: 1 },
      { group: "a", value: 2 },
      { group: "b", value: 1 },
      { group: "c", value: 1 },
      { group: "b", value: 2 },
      { group: "a", value: 3 },
      { group: "c", value: 2 },
    ];
    expect(groupBy(data, (x) => x.group)).toEqual({
      a: [
        { group: "a", value: 1 },
        { group: "a", value: 2 },
        { group: "a", value: 3 },
      ],
      b: [
        { group: "b", value: 1 },
        { group: "b", value: 2 },
      ],
      c: [
        { group: "c", value: 1 },
        { group: "c", value: 2 },
      ],
    });
  });
});
