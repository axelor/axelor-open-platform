import {
  Box,
  ClickAwayListener,
  FocusTrap,
  Popper,
  TextField,
} from "@axelor/ui";
import { atom, useAtom } from "jotai";
import { SetStateAction, useCallback, useMemo, useRef, useState } from "react";
import { MaterialIconProps } from "@axelor/ui/icons/meterial-icon";

import { Grid as GridComponent } from "@/views/grid/builder";
import { DataRecord } from "@/services/client/data.types";
import { FieldProps } from "../../builder";
import { dialogs } from "@/components/dialogs";
import { useEditor, useSelector } from "@/hooks/use-relation";
import { useAsync } from "@/hooks/use-async";
import { useGridState } from "@/views/grid/builder/utils";
import { findView } from "@/services/client/meta-cache";
import { i18n } from "@/services/client/i18n";
import { GridView } from "@/services/client/meta.types";
import styles from "./one-to-many.edit.module.scss";
import { toKebabCase } from "@/utils/names";

export function OneToManyEdit({
  schema,
  formAtom,
  valueAtom,
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
            callOnChange: boolean = true
          ) => {
            const values =
              typeof setter === "function"
                ? setter(get(valueAtom) ?? [])
                : setter;
            set(valueAtom, values, callOnChange);
          }
        ),
      [valueAtom]
    )
  );

  const showSelector = useSelector();
  const showEditor = useEditor();

  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const popupRef = useRef(false);

  const isManyToMany =
    toKebabCase(schema.serverType || schema.widget) === "many-to-many";
  const { rows, selectedRows } = state;
  const { title, target: model, formView, gridView } = schema;

  const { data: meta } = useAsync(async () => {
    return await findView<GridView>({
      type: "grid",
      name: gridView,
      model,
    });
  }, []);

  const focusInput = useCallback(() => {
    const input = inputRef.current;
    input && input.focus?.();
  }, []);

  const onPopupViewInit = useCallback(() => {
    popupRef.current = true;
    return () => {
      popupRef.current = false;
    };
  }, []);

  const onAdd = useCallback(() => {
    const onClose = onPopupViewInit();
    showSelector({
      title: i18n.get("Select {0}", title ?? ""),
      model,
      multiple: true,
      viewName: gridView,
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
    });
  }, [showSelector, title, model, gridView, setValue, onPopupViewInit]);

  const onEdit = useCallback(
    (record: DataRecord) => {
      const onClose = onPopupViewInit();
      const save = (record: DataRecord) => {
        if (record.id) {
          setValue((value) =>
            value?.map((val) =>
              val.id === record.id ? { ...val, ...record } : val
            )
          );
        }
      };
      showEditor({
        title: title ?? "",
        model,
        record,
        readonly: false,
        viewName: formView,
        onClose,
        ...(isManyToMany ? { onSelect: save } : { onSave: save }),
      });
    },
    [
      title,
      formView,
      isManyToMany,
      model,
      showEditor,
      setValue,
      onPopupViewInit,
    ]
  );

  const showPopup = useCallback(
    (popup: boolean) => {
      popup && focusInput();
      setPopup(popup);
    },
    [focusInput]
  );

  const onDelete = useCallback(async () => {
    const records = selectedRows!.map((ind) => rows[ind]?.record);
    const confirmed = await dialogs.confirm({
      content: i18n.get("Do you really want to delete the selected record(s)?"),
    });
    if (confirmed) {
      const ids = records.map((r) => r.id);
      setValue((value) => (value || []).filter(({ id }) => !ids.includes(id)));
      focusInput();
    }
  }, [rows, selectedRows, focusInput, setValue]);

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
        value={value?.length ? `(${value.length})` : ""}
        onChange={() => {}}
        icons={[
          {
            icon: "add",
            onClick: onAdd,
          },
          ...(selectedRows?.length
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
                      records={value!}
                      view={meta.view}
                      fields={meta.fields}
                      state={state}
                      setState={setState}
                      onEdit={onEdit}
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
