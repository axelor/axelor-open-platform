import {
  Box,
  ClickAwayListener,
  FocusTrap,
  Popper,
  TextField,
} from "@axelor/ui";
import { MaterialIconProps } from "@axelor/ui/icons/material-icon";
import { atom, useAtom, useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { SetStateAction, useCallback, useMemo, useRef, useState } from "react";

import { dialogs } from "@/components/dialogs";
import { useAsync } from "@/hooks/use-async";
import { useEditor, useSelector } from "@/hooks/use-relation";
import { SearchOptions } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ViewData } from "@/services/client/meta";
import { findFields, findView } from "@/services/client/meta-cache";
import { GridView, View } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import { Grid as GridComponent } from "@/views/grid/builder";
import { useGridState } from "@/views/grid/builder/utils";
import { FieldProps, usePermission, usePrepareContext } from "../../builder";
import { nextId } from "../../builder/utils";
import styles from "./one-to-many.edit.module.scss";

export function OneToManyEdit({
  schema,
  formAtom,
  widgetAtom,
  valueAtom,
  ...props
}: FieldProps<DataRecord[] | undefined>) {
  const [popup, setPopup] = useState(false);
  const [state, setState] = useGridState();
  const [value, setValue] = useAtom(
    useMemo(
      () =>
        atom(
          (get) => get(valueAtom) ?? [],
          (
            get,
            set,
            setter: SetStateAction<DataRecord[]>,
            callOnChange: boolean = true,
          ) => {
            const values =
              typeof setter === "function"
                ? setter(get(valueAtom) ?? [])
                : setter;
            set(valueAtom, values, callOnChange);

            setRecords((records) =>
              [...(values || [])].map((val) => {
                const rec = val.id
                  ? records.find((r) => r.id === val.id)
                  : null;
                if (rec) return { ...rec, ...val };
                return val;
              }),
            );
          },
        ),
      [valueAtom],
    ),
  );

  const showSelector = useSelector();
  const showEditor = useEditor();
  const { hasButton } = usePermission(schema, widgetAtom);

  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const popupRef = useRef(false);
  const [records, setRecords] = useState<DataRecord[]>([]);

  const isManyToMany =
    toKebabCase(schema.serverType || schema.widget) === "many-to-many";
  const { rows, selectedRows } = state;
  const {
    title,
    name,
    target: model,
    formView,
    gridView,
    views,
    orderBy: sortBy,
    searchLimit,
  } = schema;
  const { attrs } = useAtomValue(widgetAtom);

  const parentId = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.record.id), [formAtom]),
  );
  const getContext = usePrepareContext(formAtom);
  const dataStore = useMemo(() => new DataStore(model), [model]);

  const { focus, domain } = attrs;
  const readonly = props.readonly || attrs.readonly;
  const canNew = !readonly && hasButton("new");
  const canEdit = !readonly && hasButton("edit");
  const canDelete = !readonly && hasButton("remove");
  const canSelect = !readonly && hasButton("select");

  const { data: meta } = useAsync(async () => {
    const view = views?.find?.((v: View) => v.type === "grid");
    if (view && view.items) {
      const { fields } = await findFields(model);
      return {
        view,
        fields,
      } as unknown as ViewData<GridView>;
    }
    return await findView<GridView>({
      type: "grid",
      name: gridView,
      model,
    });
  }, [views, gridView, model]);

  const onSearch = useCallback(
    async (options?: SearchOptions) => {
      const ids = (value || []).map((x) => x.id).filter((id) => (id ?? 0) > 0);
      const unsaved = (value || []).filter((x) => !ids.includes(x.id));

      if (ids.length > 0) {
        const { records } = await dataStore.search({
          ...options,
          limit: -1,
          offset: 0,
          sortBy: sortBy?.split?.(","),
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
        setRecords(
          (ids.map((id) => records.find((r) => r.id === id)) as DataRecord[])
            .filter((r) => r)
            .concat(unsaved),
        );
      }
    },
    [value, sortBy, name, model, parentId, dataStore],
  );

  const focusInput = useCallback(() => {
    const input = inputRef.current;
    input && input.focus?.();
  }, []);

  const onPopupViewInit = useCallback(() => {
    popupRef.current = true;
    return () => {
      setTimeout(() => {
        popupRef.current = false;
      });
    };
  }, []);

  const onEdit = useCallback(
    async (record: DataRecord, readonly: boolean = false) => {
      const onClose = onPopupViewInit();
      const save = (record: DataRecord) => {
        if (!record.id) {
          record = { ...record, _dirty: true, id: nextId() };
        }
        setValue((value) => {
          if (value?.some((v) => v.id === record.id)) {
            return value?.map((val) =>
              val.id === record.id ? { ...val, ...record } : val,
            );
          }
          return [...(value || []), record];
        });
      };
      let form = views?.find?.((v: View) => v.type === "form");
      if (form) {
        const { fields } = await findFields(model);
        form = { ...form, fields };
      }
      showEditor({
        title: title ?? "",
        model,
        record,
        readonly,
        viewName: formView,
        ...(form && {
          view: { ...form },
        }),
        ...(isManyToMany ? { onSelect: save } : { onSave: save }),
        onClose,
      });
    },
    [
      title,
      model,
      views,
      formView,
      setValue,
      showEditor,
      isManyToMany,
      onPopupViewInit,
    ],
  );

  const onView = useCallback(
    (record: DataRecord) => onEdit(record, true),
    [onEdit],
  );

  const onAdd = useCallback(() => {
    if (!isManyToMany) {
      return onEdit({});
    }
    const onClose = onPopupViewInit();
    showSelector({
      model,
      multiple: true,
      viewName: gridView,
      domain: domain,
      orderBy: sortBy,
      context: getContext(),
      limit: searchLimit,
      onClose,
      onSelect: (records) => {
        setValue((value) => {
          const valIds = (value || []).map((x) => x.id);
          return [
            ...(value || []),
            ...records.filter((rec) => !valIds.includes(rec.id)),
          ];
        });
      },
      ...(canNew && {
        onCreate: () => {
          setTimeout(() => {
            onEdit({});
          });
        },
      }),
    });
  }, [
    onPopupViewInit,
    showSelector,
    sortBy,
    model,
    gridView,
    domain,
    searchLimit,
    canNew,
    isManyToMany,
    setValue,
    getContext,
    onEdit,
  ]);

  const showPopup = useCallback(
    async (popup: boolean) => {
      popup && focusInput();
      popup && (await onSearch({}));
      setPopup(popup);
    },
    [focusInput, onSearch],
  );

  const onDelete = useCallback(async () => {
    const onClose = onPopupViewInit();
    const records = selectedRows!.map((ind) => rows[ind]?.record);
    const confirmed = await dialogs.confirm({
      content: i18n.get("Do you really want to delete the selected record(s)?"),
    });
    if (confirmed) {
      const ids = records.map((r) => r.id);
      setValue((value) => (value || []).filter(({ id }) => !ids.includes(id)));
      focusInput();
      onClose();
    }
  }, [rows, selectedRows, focusInput, setValue, onPopupViewInit]);

  const handleClickAway = useCallback((e: Event) => {
    const container = containerRef.current;
    if (
      popupRef.current ||
      (e.target && container?.contains(e.target as Node))
    ) {
      return;
    }
    setPopup(false);
  }, []);

  return (
    <Box ref={containerRef} d="flex">
      <TextField
        ref={inputRef}
        readOnly
        autoFocus={focus}
        value={`(${value?.length || 0})`}
        onChange={() => {}}
        icons={[
          ...(!readonly && (isManyToMany ? canSelect : canNew)
            ? [
                {
                  icon: "add",
                  onClick: onAdd,
                } as MaterialIconProps,
              ]
            : []),
          ...(popup && selectedRows?.length && canDelete
            ? [
                {
                  icon: "remove",
                  onClick: onDelete,
                } as MaterialIconProps,
              ]
            : []),
          {
            icon: "arrow_drop_down",
            onClick: () => showPopup(!popup),
          },
        ]}
      />

      <Popper open={popup} target={inputRef.current} placement="bottom-start">
        <Box d="flex">
          <ClickAwayListener onClickAway={handleClickAway}>
            <Box d="flex">
              <FocusTrap enabled={popup}>
                <Box d="flex" flex={1} className={styles.grid}>
                  {popup && meta && (
                    <GridComponent
                      showEditIcon
                      readonly={readonly || !canEdit}
                      records={records}
                      view={meta.view}
                      fields={meta.fields}
                      state={state}
                      setState={setState}
                      onView={onView}
                      onEdit={canEdit ? onEdit : onView}
                    />
                  )}
                </Box>
              </FocusTrap>
            </Box>
          </ClickAwayListener>
        </Box>
      </Popper>
    </Box>
  );
}
