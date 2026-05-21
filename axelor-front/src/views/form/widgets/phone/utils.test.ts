import { describe, expect, it } from "vitest";

import { normalizePastedPhoneValue } from "./utils";

describe("normalizePastedPhoneValue", () => {
  it("normalizes a French national number with trunk prefix", async () => {
    await expect(
      normalizePastedPhoneValue("0608691275", "fr"),
    ).resolves.toEqual({
      phone: "+33608691275",
      countryIso2: "fr",
    });
  });

  it("ignores incomplete pasted numbers", async () => {
    await expect(normalizePastedPhoneValue("0608", "fr")).resolves.toBeNull();
  });
});
