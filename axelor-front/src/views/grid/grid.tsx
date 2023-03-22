import { useAtom } from "jotai";
import { atomWithImmer } from "jotai-immer";
import { useCallback, useMemo } from "react";

import _ from "lodash";
import {
  Grid as AxGrid,
  GridProvider as AxGridProvider,
  GridState,
} from "@axelor/ui/grid";

import { useAsync } from "@/hooks/use-async";
import { useDataStore } from "@/hooks/use-data-store";
import { Field, JsonField, GridView } from "@/services/client/meta.types";
import format from "@/utils/format";
import { ViewToolBar } from "@/view-containers/view-toolbar";

import { ViewProps } from "../types";
import { useViewProps } from "@/view-containers/views/scope";
import styles from "./grid.module.scss";

function formatter(column: Field, value: any, record: any) {
  return format(value, {
    props: column,
    context: record,
  });
}

export function Grid(props: ViewProps<GridView>) {
  const { meta, dataStore } = props;
  const { view, fields } = meta;
  const [viewProps, setViewProps] = useViewProps();

  const [state, setState] = useAtom(
    useMemo(
      () =>
        atomWithImmer<GridState>({
          rows: [],
          columns: [],
          selectedCell: viewProps?.selectedCell,
          selectedRows: [viewProps?.selectedCell?.[0]!],
        }),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      []
    )
  );

  const records = useDataStore(dataStore, (ds) => ds.records);

  const { columns, names } = useMemo(() => {
    const names: string[] = [];
    const columns = view.items!.map((item) => {
      const field = fields?.[item.name!];
      const title = item.title ?? item.autoTitle;
      const attrs = item.widgetAttrs;
      const serverType = field?.type;

      if ((item as JsonField).jsonField) {
        names.push((item as JsonField).jsonField as string);
      } else if (field) {
        names.push(field.name);
      }

      return {
        ...field,
        ...item,
        ...attrs,
        serverType,
        title,
        formatter,
      };
    });

    return { columns, names: _.uniq(names) };
  }, [view, fields]);

  const onSearch = useCallback(async () => {
    await dataStore.search({ fields: names });
  }, [dataStore, names]);

  const init = useAsync(async () => {
    if (dataStore.records.length === 0) {
      onSearch();
    }
  }, [dataStore]);

  useEffect(() => {
    if (viewProps?.selectedCell !== state.selectedCell) {
      setViewProps({ selectedCell: state.selectedCell ?? undefined });
    }
  }, [viewProps, setViewProps, state.selectedCell]);

  if (init.state === "loading") return null;

  return (
    <div className={styles.grid}>
      <ViewToolBar
        meta={meta}
        actions={[
          {
            key: "new",
            text: "New",
            iconProps: {
              icon: "add",
            },
          },
          {
            key: "edit",
            text: "Edit",
            iconProps: {
              icon: "edit",
            },
          },
          {
            key: "save",
            text: "Save",
            iconProps: {
              icon: "save",
            },
          },
          {
            key: "delete",
            text: "Delete",
            iconProps: {
              icon: "delete",
            },
            items: [
              {
                key: "archive",
                text: "Archive",
              },
              {
                key: "unarchive",
                text: "Unarchive",
              },
            ],
            onClick: () => {},
          },
          {
            key: "refresh",
            text: "Refresh",
            iconProps: {
              icon: "refresh",
            },
            onClick: () => onSearch(),
          },
        ]}
      />
      <AxGridProvider>
        <AxGrid
          allowColumnResize
          allowGrouping
          allowSorting
          allowSelection
          allowCheckboxSelection
          allowCellSelection
          allowColumnHide
          allowColumnOptions
          allowColumnCustomize
          sortType="state"
          selectionType="multiple"
          records={records}
          columns={columns}
          state={state}
          setState={setState}
        />
      </AxGridProvider>
    </div>
  );
}
