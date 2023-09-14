import getObjValue from "lodash/get";
import { useCallback } from "react";

import { SelectOption } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";

const TR_PREFIX = "$tr:";

function getTrKey(targetName: string) {
  if (targetName?.includes(".")) {
    const ind = targetName.lastIndexOf(".") + 1;
    return `${targetName.slice(0, ind)}${TR_PREFIX}${targetName.slice(ind)}`;
  }
  return `${TR_PREFIX}${targetName}`;
}

function getLabel(option: SelectOption, key: string) {
  return getObjValue(option, getTrKey(key)) ?? getObjValue(option, key);
}

export function useOptionLabel({ targetName = "id", targetSearch }: Schema) {
  return useCallback(
    (option: SelectOption) => {
      let label = getLabel(option, targetName);

      if (typeof label === "object") {
        const names = [
          ...(Array.isArray(targetSearch) ? targetSearch : []),
          "id",
        ];
        label = names.map((key) => getLabel(label, key)).find((value) => value);
      }

      return label;
    },
    [targetName, targetSearch],
  );
}
