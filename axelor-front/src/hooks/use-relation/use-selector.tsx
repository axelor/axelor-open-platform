import { useCallback, useEffect, useMemo } from "react";

import { Box, CommandBar, CommandItemProps } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { PageText } from "@/components/page-text";
import { SearchOptions } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import { findView } from "@/services/client/meta-cache";
import { GridView } from "@/services/client/meta.types";
import { Grid } from "@/views/grid/builder";
import { useGridState } from "@/views/grid/builder/utils";

import { useDataStore } from "../use-data-store";
import styles from "./use-selector.module.scss";

export type SelectorProps = {
  model: string;
  title: string;
  multiple?: boolean;
  canCreate?: boolean;
  viewName?: string;
  domain?: string;
  context?: DataContext;
  onSelect?: (records: DataRecord[]) => void;
};

export function useSelector() {
  const select = useCallback(async (props: SelectorProps) => {
    const { onSelect, ...rest } = props;
    const { title, model, domain, context, viewName, multiple } = props;

    const dataStore = new DataStore(model, {
      filter: {
        _domain: domain,
        _domainContext: context,
      },
    });

    const meta = await findView<GridView>({
      type: "grid",
      name: viewName,
      model,
    });

    let records: DataRecord[] = [];

    const onSelectionChange = async (
      cellIndex: number,
      selected: DataRecord[]
    ) => {
      records = selected;
      if (multiple && cellIndex === 0) return;
      if (records.length > 0) {
        await close(true);
        onSelect?.(selected);
      }
    };

    const content = (
      <SelectorGrid
        meta={meta}
        dataStore={dataStore}
        onSelectionChange={onSelectionChange}
        {...rest}
      />
    );

    const header = <SelectorHeader dataStore={dataStore} />;

    const close = await dialogs.modal({
      title,
      content,
      header,
      size: "xl",
      classes: {
        header: styles.header,
      },
      buttons: [
        {
          name: "cancel",
          title: i18n.get("Cancel"),
          variant: "secondary",
          onClick(fn) {
            fn(false);
          },
        },
        {
          name: "select",
          title: i18n.get("Select"),
          variant: "primary",
          onClick(fn) {
            onSelect?.(records);
            fn(true);
          },
        },
      ],
    });
  }, []);

  return select;
}

function SelectorHeader({ dataStore }: { dataStore: DataStore }) {
  const page = useDataStore(dataStore, (state) => state.page);
  const { offset = 0, limit = 0, totalCount = 0 } = page;

  const onNext = useCallback(() => {
    const nextOffset = Math.min(offset + limit, totalCount);
    dataStore.search({ offset: nextOffset });
  }, [dataStore, limit, offset, totalCount]);

  const onPrev = useCallback(() => {
    const nextOffset = Math.max(offset - limit, 0);
    dataStore.search({ offset: nextOffset });
  }, [dataStore, limit, offset]);

  const commands = useMemo(() => {
    const items: CommandItemProps[] = [
      {
        key: "prev",
        iconOnly: true,
        iconProps: {
          icon: "navigate_before",
        },
        disabled: offset === 0,
        onClick: onPrev,
      },
      {
        key: "next",
        iconOnly: true,
        iconProps: {
          icon: "navigate_next",
        },
        disabled: offset + limit >= totalCount,
        onClick: onNext,
      },
    ];
    return items;
  }, [limit, offset, onNext, onPrev, totalCount]);

  return (
    <Box d="flex" alignItems="center" g={2}>
      <PageText dataStore={dataStore} />
      <CommandBar items={commands} />
    </Box>
  );
}

function SelectorGrid({
  meta,
  multiple,
  dataStore,
  onSelectionChange,
}: SelectorProps & {
  meta: ViewData<GridView>;
  dataStore: DataStore;
  onSelectionChange?: (cellIndex: number, records: DataRecord[]) => void;
}) {
  const { view, fields } = meta;
  const [state, setState] = useGridState({});

  const onSearch = useCallback(
    (options: SearchOptions = {}) => dataStore.search(options),
    [dataStore]
  );

  useEffect(() => {
    const selectedRows = state.selectedRows || [];
    const records = selectedRows.map((i) => state.rows[i].record);
    onSelectionChange?.(state.selectedCell?.[1] ?? 0, records);
  }, [onSelectionChange, state.rows, state.selectedCell, state.selectedRows]);

  return (
    <Grid
      dataStore={dataStore}
      view={view}
      fields={fields}
      state={state}
      setState={setState}
      onSearch={onSearch}
      showEditIcon={false}
      allowSelection
      selectionType={multiple ? "multiple" : "single"}
      allowCheckboxSelection={multiple}
    />
  );
}
