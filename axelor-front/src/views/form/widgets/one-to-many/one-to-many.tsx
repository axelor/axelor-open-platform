import { atom, useAtom, useAtomValue } from "jotai";
import { FieldProps } from "../../builder";
import { DataStore } from "@/services/client/data-store";
import { SetStateAction, useCallback, useMemo, useRef, useState } from "react";
import { Grid as GridComponent } from "@/views/grid/builder";
import { useGridState } from "@/views/grid/builder/utils";
import { Box, CommandBar, CommandItemProps } from "@axelor/ui";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { GridView } from "@/services/client/meta.types";
import { selectAtom } from "jotai/utils";
import { useAsync } from "@/hooks/use-async";
import { findView } from "@/services/client/meta-cache";
import { DataRecord } from "@/services/client/data.types";
import { EditorOptions, useEditor, useSelector } from "@/hooks/use-relation";
import { i18n } from "@/services/client/i18n";
import classes from "./one-to-many.module.scss";
import { GridRow } from "@axelor/ui/src/grid";
import { dialogs } from "@/components/dialogs";
import { toKebabCase } from "@/utils/names";

export function OneToMany({
  schema,
  readonly,
  valueAtom,
  formAtom,
}: FieldProps<DataRecord[]>) {
  const { title, name, target: model, fields } = schema;
  // use ref to avoid onSearch call
  const shouldSearch = useRef(true);
  const [records, setRecords] = useState<DataRecord[]>([]);
  const [value, setValue] = useAtom(
    useMemo(
      () =>
        atom(
          (get) => get(valueAtom),
          (
            get,
            set,
            setter: SetStateAction<DataRecord[]>,
            callOnChange: boolean = true
          ) => {
            shouldSearch.current = false;
            const values =
              typeof setter === "function" ? setter(get(valueAtom)!) : setter;

            set(valueAtom, values, callOnChange);

            setRecords((records) =>
              [...(values || [])].map((val) => {
                const rec = val.id
                  ? records.find((r) => r.id === val.id)
                  : null;
                if (rec) return { ...rec, ...val };
                return val;
              })
            );
          }
        ),
      [valueAtom]
    )
  );
  const parentId = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record.id), [formAtom])
  );
  const isManyToMany =
    toKebabCase(schema.serverType || schema.widget) === "many-to-many";

  const { state: viewState, data: viewData } = useAsync(async () => {
    const { items, gridView } = schema;
    if ((items || []).length > 0) return;
    return findView({
      type: "grid",
      name: gridView,
      model,
    });
  });

  const showEditor = useEditor();
  const showSelector = useSelector();
  const [state, setState] = useGridState();
  const dataStore = useMemo(() => new DataStore(model), [model]);

  const clearSelection = useCallback(() => {
    setState((draft) => {
      draft.selectedRows = null;
      draft.selectedCell = null;
    });
  }, [setState]);

  const onSearch = useCallback(
    async (options?: SearchOptions) => {
      // avoid search for internal value changes
      if (!shouldSearch.current) {
        shouldSearch.current = true;
        return;
      }
      const ids = (value || []).map((x) => x.id).filter((id) => (id ?? 0) > 0);
      const unsaved = (value || []).filter(({ id }) => !ids.includes(id));

      let records: DataRecord[] = [];
      let page = dataStore.page;

      if (ids.length > 0) {
        const res = await dataStore.search({
          ...options,
          filter: {
            ...options?.filter,
            _domain: "self.id in (:_ids)",
            _domainContext: {
              id: parentId,
              _field: name,
              _model: model,
              _ids: ids as number[],
            },
          },
        });
        page = res.page;
        records = res.records;
      }

      records = [
        ...(
          ids.map((id) => records.find((r) => r.id === id)) as DataRecord[]
        ).filter((r) => r),
        ...unsaved,
      ];

      setRecords(records);

      return {
        page,
        records,
      } as SearchResult;
    },
    [value, name, model, parentId, dataStore]
  );

  const onSelect = useCallback(() => {
    showSelector({
      title: i18n.get("Select {0}", title ?? ""),
      model,
      multiple: true,
      onSelect: (records) => {
        setValue((value) => {
          const valIds = (value || []).map((x) => x.id);
          return [
            ...(value || []),
            ...records.filter((rec) => !valIds.includes(rec.id)),
          ];
        });
      },
    });
  }, [setValue, showSelector, model, title]);

  const openEditor = useCallback(
    (
      options?: Partial<EditorOptions>,
      onSelect?: (record: DataRecord) => void,
      onSave?: (record: DataRecord) => void
    ) => {
      showEditor({
        title: title ?? "",
        model,
        record: { id: null },
        readonly: false,
        ...(isManyToMany ? { onSelect } : { onSave }),
        ...options,
      });
    },
    [isManyToMany, model, title, showEditor]
  );

  const onAdd = useCallback(() => {
    openEditor(
      {},
      (record) => setValue((value) => [...(value || []), { ...record }]),
      (record) =>
        setValue((value) => [...(value || []), { ...record, _dirty: true }])
    );
  }, [openEditor, setValue]);

  const onEdit = useCallback(
    (record: DataRecord, readonly = false) => {
      const matcher = (rec: DataRecord) =>
        (rec.id && rec.id === record.id) || rec === record;

      openEditor(
        { record, readonly },
        (record) =>
          setValue((value) =>
            value?.map((val) => (matcher(val) ? { ...val, ...record } : val))
          ),
        (record) =>
          setValue((value) =>
            value?.map((val) =>
              matcher(val) ? { ...val, ...record, _dirty: true } : val
            )
          )
      );
    },
    [setValue, openEditor]
  );

  const onView = useCallback(
    (record: DataRecord) => {
      onEdit(record, true);
    },
    [onEdit]
  );

  const onDelete = useCallback(
    async (records: GridRow["record"][]) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: i18n.get(
          "Do you really want to delete the selected record(s)?"
        ),
      });
      if (confirmed) {
        const ids = records.map((r) => r.id);
        setValue((value) =>
          (value || []).filter(({ id }) => !ids.includes(id))
        );
        clearSelection();
      }
    },
    [setValue, clearSelection]
  );

  if (viewState === "loading") return null;
  const { selectedRows, rows } = state;
  const hasRowSelected = !!selectedRows?.length;

  return (
    <Box d="flex" flexDirection="column" className={classes.container}>
      <Box shadow>
        {!readonly && (
          <CommandBar
            iconProps={{
              weight: 300,
            }}
            iconOnly
            items={[
              ...(isManyToMany
                ? [
                    {
                      key: "select",
                      text: i18n.get("Select"),
                      iconProps: {
                        icon: "search",
                      },
                      onClick: onSelect,
                    } as CommandItemProps,
                  ]
                : []),
              {
                key: "new",
                text: i18n.get("New"),
                iconProps: {
                  icon: "add",
                },
                onClick: onAdd,
              },
              {
                key: "edit",
                text: i18n.get("Edit"),
                iconProps: {
                  icon: "edit",
                },
                disabled: !hasRowSelected,
                onClick: () => {
                  const [rowIndex] = selectedRows || [];
                  const record = rows[rowIndex]?.record;
                  record && onEdit(record);
                },
              },
              {
                key: "delete",
                text: i18n.get("Delete"),
                iconProps: {
                  icon: "delete",
                },
                disabled: !hasRowSelected,
                onClick: () => {
                  onDelete(selectedRows!.map((ind) => rows[ind]?.record));
                },
              },
              {
                key: "refresh",
                text: i18n.get("Refresh"),
                iconProps: {
                  icon: "refresh",
                },
                onClick: () => onSearch(),
              },
            ]}
          />
        )}
      </Box>
      <GridComponent
        showEditIcon={!readonly}
        records={records}
        view={(viewData?.view || schema) as GridView}
        fields={viewData?.fields || fields}
        state={state}
        setState={setState}
        onEdit={onEdit}
        onView={onView}
        onSearch={onSearch}
      />
    </Box>
  );
}
