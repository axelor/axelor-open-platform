import isNumber from "lodash/isNumber";
import isEmpty from "lodash/isEmpty";
import isString from "lodash/isString";
import map from "lodash/map";
import filter from "lodash/filter";
import { parseExpression } from "@/hooks/use-parser/utils";

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

export function getSelectionList(
  context: any,
  selection: any,
  selectionIn: any,
  current: any
) {
  if (isEmpty(selection)) return selection;
  if (isEmpty(selectionIn)) return selection;

  let list = selectionIn;

  if (isString(selectionIn)) {
    let expr = selectionIn.trim();
    if (expr.indexOf("[") !== 0) {
      expr = "[" + expr + "]";
    }
    list = parseExpression(expr)(context);
  }

  if (isEmpty(list)) {
    return selection;
  }

  const value = acceptNumber(current);

  list = map(list, acceptNumber);

  return filter(selection, function (item) {
    const val = acceptNumber(item.value);
    return val === value || list.indexOf(val) > -1;
  });
}
