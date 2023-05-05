import isNumber from "lodash/isNumber";
import isEmpty from "lodash/isEmpty";
import isString from "lodash/isString";
import map from "lodash/map";
import filter from "lodash/filter";
import { useAtomValue } from "jotai";

import { parseExpression } from "@/hooks/use-parser/utils";
import { Schema, Selection } from "@/services/client/meta.types";
import { useFormScope } from "../../builder/scope";
import { WidgetAtom } from "../../builder";
import { useEffect, useMemo, useState } from "react";
import { isEqual } from "lodash";

function acceptNumber(value?: null | string | number) {
  if (value === null || value === undefined) {
    return value;
  }
  if (isNumber(value)) {
    return +value;
  }
  if (/^(-)?\d+(\.\d+)?$/.test(value)) {
    return +value;
  }
  return value;
}

const getSelectionIn = (obj: Schema) =>
  obj?.["selection-in"] || obj.selectionIn;

export function useSelectionList({
  value,
  schema,
  widgetAtom,
}: {
  value: any;
  schema: Schema;
  widgetAtom: WidgetAtom;
}) {
  const { recordHandler } = useFormScope();
  const [filterList, setFilterList] = useState<null | any[]>([]);
  const { attrs } = useAtomValue(widgetAtom);
  const selectionIn = (getSelectionIn(attrs) ||
    getSelectionIn(schema)) as string;

  useEffect(() => {
    const { selectionList = [] } = schema;

    if (selectionIn && selectionList?.length > 0) {
      let list: any = selectionIn ?? null;

      return recordHandler.subscribe((record) => {
        if (isString(selectionIn)) {
          let expr = selectionIn.trim();
          if (!expr.startsWith("[")) {
            expr = "[" + expr + "]";
          }
          list = parseExpression(expr)(record);
        }

        if (isEmpty(list) || !Array.isArray(list)) {
          list = null;
        } else {
          list = map(list, acceptNumber);
        }
        setFilterList((_list) => (isEqual(_list, list) ? _list : list));
      });
    }

    setFilterList(null);
  }, [recordHandler, schema, selectionIn]);

  return useMemo<Selection[]>(() => {
    const { selectionList = [] } = schema;
    if (!filterList) return selectionList || [];
    const currentValue = acceptNumber(value);

    return filter(selectionList || [], function (item) {
      const val = acceptNumber(item.value);
      return val === currentValue || filterList.indexOf(val) > -1;
    });
  }, [schema, filterList, value]);
}
