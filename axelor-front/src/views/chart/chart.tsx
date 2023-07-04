import { Box } from "@axelor/ui";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useSetAtom } from "jotai";
import unique from "lodash/uniq";

import { ChartView } from "@/services/client/meta.types";
import { ViewProps } from "../types";
import { useViewContext, useViewTab, useViewTabRefresh } from "@/view-containers/views/scope";
import { chart as fetchChart } from "@/services/client/meta";
import { useDashletHandlerAtom } from "@/view-containers/view-dashlet/handler";
import { download } from "@/utils/download";

import { ChartDataRecord, Chart as ChartComponent } from "./builder";
import { getChartData, getScale } from "./builder/utils";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { FormActionHandler } from "../form/builder/scope";
import { ActionOptions, DefaultActionExecutor } from "@/view-containers/action";
import { DataContext } from "@/services/client/data.types";
import classes from "./chart.module.scss";

export function Chart(props: ViewProps<ChartView>) {
  const { meta } = props;
  const chartName = meta.view.name!;
  const { dashlet } = useViewTab();
  const [view, setView] = useState<ChartView>();
  const [records, setRecords] = useState<ChartDataRecord[]>([]);
  const [legend, showLegend] = useState(true);

  const getContext = useViewContext();

  const actionExecutor = useMemo(() => {
    return new DefaultActionExecutor(
      new FormActionHandler(() => ({
        ...getContext(),
        _chart: chartName,
        _model: "com.axelor.script.ScriptBindings",
      }))
    );
  }, [getContext, chartName]);

  const loadView = useCallback(async () => {
    const view = await fetchChart<ChartView>(chartName, getContext());
    setView(view);
  }, [chartName, getContext]);

  const onRefresh = useCallback(async () => {
    const records = await fetchChart<ChartDataRecord[]>(
      chartName,
      getContext(),
      true
    );
    setRecords(records);
  }, [chartName, getContext]);

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
    await loadView();
    await onRefresh();
  }, [loadView, onRefresh]);

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
    <Box className={classes.chart}>
      {chartOptions && (
        <ChartComponent
          legend={legend}
          {...chartOptions}
          {...(hasAction && { onClick: onClickAction })}
        />
      )}
    </Box>
  );
}
