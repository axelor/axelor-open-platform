import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import get from "lodash/get";
import { useMemo } from "react";

import { Schema } from "@/services/client/meta.types";
import { DEFAULT_SCALE } from "@/utils/format";

import { FormAtom, WidgetAtom } from "../../builder";

export function useScale(
  widgetAtom: WidgetAtom,
  formAtom: FormAtom,
  schema: Schema & { name: string },
) {
  const { attrs } = useAtomValue(widgetAtom);
  const { scale: scaleAttr } = attrs;

  const isDecimal =
    schema.widget === "decimal" || schema.serverType === "DECIMAL";

  const scaleNum = useMemo(() => {
    if (!isDecimal) {
      return 0;
    }
    if (typeof scaleAttr === "number") {
      return Math.floor(scaleAttr);
    }
    if (scaleAttr == null || scaleAttr === "") {
      return DEFAULT_SCALE;
    }
    return parseInt(scaleAttr);
  }, [isDecimal, scaleAttr]);

  return useAtomValue(
    useMemo(
      () =>
        selectAtom(formAtom, (form) => {
          if (!isNaN(scaleNum)) {
            return scaleNum;
          }
          const value = parseInt(get(form.record, scaleAttr ?? ""));
          return isNaN(value) ? DEFAULT_SCALE : value;
        }),
      [formAtom, scaleAttr, scaleNum],
    ),
  );
}
