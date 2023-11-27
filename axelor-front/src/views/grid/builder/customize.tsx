import { Box, Input, InputLabel, Panel } from "@axelor/ui";
import { Grid, GridProvider, GridState } from "@axelor/ui/grid";
import { useCallback, useEffect, useMemo, useState } from "react";
import { WritableAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";

import { DialogButton, dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";
import { Field, GridView } from "@/services/client/meta.types";
import { useGridState } from "./utils";
import { DataRecord } from "@/services/client/data.types";
import { resetView } from "@/services/client/meta";
import { saveView } from "@/services/client/meta-cache";
import { session } from "@/services/client/session";
import { useSelector } from "@/hooks/use-relation";
import { toTitleCase } from "@/utils/names";
import styles from "./customize.module.scss";

const reload = () => window.location.reload();

type ViewHandler = (state?: GridState) => GridView | undefined;

function CustomizeDialog({
  title = i18n.get("Columns"),
  view,
  canShare,
  onUpdate,
}: {
  title?: string;
  view: GridView;
  canShare?: boolean;
  onUpdate?: (fn: ViewHandler) => void;
}) {
  const [state, setState] = useGridState();
  const [saveWidths, setSaveWidths] = useState(false);
  const [shared, setShared] = useState(false);
  const [records, setRecords] = useState(
    (view.items || []).map((item, ind) => ({
      id: ind + 1,
      ...item,
      title: item.title || item.autoTitle,
    })) as DataRecord[]
  );
  const showSelector = useSelector();

  const { selectedRows } = state;

  const columns = useMemo(
    () => [
      { title: i18n.get("Title"), name: "title" },
      { title: i18n.get("Name"), name: "name" },
    ],
    []
  );

  const getSavedView = useCallback(
    (gridState?: GridState) => {
      if (!records.some((c) => c.type || c.type === "field")) {
        dialogs.error({
          content: i18n.get("Grid requires at least one field."),
        });
        return;
      }

      const items = (state.rows || [])
        .map((r) => r.record)
        .filter((r) => records.includes(r))
        .map((record) => {
          const schemaItem = view.items?.find(
            (v) => v.name === record.name
          ) || {
            name: record.name,
            type: "field",
          };
          if (saveWidths && schemaItem.type === "field") {
            const mainGridItem = gridState?.columns?.find(
              (c) => c.name === record.name && c.computed && c.width
            );
            mainGridItem &&
              ((schemaItem as Field).width = `${parseInt(
                String(mainGridItem.width)!
              )}`);
          }
          return schemaItem;
        });

      view.customViewShared = shared;

      return {
        ...view,
        items,
      } as GridView;
    },
    [view, shared, state.rows, saveWidths, records]
  );

  const handleSelect = useCallback(() => {
    showSelector({
      model: "com.axelor.meta.db.MetaField",
      title: i18n.get("Columns"),
      multiple: true,
      domain:
        "self.metaModel.fullName = :_modelName AND self.name NOT IN :_excludedFieldNames",
      context: {
        _excludedFieldNames: ["id", "version"],
        _model: "com.axelor.meta.db.MetaField",
        _modelName: view.model,
      },
      onSelect: (selected) => {
        setRecords((records) => [
          ...records,
          ...(selected || [])
            .filter((s) => !records.find((r) => r.name === s.name))
            .map((record) => ({
              ...record,
              title: record.title ?? toTitleCase(record.name),
            })),
        ]);
      },
    });
  }, [showSelector, view.model]);

  const handleRemove = useCallback(async () => {
    const confirmed = await dialogs.confirm({
      content: i18n.get("Do you really want to delete the selected record(s)?"),
    });
    confirmed &&
      setRecords((records) =>
        records.filter((r, ind) => !selectedRows?.includes(ind))
      );
  }, [selectedRows]);

  useEffect(() => {
    onUpdate?.(getSavedView);
  }, [onUpdate, getSavedView]);

  return (
    <Box flex={1} p={3}>
      <Panel
        className={styles.panel}
        header={title}
        toolbar={{
          items: [
            {
              key: "select",
              text: i18n.get("Select"),
              iconProps: {
                icon: "search",
              },
              onClick: handleSelect,
            },
            {
              key: "remove",
              text: i18n.get("Remove"),
              iconProps: {
                icon: "close",
              },
              hidden: (selectedRows?.length ?? 0) === 0,
              onClick: handleRemove,
            },
          ],
        }}
      >
        <GridProvider>
          <Grid
            allowRowReorder
            allowSelection
            allowCellSelection
            selectionType="multiple"
            records={records as DataRecord[]}
            columns={columns}
            state={state}
            setState={setState}
          />
        </GridProvider>
      </Panel>
      <Panel>
        <div className={styles.checkbox}>
          <Input
            data-input
            type="checkbox"
            checked={saveWidths}
            onChange={() => setSaveWidths(!saveWidths)}
          />
          <InputLabel ms={2}>{i18n.get("Save column widths")}</InputLabel>
        </div>
        {canShare && (
          <div className={styles.checkbox}>
            <Input
              data-input
              type="checkbox"
              checked={shared}
              onChange={() => setShared(!shared)}
            />
            <InputLabel ms={2}>
              {i18n.get("Apply as default for all users")}
            </InputLabel>
          </div>
        )}
      </Panel>
    </Box>
  );
}

export function useCustomizePopup({
  view,
  stateAtom,
}: {
  view: GridView;
  stateAtom: WritableAtom<GridState, any, any>;
}) {
  return useAtomCallback(
    useCallback(
      async (get, set, { title }: { title?: string }) => {
        const gridState = get(stateAtom);
        const canShare =
          (session?.info?.user?.viewCustomizationPermission ?? 0) > 1;
        const canReset =
          view.customViewId && (!view.customViewShared || canShare);

        let getView: ViewHandler;

        const buttons: DialogButton[] = (
          canReset
            ? [
              {
                name: "reset",
                title: i18n.get("Reset"),
                variant: "danger",
                onClick: async (fn) => {
                  const confirmed = await dialogs.confirm({
                    content: i18n.get(
                      "Are you sure you want to reset this view customization?"
                    ),
                  });
                  if (confirmed) {
                    fn(false);
                    await resetView(view);
                    reload();
                  }
                },
              } as DialogButton,
            ]
            : []
        ).concat([
          {
            name: "cancel",
            title: i18n.get("Close"),
            variant: "secondary",
            onClick(fn) {
              fn(false);
            },
          },
          {
            name: "confirm",
            title: i18n.get("OK"),
            variant: "primary",
            onClick: async (fn) => {
              const view = getView?.(gridState);
              if (view) {
                fn(true);
                await saveView(view);
                reload();
              }
            },
          },
        ]);

        await dialogs.modal({
          open: true,
          title,
          content: (
            <CustomizeDialog
              view={view}
              title={title}
              canShare={canShare}
              onUpdate={(fn) => {
                getView = fn;
              }}
            />
          ),
          buttons,
          size: "lg",
          onClose: () => { },
        });
      },
      [view, stateAtom]
    )
  );
}
