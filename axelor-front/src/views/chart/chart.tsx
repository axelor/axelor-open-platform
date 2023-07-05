import { Box } from "@axelor/ui";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useAtomValue, useSetAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";
import unique from "lodash/uniq";
import isString from "lodash/isString";

import {
  ChartView,
  FormView,
  Property,
  Schema,
} from "@/services/client/meta.types";
import { ViewProps } from "../types";
import {
  useViewContext,
  useViewTab,
  useViewTabRefresh,
} from "@/view-containers/views/scope";
import { ViewData, chart as fetchChart } from "@/services/client/meta";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { download } from "@/utils/download";

import { Form, FormAtom, useFormHandlers } from "../form/builder";
import { ChartDataRecord, Chart as ChartComponent } from "./builder";
import { getChartData, getScale } from "./builder/utils";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useGetErrors } from "../form";
import { FormActionHandler } from "../form/builder/scope";
import { ActionOptions, DefaultActionExecutor } from "@/view-containers/action";
import { DataContext } from "@/services/client/data.types";
import { processSelection, processWidget } from "@/services/client/meta-utils";
import { i18n } from "@/services/client/i18n";
import classes from "./chart.module.scss";

export function Chart(props: ViewProps<ChartView>) {
  const { meta } = props;
  const chartName = meta.view.name!;
  const getContext = useViewContext();
  const [view, setView] = useState<ChartView>();

  useAsyncEffect(async () => {
    const view = await fetchChart<ChartView>(chartName, getContext());
    setView(view);
  }, [chartName, getContext]);

  return view ? <ChartInner {...props} view={view} /> : null;
}

function ChartRefreshOnFormChanged({
  formAtom,
  onRefresh,
}: {
  formAtom: FormAtom;
  onRefresh: () => void;
}) {
  const { record } = useAtomValue(formAtom);

  useAsyncEffect(async () => {
    record && Object.keys(record).length > 0 && onRefresh();
  }, [record, onRefresh]);

  return null;
}

