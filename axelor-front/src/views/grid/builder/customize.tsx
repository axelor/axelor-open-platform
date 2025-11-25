import { Box, Input, InputLabel, Panel } from "@axelor/ui";
import { Grid, GridProvider, GridState } from "@axelor/ui/grid";
import { useCallback, useEffect, useMemo, useState } from "react";
import { WritableAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";

import { DialogButton, dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";
import { Field, GridView, JsonField } from "@/services/client/meta.types";
import { DataRecord } from "@/services/client/data.types";
import { MetaData, resetView } from "@/services/client/meta";
import { saveView } from "@/services/client/meta-cache";
import { session } from "@/services/client/session";
import { isUserAllowedCustomizeViews } from "@/utils/app-settings.ts";
import { useViewMeta } from "@/view-containers/views/scope";

import { CustomizeSelectorDialog } from "./customize-selector";
import { useGridState } from "./utils";
import styles from "./customize.module.scss";

const reload = () => window.location.reload();

type ViewHandler = (state?: GridState) => GridView | undefined;

function CustomizeDialog({
  title = i18n.get("Columns"),
  view,
  jsonFields,
  canShare,
  onUpdate,
}: {
  title?: string;
  view: GridView;
  jsonFields?: MetaData["jsonFields"];
  canShare?: boolean;
  onUpdate?: (fn: ViewHandler) => void;
}) {
  const [state, setState] = useGridState();
  const [saveWidths, setSaveWidths] = useState(false);
  const [shared, setShared] = useState(false);
  const [records, setRecords] = useState(
    (view.items || [])
      .map((item, ind) => ({
        id: ind + 1,
        ...item,
        title: item.title || item.autoTitle,
      }))
      .filter((item) => item.hidden !== true) as DataRecord[],
  );

  const currentFieldsInView: string[] = useMemo(
    () => records.map((r) => r.name) as string[],
    [records],
  );

  const { selectedRows } = state;

  const columns = useMemo(
    () => [
      { title: i18n.get("Title"), name: "title" },
      { title: i18n.get("Name"), name: "name" },
    ],
    [],
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
            (v) => v.name === record.name,
          ) || {
            name: record.name,
            type: record.type,
          };
          if (saveWidths && schemaItem.type === "field") {
            const mainGridItem = gridState?.columns?.find(
              (c) => c.name === record.name && c.computed && c.width,
            );
            if (mainGridItem) {
              (schemaItem as Field).width = `${parseInt(
                String(mainGridItem.width)!,
              )}`;
            }
          }
          if ((schemaItem as JsonField).jsonField) {
            return {
              name: schemaItem.name,
              type: "field",
            };
          }
          return schemaItem;
        });

      view.customViewShared = shared;

      return {
        ...view,
        items,
      } as GridView;
    },
    [view, shared, state.rows, saveWidths, records],
  );

  const handleSelect = useCallback(async () => {
    let selected: DataRecord[] = [];

    await dialogs.modal({
      open: true,
      title: i18n.get("Columns"),
      content: (
        <CustomizeSelectorDialog
          view={view}
          jsonFields={jsonFields}
          excludeFields={currentFieldsInView}
          onSelectionChange={(selection: DataRecord[]) => {
            selected = selection;
          }}
        />
      ),
      size: "lg",
      onClose: (isOk) => {
        if (isOk) {
          setRecords((_records) => [
            ..._records,
            ...(selected || [])
              .filter((s) => !_records.find((r) => r.name === s.name))
              .map((record) => ({
                ...record,
                title: record.label,
              })),
          ]);
        }
      },
    });
  }, [currentFieldsInView, jsonFields, view]);

  const handleRemove = useCallback(async () => {
    const confirmed = await dialogs.confirm({
      content: i18n.get("Do you really want to delete the selected record(s)?"),
    });
    if (confirmed) {
      setRecords((_records) =>
        _records.filter((r, ind) => !selectedRows?.includes(ind))
      );
    }
  }, [selectedRows]);

  useEffect(() => {
    onUpdate?.(getSavedView);
  }, [onUpdate, getSavedView]);

  return (
    <Box d="flex" flexDirection="column" flex={1} p={3}>
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
            allowCheckboxSelection
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
          <InputLabel d="flex" alignItems="center" gap={8}>
            <Input
              data-input
              type="checkbox"
              checked={saveWidths}
              onChange={() => setSaveWidths(!saveWidths)}
            />
            {i18n.get("Save column widths")}
          </InputLabel>
        </div>
        {canShare && (
          <div className={styles.checkbox}>
            <InputLabel d="flex" alignItems="center" gap={8}>
              <Input
                data-input
                type="checkbox"
                checked={shared}
                onChange={() => setShared(!shared)}
              />
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
  allowCustomization = true,
}: {
  view?: GridView;
  stateAtom: WritableAtom<GridState, any, any>;
  allowCustomization?: boolean;
}) {
  const canCustomize =
    allowCustomization && view?.name && isUserAllowedCustomizeViews();

  const {
    meta: { jsonFields },
  } = useViewMeta();

  const showCustomizeDialog = useAtomCallback(
    useCallback(
      async (get, set, { title }: { title?: string }) => {
        if (!view) return;

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
                        "Are you sure you want to reset this view customization?",
                      ),
                    });
                    if (confirmed) {
                      await resetView(view);
                      fn(false);
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
              const _view = getView?.(gridState);
              if (_view) {
                await saveView(_view);
                fn(true);
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
              jsonFields={jsonFields}
              title={title}
              canShare={canShare}
              onUpdate={(fn) => {
                getView = fn;
              }}
            />
          ),
          buttons,
          size: "lg",
          onClose: () => {},
        });
      },
      [view, stateAtom, jsonFields],
    ),
  );

  return canCustomize ? showCustomizeDialog : undefined;
}
