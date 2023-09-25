import { useSetAtom } from "jotai";
import { forwardRef, useCallback, useEffect, useMemo } from "react";

import { DataRecord } from "@/services/client/data.types";
import {
  CustomView,
  Field,
  GridView,
  Property,
  Schema,
  Widget,
} from "@/services/client/meta.types";
import { download } from "@/utils/download";
import { toKebabCase, toTitleCase } from "@/utils/names";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { Grid as GridComponent } from "@/views/grid/builder";
import { useGridState } from "@/views/grid/builder/utils";
import format from "@/utils/format";
import { i18n } from "@/services/client/i18n";

function formatter(column: Field, value: any, record: any) {
  if (column.translatable && toKebabCase(column.type) === "string") {
    const trKey = "value:" + value;
    const trValue = i18n.get(trKey);
    if (trValue !== trKey) {
      record = { ...record, ["$t:" + column.name]: trValue };
    }
  }
  return format(value, {
    props: column,
    context: record,
  });
}

export const ReportTable = forwardRef(function ReportTable(
  {
    context,
    columns,
    sums,
    view,
  }: {
    view?: CustomView;
    context: Record<string, any>;
    columns?: string;
    sums?: string;
  },
  ref
) {
  const [state, setState] = useGridState();
  const records = useMemo(() => context.data || [], [context]);

  const onSearch = useCallback(() => {}, []);

  const defaultColumnNames = useMemo<string>(() => {
    const [first] = records || [];
    return Object.keys(first || {})
      .filter((name) => name !== "$$hashKey")
      .join(",");
  }, [records]);

  const { view: gridView, fields } = useMemo(() => {
    const sumCols = (sums || "").split(/\s*,\s*/);
    const names = (columns ?? defaultColumnNames).split(/\s*,\s*/);
    const fields: Record<string, Property> = {};
    const viewItems: Widget[] = [];

    names.forEach((name) => {
      const item: Schema =
        view?.items?.find((item) => item.name === name) ?? {};
      if (item && !item.hidden) {
        const col = Object.assign({}, item, item.widgetAttrs, {
          name: name,
          title: item.title || item.autoTitle || toTitleCase(name),
          type: toKebabCase((item as Field).serverType || "STRING").toUpperCase(),
          serverType: toKebabCase((item as Field).serverType || "STRING").toUpperCase(),
          ...(sumCols.includes(name) && { aggregate: "sum" }),
        });
        fields[name] = col as Property;
        viewItems.push(col);
      }
    });

    return {
      fields,
      view: { ...view, type: "grid", items: viewItems } as GridView,
    };
  }, [sums, columns, view, defaultColumnNames]);

  const onExport = useCallback(async () => {
    const view = gridView;
    const { items = [] } = view;

    const header = items.map((col) => col.title);
    let content = "data:text/csv;charset=utf-8," + header.join(";") + "\n";

    records.forEach((record: DataRecord) => {
      const row = items
        .map((col) => col.name)
        .map(function (key) {
          let val = key && record[key];
          if (val === undefined || val === null) {
            val = "";
          }
          return '"' + ("" + val).replace(/"/g, '""') + '"';
        });
      content += row.join(";") + "\n";
    });
    const name = (view.title || "export").toLowerCase().replace(/ /g, "_");
    download(encodeURI(content), name + ".csv");
  }, [records, gridView]);

  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useEffect(() => {
    setDashletHandlers((state) => ({
      ...state,
      view: gridView,
      onExport,
    }));
  }, [gridView, setDashletHandlers, onExport]);

  return (
    <GridComponent
      showEditIcon={false}
      records={records}
      aggregationType="all"
      view={gridView}
      fields={fields}
      state={state}
      setState={setState}
      allowCheckboxSelection={false}
      columnFormatter={formatter}
      onSearch={onSearch as any}
    />
  );
});