function ChartInner(props: ViewProps<ChartView> & { view: ChartView }) {
  const { meta, view } = props;
  const chartName = meta.view.name!;
  const { dashlet } = useViewTab();
  const [records, setRecords] = useState<ChartDataRecord[]>([]);
  const [legend, showLegend] = useState(true);
  const fetched = useRef(false);

  const formMeta = useMemo(() => {
    const model = "com.axelor.script.ScriptBindings";
    const fields: Record<string, Property> = (view.search || []).reduce(
      (fields, _item) => {
        const item = { ..._item };
        processSelection(item);
        processWidget(item);
        let type = item.type?.toUpperCase() || "STRING";
        if (type?.toLowerCase() === "reference") {
          type = "MANY_TO_ONE";
          item.canNew = "false";
          item.canEdit = "false";
        }
        return {
          ...fields,
          [item.name]: {
            ...item,
            ...item.widgetAttrs,
            type,
          },
        };
      },
      {}
    );
    const meta = {
      view: {
        type: "form",
        model,
        items: (view.search || []).length
          ? [
              {
                colSpan: 12,
                type: "panel",
                showFrame: false,
                css: classes.panel,
                items: (view.search || []).map((item) => ({
                  ...item,
                  ...fields?.[item.name ?? ""],
                  placeholder: item.title || item.autoTitle,
                  showTitle: false,
                  type: "field",
                  ...(item.multiple && (item.target || item.selection)
                    ? {
                        widget: item.target ? "tag-select" : "multi-select",
                      }
                    : {}),
                })),
              },
            ]
          : ([] as Schema[]),
      },
      model,
      fields,
    };
    return meta as unknown as ViewData<FormView>;
  }, [view]);

  const { formAtom, ...formProps } = useFormHandlers(
    formMeta,
    useRef({}).current
  );

  const getContext = useViewContext();
  const getErrors = useGetErrors();

  const actionExecutor = useMemo(() => {
    return new DefaultActionExecutor(
      new FormActionHandler(() => ({
        ...getContext(),
        _chart: chartName,
        _model: "com.axelor.script.ScriptBindings",
      }))
    );
  }, [getContext, chartName]);

  const onDataInit = useAtomCallback(
    useCallback(
      async (get, set) => {
        if (view?.onInit) {
          const res = await actionExecutor.execute(view.onInit, {
            context: getContext(),
          });
          // collect values from action result
          const values = res?.reduce?.(
            (obj, { values, attrs }) => ({
              ...obj,
              ...values,
              ...Object.keys(attrs || {}).reduce((values, key) => {
                const { value } = attrs?.[key] || {};
                return value !== undefined
                  ? {
                      ...values,
                      [key]: value,
                    }
                  : values;
              }, {}),
            }),
            {}
          );
          const formState = get(formAtom);
          set(formAtom, {
            ...formState,
            record: { ...formState.record, ...values },
          });
          return formMeta && Object.values(values || {}).length > 0;
        }
        return false;
      },
      [formMeta, formAtom, actionExecutor, view.onInit, getContext]
    )
  );

  const onRefresh = useAtomCallback(
    useCallback(
      async (get) => {
        const state = get(formAtom);
        const errors = getErrors(state);

        if (errors) {
          return;
        }

        const context = { ...getContext(), ...state.record };

        Object.keys(formMeta.fields || {}).forEach((name) => {
          const field: any = formMeta.fields?.[name];
          if (field && field.multiple && (field.target || field.selection)) {
            let value = context[name];
            if (Array.isArray(value)) value = value.map((v) => v.id);
            if (isString(value)) value = value.split(/\s*,\s*/g);
            context[name] = value;
          } else if (field?.target && view.usingSQL) {
            const value = context[name];
            if (value) {
              context[name] = value.id;
            }
          }
        });

        const records = await fetchChart<ChartDataRecord[]>(
          chartName,
          context,
          true
        );
        fetched.current = true;
        setRecords(records);
      },
      [chartName, view, formMeta, formAtom, getErrors, getContext]
    )
  );

  const onExport = useCallback(async () => {
    if (!view || !records) return;

    const name = (view.title || "export").toLowerCase().replace(/ /g, "_");
    const header = records.reduce(
      (list, row) => unique([...(list as []), ...Object.keys(row)]),
      []
    );
    let content = "data:text/csv;charset=utf-8," + header.join(";") + "\n";

    records.forEach((item) => {
      let row = header.map((key: string) => {
        let val = item[key];
        if (val === undefined || val === null) {
          val = "";
        }
        return '"' + ("" + val).replace(/"/g, '""') + '"';
      });
      content += row.join(";") + "\n";
    });

    download(encodeURI(content), `${name}.csv`);
  }, [view, records]);

  const onAction = useCallback(
    async (action: string, options?: ActionOptions) => {
      return actionExecutor.execute(action, options);
    },
    [actionExecutor]
  );

  const onClickAction = useCallback(
    async (record?: ChartDataRecord) => {
      return onAction(view?.config?.onClick!, {
        context: { ...record, _signal: "onClick" },
      });
    },
    [view, onAction]
  );

  const onDatasetAction = useCallback(
    async (action: string, context?: DataContext) => {
      return onAction(action, {
        context: { _data: records, ...context },
      });
    },
    [records, onAction]
  );

  const setDashletHandlers = useSetAtom(useDashletHandlerAtom());

  useAsyncEffect(async () => {
    const initialized = await onDataInit();
    !initialized && (await onRefresh());
  }, [onDataInit, onRefresh]);

  const chartOptions = useMemo(() => {
    if (view) {
      const { config, series: [{ type }] = [] } = view;
      if (type && (records?.length || ["gauge"].includes(type))) {
        const scale = getScale(view, records);
        return {
          type,
          data: {
            ...view,
            scale,
            dataset: getChartData(view, records, {
              ...config,
              scale,
            }),
          },
        };
      }
    }
    return null;
  }, [view, records]);

  useEffect(() => {
    if (view && dashlet) {
      setDashletHandlers({
        view,
        onLegendShowHide: showLegend,
        onAction: onDatasetAction,
        onRefresh,
        onExport,
      });
    }
  }, [dashlet, view, onDatasetAction, onRefresh, onExport, setDashletHandlers]);

  // register tab:refresh
  useViewTabRefresh("chart", onRefresh);

  const hasAction = view?.config?.onClick;
  return (
    <Box className={classes.chart} borderTop={dashlet}>
      {view && (formMeta?.view?.items?.length ?? 0) > 0 && (
        <Box w={100} p={2}>
          <Form
            schema={formMeta.view}
            fields={formMeta.fields}
            readonly={false}
            formAtom={formAtom}
            {...(formProps as any)}
          />
          <ChartRefreshOnFormChanged
            formAtom={formAtom}
            onRefresh={onRefresh}
          />
        </Box>
      )}
      {chartOptions ? (
        <ChartComponent
          legend={legend}
          {...chartOptions}
          {...(hasAction && { onClick: onClickAction })}
        />
      ) : (
        fetched.current && (
          <Box
            d="flex"
            flex={1}
            justifyContent={"center"}
            alignItems="center"
            className={classes["noRecordsText"]}
          >
            {i18n.get("No records found.")}
          </Box>
        )
      )}
    </Box>
  );
}
