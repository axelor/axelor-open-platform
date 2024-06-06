import { useAtom } from "jotai";
import { atomWithImmer } from "jotai-immer";
import { useCallback, useMemo } from "react";
import getObjValue from "lodash/get";

import { GridColumn, GridSortColumn, GridState } from "@axelor/ui/grid";

import { Field, GridView, Property } from "@/services/client/meta.types";
import { DataRecord } from "@/services/client/data.types";
import { MetaData } from "@/services/client/meta";
import { l10n } from "@/services/client/l10n";
import { toKebabCase } from "@/utils/names.ts";
import { isValidWidget, normalizeWidget } from "@/views/form/builder/utils";
import { getFieldValue } from "@/utils/data-record";

export function useGridState(
  initialState?: Partial<GridState> & {
    view?: GridView;
    params?: Record<string, any>;
  },
  deps = [],
) {
  const { view, params, ...gridState } = initialState || {};
  const groupBy: string = params?.groupBy || view?.groupBy;
  const orderBy: string = params?.orderBy || view?.orderBy;

  const gridAtom = useMemo(
    () =>
      atomWithImmer<GridState>({
        rows: [],
        columns: [],
        ...(groupBy && {
          groupBy: groupBy.split(/\s*,\s*/).map((name) => ({ name })),
        }),
        ...(orderBy && {
          orderBy: parseOrderBy(orderBy),
        }),
        ...gridState,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    deps,
  );
  const [state, setState] = useAtom(gridAtom);
  return [state, setState, gridAtom] as const;
}

export function parseOrderBy(orderBy?: string): GridSortColumn[] | undefined {
  return orderBy
    ?.split(/\s*,\s*/)
    .map((name) =>
      name.startsWith("-")
        ? { name: name.slice(1), order: "desc" }
        : { name, order: "asc" },
    );
}

export function getSortBy(orderBy?: GridSortColumn[] | null) {
  return orderBy?.map(
    (column) => `${column.order === "desc" ? "-" : ""}${column.name}`,
  );
}

export function getWidget(item: any, field: any): string {
  let widget = item.widget;

  // default widget depending on field server type
  if (!isValidWidget(item.widget)) {
    widget = item.serverType;
  }

  // default image fields
  if (!item.widget && field?.image) {
    widget = "image";
  }

  // adapt widget naming, ie boolean-select to BooleanSelect
  widget = normalizeWidget(widget) ?? widget;

  return toKebabCase(widget);
}

export function isValidSequence(field: Property) {
  const { type, name } = field;
  return (
    ["INTEGER", "LONG"].includes(type) &&
    !["id", "version"].includes(name) &&
    !name.includes(".")
  );
}

function sortDataByColumns(
  list: DataRecord[],
  sortColumns: GridSortColumn[],
  extractor: (data: DataRecord, key: string) => any = (
    item: DataRecord,
    key: string,
  ) => item[key],
) {
  if (!sortColumns.some((c) => c.name === "id")) {
    sortColumns = [
      ...sortColumns,
      { name: "id", order: sortColumns[0]?.order ?? "asc" },
    ];
  }

  const locale = l10n.getLocale();
  const localeCompare = Intl.Collator(locale).compare;

  function toLocaleString(value?: string) {
    return (value || "").toLocaleString().toLocaleLowerCase();
  }

  function compare(first: any, second: any) {
    if (first == null) {
      return 1;
    }
    if (second == null) {
      return -1;
    }
    if (isNaN(first) || isNaN(second)) {
      return localeCompare(toLocaleString(first), toLocaleString(second));
    }
    return first - second;
  }

  function rcompare(first: any, second: any) {
    return compare(second, first);
  }

  function comparator(first: DataRecord, second: DataRecord) {
    for (let i = 0; i < sortColumns.length; ++i) {
      const { name, order } = sortColumns[i];
      const cmp = order === "desc" ? rcompare : compare;
      const result = cmp(extractor(first, name), extractor(second, name));
      if (result) {
        return result;
      }
    }
    return 0;
  }

  return [...list].sort(comparator);
}

export function getFieldSortValue(column: Field, record: DataRecord) {
  const { name } = column;

  if (record?.[`$t:${name}`]) {
    return record[`$t:${name}`];
  }

  const value = getFieldValue(record, column as Field);

  if (Array.isArray(value)) {
    return value.length;
  } else if (value && typeof value === "object") {
    const targetName = column.targetName || "id";
    return (
      getObjValue(value, `$t:${targetName}`) || getObjValue(value, targetName)
    );
  }
  return value;
}

export function useGridSortHandler(
  fields: MetaData["fields"] = {},
  getSortValue: (column: Field, record: DataRecord) => any = getFieldSortValue,
) {
  return useCallback(
    (
      data: DataRecord[],
      sortColumns: GridSortColumn[],
      allColumns: GridColumn[],
    ) => {
      return sortDataByColumns(
        data,
        sortColumns,
        (record: DataRecord, key: string) => {
          const column = allColumns.find((c) => c.name === key);
          const field = {
            ...column,
            ...(fields?.[key] || { name: key }),
          } as unknown as Field;
          return getSortValue(field, record);
        },
      );
    },
    [fields, getSortValue],
  );
}
