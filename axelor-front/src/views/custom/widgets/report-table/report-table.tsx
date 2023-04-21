import { forwardRef, useCallback, useEffect, useMemo } from "react";
import { useGridState } from "@/views/grid/builder/utils";
import {
  CustomView,
  Field,
  GridView,
  Property,
  Widget,
} from "@/services/client/meta.types";
import { Grid as GridComponent } from "@/views/grid/builder";
import { toTitleCase } from "@/utils/names";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { useSetAtom } from "jotai";
import { download } from "@/utils/download";
import { DataRecord } from "@/services/client/data.types";

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
      const item = view?.items?.find((item) => item.name === name);
      if (item && !item.hidden) {
        const col = Object.assign({}, item, item.widgetAttrs, {
          name: name,
          title: item.title || item.autoTitle || toTitleCase(name),
          type: (item as Field).serverType || "STRING",
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
      onSearch={onSearch as any}
    />
  );
});
