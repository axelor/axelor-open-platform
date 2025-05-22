import { Box, Input, InputLabel, Panel } from "@axelor/ui";
import { Grid, GridProvider, GridState } from "@axelor/ui/grid";
import { useCallback, useEffect, useMemo, useState } from "react";
import { WritableAtom } from "jotai";
import { useAtomCallback } from "jotai/utils";
import forEach from "lodash/forEach";

import { DialogButton, dialogs } from "@/components/dialogs";
import { i18n } from "@/services/client/i18n";
import { Field, GridView } from "@/services/client/meta.types";
import { useGridState } from "./utils";
import { DataRecord } from "@/services/client/data.types";
import { resetView } from "@/services/client/meta";
import { saveView } from "@/services/client/meta-cache";
import { session } from "@/services/client/session";
import { useSelector } from "@/hooks/use-relation";
import { nextId } from "@/views/form/builder/utils";
import { isUserAllowedCustomizeViews } from "@/utils/app-settings.ts";
import { toTitleCase } from "@/utils/names";
import { unaccent } from "@/utils/sanitize.ts";

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
    (view.items || [])
      .map((item, ind) => ({
        id: ind + 1,
        ...item,
        title: item.title || item.autoTitle,
      }))
      .filter((item) => item.hidden !== true) as DataRecord[],
  );
  const showSelector = useSelector();

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
            type: "field",
          };
          if (saveWidths && schemaItem.type === "field") {
            const mainGridItem = gridState?.columns?.find(
              (c) => c.name === record.name && c.computed && c.width,
            );
            mainGridItem &&
              ((schemaItem as Field).width = `${parseInt(
                String(mainGridItem.width)!,
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
    [view, shared, state.rows, saveWidths, records],
  );

  const handleSelect = useCallback(() => {
    const extraFields = view?.items
      ?.filter(
        (item) =>
          (item.name && item.name.includes(".")) || item.type !== "field",
      )
      .map((item) => ({
        id: nextId(),
        name: item.name,
        type: "field",
        label: item.title || item.autoTitle,
      }));
    showSelector({
      model: "com.axelor.meta.db.MetaField",
      title: i18n.get("Columns"),
      multiple: true,
      view: {
        name: "custom-meta-field-grid",
        fields: {
          label: {
            name: "label",
            type: "STRING",
            required: true,
          },
          name: {
            name: "name",
            type: "STRING",
            required: true,
          },
        },
        type: "grid",
        items: [
          {
            type: "field",
            name: "label",
            title: "Title",
            sortable: false,
            searchable: false,
          },
          {
            type: "field",
            name: "name",
            title: "Name",
          },
        ],
      } as unknown as GridView,
      viewParams: {
        "_can-customize-popup": false,
      },
      domain:
        "self.metaModel.fullName = :_modelName AND self.name NOT IN :_excludedFieldNames",
      context: {
        _excludedFieldNames: ["id", "version"],
        _model: "com.axelor.meta.db.MetaField",
        _modelName: view.model,
      },
      onGridSearch: (records, page, search) => {
        let recs: DataRecord[] = [];
        forEach(records, (rec) => {
          recs.push({
            ...rec,
            label: i18n.get(rec.label || toTitleCase(rec.name ?? "")),
          });
        });
        if (page.offset === 0) {
          // add the extra fields at the end of the first page only
          let extra = extraFields;
          if (search && search.name) {
            extra = extraFields?.filter(
              (f) =>
                f.name &&
                unaccent(f.name.toLowerCase()).includes(
                  unaccent(search.name.toLowerCase()),
                ),
            );
          }
          extra?.forEach((i) => recs.push(i));
        }
        return recs;
      },
      onSelect: (selected) => {
        setRecords((records) => [
          ...records,
          ...(selected || [])
            .filter((s) => !records.find((r) => r.name === s.name))
            .map((record) => ({
              ...record,
              type: "field",
              title: record.label,
            })),
        ]);
      },
    });
  }, [showSelector, view]);

  const handleRemove = useCallback(async () => {
    const confirmed = await dialogs.confirm({
      content: i18n.get("Do you really want to delete the selected record(s)?"),
    });
    confirmed &&
      setRecords((records) =>
        records.filter((r, ind) => !selectedRows?.includes(ind)),
      );
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
              const view = getView?.(gridState);
              if (view) {
                await saveView(view);
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
      [view, stateAtom],
    ),
  );

  return canCustomize ? showCustomizeDialog : undefined;
}
