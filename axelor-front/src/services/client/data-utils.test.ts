import { vi } from "vitest";

vi.mock("@/views/form/builder/utils", () => ({
  compactJson: (value: unknown) => JSON.stringify(value),
  isReferenceField: () => false,
}));

import { updateRecord } from "./data-utils";

describe("updateRecord", () => {
  it("updates nested json fields without flattening the path", () => {
    const target = {
      attrs: JSON.stringify({
        details: {
          label: "Old",
          enabled: false,
        },
      }),
    };

    const result = updateRecord(
      target,
      {
        "attrs.details.label": "New",
      },
      undefined,
      {
        findJsonItem: (fieldName: string) =>
          fieldName === "attrs.details.label"
            ? ({ name: "details.label" } as any)
            : undefined,
      },
    );

    expect(result).toEqual({
      attrs: JSON.stringify({
        details: {
          label: "New",
          enabled: false,
        },
      }),
      _dirty: true,
    });
  });

  it("updates array values inside a json field from a dotted key", () => {
    const target = {
      attrs: JSON.stringify({
        code: "test",
        name: "Test",
        worldList: [{ id: 8, name: "Hi" }],
      }),
      id: 7,
      name: "Test",
      version: 2,
    };

    const worldList = [{ name: "w1", attrs: "..." }];
                                                                                                                                                 
    for (let i = 1; i <= 5; i++) {
        const worldName = target.name + " world " + i;
        worldList.push({ name: worldName, attrs: JSON.stringify({ name: worldName, message: "msg" + i }) });                                                              
    }
    
    const result = updateRecord(
      target,
      {
        "attrs.worldList": worldList,
      },
      undefined,
      {
        findJsonItem: (fieldName: string) =>
          fieldName === "attrs.worldList"
            ? ({ name: "worldList" } as any)
            : undefined,
      },
    );

    expect(result).toEqual({
      attrs: JSON.stringify({
        code: "test",
        name: "Test",
        worldList,
      }),
      id: 7,
      name: "Test",
      version: 2,
      _dirty: true,
    });
  });
});
