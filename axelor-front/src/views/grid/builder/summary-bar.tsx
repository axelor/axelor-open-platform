import {
  Fragment,
  memo,
  Ref,
  useImperativeHandle,
  useMemo,
  useState,
} from "react";

import { Box, clsx } from "@axelor/ui";
import { GridState, getNumberScale } from "@axelor/ui/grid";

import {
  Field,
  SummaryBarItem,
  SummaryCallItem,
  type SummaryBar,
  SummaryItem,
} from "@/services/client/meta.types";
import { DataRecord } from "@/services/client/data.types";
import { l10n } from "@/services/client/l10n";
import { ActionExecutor, ActionOptions } from "@/view-containers/action";
import styles from "./summary-bar.module.scss";

function median(numbers: number[]) {
  if (numbers.length === 0) {
    return 0;
  }

  const sorted = [...numbers].sort((a, b) => a - b);

  const mid = Math.floor(sorted.length / 2);

  if (sorted.length % 2 !== 0) {
    return sorted[mid];
  }

  return (sorted[mid - 1] + sorted[mid]) / 2;
}

function isNumeric(val: unknown): boolean {
  if (val === null || val === undefined || val === "") return false;
  if (typeof val === "boolean" || Array.isArray(val)) return false;
  return !isNaN(Number(val));
}

const SummaryValue = memo(function SummaryValue({
  title,
  value,
}: {
  title: string;
  value: number | string;
}) {
  return (
    <Box p={2}>
      <span className={styles.label}>{title} : </span>
      <span className={styles.value}>{value}</span>
    </Box>
  );
});

const SummaryCallField = memo(function SummaryCallField(
  props: SummaryCallItem & { value: string },
) {
  const { serverType, title, value } = props;

  const val = useMemo(() => {
    if (serverType?.toUpperCase() === "DECIMAL" && isNumeric(value)) {
      const scale = getNumberScale(Number(value));
      // strip trailing zeros and format
      return l10n.formatNumber(Number(value), {
        minimumFractionDigits: scale,
        maximumFractionDigits: scale,
      });
    }
    return value;
  }, [serverType, value]);

  return <SummaryValue title={title} value={val} />;
});

const SummaryField = memo(function SummaryField(
  props: SummaryBarItem & { state: GridState },
) {
  const { title, aggregate, on, name, state } = props;
  const { rows, selectedRows, columns } = state;

  const fieldColumn = useMemo(
    () => columns?.find((c) => c.name === name),
    [columns, name],
  );

  const value = useMemo(() => {
    const isSupported = ["DECIMAL", "INTEGER", "LONG"].includes(
      (fieldColumn as Field)?.serverType ?? "",
    );
    if (!fieldColumn || !isSupported) return null;

    const values = (
      on === "selection"
        ? (selectedRows ?? [])
            .map((ind) => rows[ind])
            .filter((row) => row && row.type === "row")
        : rows.filter((row) => row.type === "row")
    )
      .map((row) => {
        const record = row.record!;
        return fieldColumn.valueGetter
          ? fieldColumn.valueGetter(fieldColumn, record)
          : record[name];
      })
      .filter((v) => v != null);

    if (!values.length) return null;

    switch (aggregate) {
      case "sum":
      case "avg": {
        const total = values.reduce((_total, val) => _total + Number(val), 0);
        const floatingDigits = Math.max(0, ...values.map(getNumberScale));
        return Number(
          aggregate === "avg"
            ? Number(total / values.length).toFixed(floatingDigits)
            : Number(total).toFixed(floatingDigits),
        );
      }
      case "count":
        return values.length;
      case "max":
        return Math.max(...values.map((val) => (isNaN(val) ? 0 : Number(val))));
      case "min":
        return Math.min(...values.map((val) => (isNaN(val) ? 0 : Number(val))));
      case "median":
        return median(values.map(Number));
      default:
        return null;
    }
  }, [aggregate, selectedRows, on, rows, name, fieldColumn]);

  const formattedValue = useMemo(() => {
    if (!fieldColumn?.formatter || value == null) {
      return value;
    }

    const scale =
      (fieldColumn as Field)?.serverType?.toUpperCase() === "DECIMAL"
        ? getNumberScale(value)
        : undefined;

    return fieldColumn.formatter({ ...fieldColumn, scale }, value, {
      [name]: value,
    });
  }, [fieldColumn, name, value]);

  return (
    formattedValue != null && (
      <SummaryValue title={title} value={formattedValue} />
    )
  );
});

export type SummaryBarHandler = {
  refresh?: (recordIds: number[], data: ActionOptions["data"]) => void;
};

export function SummaryBar({
  state,
  actionExecutor,
  handler,
  data,
  callAction = true,
}: {
  state: GridState;
  data: SummaryBar;
  handler?: Ref<SummaryBarHandler>;
  actionExecutor?: ActionExecutor;
  callAction?: boolean;
}) {
  const { items, hint } = data;
  const call = callAction ? data.call : null;

  const hasSelected = useMemo(() => {
    return (state.selectedRows ?? [])
      .map((ind) => state.rows[ind])
      .some((row) => row.type === "row");
  }, [state.selectedRows, state.rows]);

  const [actionValues, setActionValues] = useState<DataRecord>({});

  const actionItems = useMemo(
    () =>
      (call?.items ?? []).filter((item) => actionValues[item.name] !== null),
    [call?.items, actionValues],
  );

  const displayItems = useMemo(
    () =>
      (items || []).filter((item) =>
        item.on === "selection" ? hasSelected : true,
      ),
    [items, hasSelected],
  );

  const [startItems, endItems] = useMemo(() => {
    return [
      [
        ...displayItems.filter((item) => item.align !== "end"),
        ...actionItems.filter((item) => item.align !== "end"),
      ],
      [
        ...displayItems.filter((item) => item.align === "end"),
        ...actionItems.filter((item) => item.align === "end"),
      ],
    ];
  }, [displayItems, actionItems]);

  const showHint =
    !hasSelected &&
    actionItems.length === 0 &&
    displayItems.every((item) => item.on === "selection");

  useImperativeHandle(handler, () => {
    if (!call?.action) return {};
    return {
      refresh: (_pageIds: number[], actionData: ActionOptions["data"]) => {
        actionExecutor
          ?.execute(call.action, { data: actionData, context: { _pageIds } })
          .then((res) => {
            const values = (res || []).reduce(
              (_values, result) => ({ ..._values, ...result.values }),
              {},
            );
            setActionValues(values);
          });
      },
    };
  }, [actionExecutor, call?.action]);

  function renderItem(item: SummaryItem, ind: number) {
    return (
      <Fragment key={ind}>
        {(item as SummaryBarItem).aggregate ? (
          <SummaryField {...(item as SummaryBarItem)} state={state} />
        ) : actionValues[item.name] != null ? (
          <SummaryCallField
            {...(item as SummaryCallItem)}
            value={actionValues[item.name]}
          />
        ) : null}
      </Fragment>
    );
  }

  return (
    <Box shadow w={100} d="flex" flex={0} gap={5} className={styles.summaryBar}>
      {showHint && hint && (
        <Box d="flex" flex={1} p={2}>
          {hint}
        </Box>
      )}
      {startItems.length > 0 && (
        <Box d="flex" className={clsx(styles.section)}>
          {startItems.map(renderItem)}
        </Box>
      )}
      {endItems.length > 0 && (
        <Box d="flex" className={clsx(styles.section, styles.endSection)}>
          {endItems.map(renderItem)}
        </Box>
      )}
    </Box>
  );
}
