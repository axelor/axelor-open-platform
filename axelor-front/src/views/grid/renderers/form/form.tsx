import { useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import clone from "lodash/cloneDeep";
import isEqual from "lodash/isEqual";
import setObjectValue from "lodash/set";
import uniqueId from "lodash/uniqueId";
import {
  KeyboardEvent,
  SyntheticEvent,
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
} from "react";

import { clsx, Box, ClickAwayListener, FocusTrap } from "@axelor/ui";
import { GridColumn, GridRowProps } from "@axelor/ui/grid";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useTabShortcut } from "@/hooks/use-shortcut";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { MetaData, ViewData } from "@/services/client/meta";
import { FormView, GridView, Schema } from "@/services/client/meta.types";
import { useViewDirtyAtom } from "@/view-containers/views/scope";
import { showErrors, useGetErrors, useHandleFocus } from "@/views/form";
import {
  FormAtom,
  Form as FormComponent,
  FormLayout,
  FormProps,
  FormWidget,
  WidgetErrors,
  WidgetProps,
  useFormHandlers,
} from "@/views/form/builder";
import {
  useFormEditableScope,
  useFormScope,
  useFormValidityScope,
} from "@/views/form/builder/scope";
import {
  useCollectionTree,
  useCollectionTreeEditable,
} from "../../builder/scope";
import { ExpandIcon } from "../../builder/expandable";
import { AUTO_ADD_ROW } from "../../builder/utils";
import { fallbackFormAtom } from "@/views/form/builder/atoms";
import { executeWithoutQueue } from "@/view-containers/action";

import styles from "./form.module.scss";

export interface GridFormRendererProps extends GridRowProps {
  view: GridView;
  viewContext?: DataContext;
  fields?: MetaData["fields"];
  isLastRow?: boolean;
  onAddSubLine?: (parent: DataRecord) => void;
  onInit?: () => void;
}

type LayoutProps = Omit<WidgetProps, "widgetAtom"> &
  Pick<GridRowProps, "columns"> & {
    onSave?: () => void;
    onCancel?: () => void;
  };

const findColumnIndexByNode = (ele: HTMLElement) => {
  function getParent(ele: HTMLElement): string {
    if (!ele) return "";
    ele = ele?.offsetParent as HTMLElement;
    return ele?.dataset?.columnIndex || getParent(ele);
  }
  const colIndex = getParent(ele as HTMLElement);
  return colIndex ? +colIndex : undefined;
};

export type GridFormHandler = {
  formAtom?: FormAtom;
  invalid?: () => null | WidgetErrors[];
  onSave?: (saveFromEdit?: boolean) => void;
  onCancel?: () => void;
};

export const FormLayoutComponent = ({
  schema,
  formAtom,
  readonly,
  hasSaved,
  expandState,
  onExpand,
  onCancel,
  onCellClick,
  columns = [],
}: LayoutProps & {
  hasSaved?: boolean;
  expandState: {
    expand: boolean;
    disable?: boolean;
    children?: boolean;
  };
  onCellClick?: (
    e: React.SyntheticEvent,
    col: GridColumn,
    colIndex: number,
  ) => void;
  onExpand?: () => void;
}) => {
  const items = useMemo<Schema[]>(
    () =>
      (schema.items || []).map((item) => ({
        ...(item.jsonField
          ? {
              uid: uniqueId("w"),
              type: "field",
              name: item.jsonField,
              title: item.title || item.name,
              editor: {
                items: [
                  {
                    ...item,
                    inGridEditor: true,
                    colSpan: 12,
                    name: item.jsonPath ?? item.name,
                  },
                ],
                widgetAttrs: {
                  showTitles: "false",
                },
              },
              jsonFields: {
                [item.jsonPath!]: {
                  ...item,
                  type: item.serverType ?? item.type,
                },
              },
              json: true,
              cols: 12,
              inGridEditorColName: item.name,
            }
          : item),
        inGridEditor: true,
        inGridEditorIndex: columns.findIndex((c) => c.name === item.name),
        showTitle: false,
      })),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [schema.items],
  );

  const expandColumn = columns.find((col) => col.type === "row-expand");
  const undoColumn = columns.find(
    (col) =>
      col.type === "row-checked" || (col as Schema).widget === "edit-icon",
  );
  return (
    <Box d="flex" alignItems="center">
      {columns.map((column, ind) => {
        const item = items.find(
          (item) => (item.inGridEditorColName ?? item.name) === column.name,
        );
        return (
          <Box
            key={item?.uid ?? `column_${ind}`}
            className={styles.column}
            data-column-index={ind}
            {...(column.width && {
              style: {
                width: column.width,
                minWidth: column.width,
              },
            })}
          >
            {!item && column === expandColumn && (
              <Box d="flex" onClick={() => onExpand?.()}>
                <ExpandIcon {...expandState} />
              </Box>
            )}
            {!item && column === undoColumn && (
              <Box
                d="flex"
                justifyContent="center"
                alignItems="center"
                onClick={() => onCancel?.()}
              >
                <MaterialIcon icon="undo" />
              </Box>
            )}
            {!item && (column as Schema).widget === "new-icon" && (
              <Box
                d="flex"
                justifyContent="center"
                alignItems="center"
                onClick={(e) => onCellClick?.(e, column, ind)}
              >
                <MaterialIcon icon="add" />
              </Box>
            )}
            {!item && (column as Schema).widget === "delete-icon" && (
              <Box
                d="flex"
                justifyContent="center"
                alignItems="center"
                onClick={async (e) => {
                  onCancel?.();
                  hasSaved && onCellClick?.(e, column, ind);
                }}
              >
                <MaterialIcon icon="delete" />
              </Box>
            )}
            {item && (
              <FormWidget
                schema={item}
                formAtom={formAtom}
                readonly={readonly}
              />
            )}
          </Box>
        );
      })}
    </Box>
  );
};

function isFocusableField(item: Schema) {
  if (
    item.readonly ||
    item.hidden ||
    ["button"].includes(item.type ?? "") ||
    ["BINARY"].includes(item.serverType ?? "") ||
    ["image", "binary", "binary-link", "image-link"].includes(item.widget ?? "")
  ) {
    return false;
  }
  return true;
}

function processGridRecord(record: DataRecord) {
  const $record: DataRecord = clone(record);
  for (const [key, value] of Object.entries(record)) {
    if (key.includes(".")) {
      const hasTranslation = key.startsWith("$t:");
      const [name, ...subFieldNames] = key.split(".");
      const fieldName = hasTranslation ? name.slice("$t:".length) : name;
      if (
        Object.prototype.hasOwnProperty.call(record, fieldName) &&
        record[fieldName]
      ) {
        const lastName = subFieldNames.pop();
        const newKey = hasTranslation
          ? [fieldName, ...subFieldNames, `$t:${lastName}`].join(".")
          : key;
        delete $record[key];
        setObjectValue($record, newKey.split("."), clone(value));
      }
    }
  }
  return $record;
}

export const Form = forwardRef<GridFormHandler, GridFormRendererProps>(
  function Form(props, ref) {
    const {
      view,
      viewContext,
      fields,
      className,
      columns,
      hasExpanded,
      data: gridRow,
      index: rowIndex,
      editCell: cellIndex,
      isLastRow,
      onCellClick,
      onExpand,
      onInit,
      onAddSubLine,
      onSave,
      onCancel,
    } = props;
    const { record: _record, expand } = gridRow;
    const record = useMemo(() => processGridRecord(_record), [_record]);
    const containerRef = useRef<HTMLDivElement>(null);
    const recordRef = useRef<DataRecord>({});
    const parentRef = useRef<Element>();
    const meta = useMemo(
      () => ({
        view,
        fields,
        model: (view as Schema).target || view?.model,
      }),
      [view, fields],
    );
    const { newItem: newItemAtom } = useCollectionTree();

    const editColumnName = columns?.[cellIndex ?? -1]?.name;
    const initFormFieldsStates = useMemo(() => {
      const defaultColumnName = view.items?.find(isFocusableField)?.name;
      const editColumn = view.items?.find((c) => c.name === editColumnName);
      const name = editColumn?.readonly
        ? defaultColumnName
        : editColumnName || defaultColumnName;
      const item = view.items?.find((item) => item.name === name);
      if (item) {
        return {
          [item.name as string]: {
            attrs: {
              focus: true,
            },
          },
        };
      }
    }, [editColumnName, view.items]);

    const expandState = useMemo(
      () =>
        hasExpanded
          ? hasExpanded(gridRow)
          : { expand: Boolean(gridRow.expand) },
      [gridRow, hasExpanded],
    );

    const { add: addWidgetValidator } = useFormValidityScope();
    const { add: addEditableWidget } = useFormEditableScope();
    const { enabled: isCollectionTree } = useCollectionTree();
    const { setCommit } = useCollectionTreeEditable();
    const isTreeGrid =
      isCollectionTree && (view as Schema).widget === "tree-grid";
    const isManyToMany = (view as Schema).serverType === "MANY_TO_MANY";
    const autoAddNewRow = (view as any)[AUTO_ADD_ROW] ?? true;

    const { formAtom: parent, actionHandler: parentActionHandler } =
      useFormScope();
    const { formAtom, actionHandler, recordHandler, actionExecutor } =
      useFormHandlers(meta as unknown as ViewData<FormView>, record, {
        ...(parent !== fallbackFormAtom && {
          parent,
        }),
        context: viewContext,
        states: initFormFieldsStates,
      });

    const isNew = !record?.id || record?.id < 0;
    const onSaveAction =
      (view as Schema).serverType !== "ONE_TO_MANY" && view.onSave;
    const onNewAction = isNew && !record?._dirty && view.onNew;

    const formDirty = useAtomValue(
      useMemo(() => selectAtom(formAtom, (x) => x.dirty), [formAtom]),
    );
    const dirtyAtom = useViewDirtyAtom();
    const setDirty = useSetAtom(dirtyAtom);
    const hasSaved = _record._dirty || _record.id > 0;

    useEffect(() => {
      !isManyToMany && formDirty !== undefined && setDirty(formDirty);
    }, [isManyToMany, formDirty, setDirty]);

    const getParent = useCallback(() => {
      if (parentRef.current) return parentRef.current;
      const container = containerRef.current;
      const body = document.body;
      for (let i = 0; i < body.children.length; i++) {
        const element = body.children[i];
        if (element?.contains(container)) {
          return (parentRef.current = element);
        }
      }
    }, []);

    const getErrors = useGetErrors();

    const checkInvalid = useAtomCallback(
      useCallback(
        (get, set, name?: string) => getErrors(get(formAtom), name),
        [formAtom, getErrors],
      ),
    );

    const handleCancel = useCallback(
      (columnIndex?: number) =>
        onCancel?.(record, rowIndex, columnIndex ?? cellIndex!),
      [onCancel, record, rowIndex, cellIndex],
    );

    const isRecordSavable = useAtomCallback(
      useCallback(
        (get) => {
          const formState = get(formAtom);
          return !isEqual(record, formState.record) || !hasSaved;
        },
        [formAtom, record, hasSaved],
      ),
    );

    const handleSave = useAtomCallback(
      useCallback(
        async (
          get,
          set,
          saveFromEdit?: boolean,
          columnIndex?: number,
          saveFromAction?: boolean,
        ) => {
          const input = document.activeElement as HTMLInputElement;
          const elem = containerRef.current;
          if (input && elem?.contains(input)) {
            input.blur?.();
            input.focus?.();
          }

          await actionExecutor.waitFor();

          if (!saveFromAction) {
            await actionExecutor.wait();
          }

          if (onSaveAction) {
            await actionExecutor.execute(onSaveAction);
          }

          const formState = get(formAtom);

          // check record changes
          // if saved record is same then discard the editing
          if (!isRecordSavable()) {
            return onSave?.(
              record,
              rowIndex,
              columnIndex ?? cellIndex!,
              false,
              saveFromEdit || expand,
            );
          }

          const errors = getErrors(formState);

          if (errors) {
            showErrors(errors);
            return Promise.reject();
          }

          return await onSave?.(
            {
              ...formState.record,
              _dirty: formState.dirty,
              ...(isTreeGrid && {
                ...(isLastRow &&
                  !saveFromEdit && {
                    selected: false,
                  }),
                _changed: true,
                _original: formState.original,
              }),
            },
            rowIndex,
            columnIndex ?? cellIndex!,
            true,
            saveFromEdit,
          );
        },
        [
          expand,
          isLastRow,
          actionExecutor,
          formAtom,
          record,
          getErrors,
          onSaveAction,
          onSave,
          rowIndex,
          cellIndex,
          isTreeGrid,
          isRecordSavable,
        ],
      ),
    );

    const handleRecordCommit = useAtomCallback(
      useCallback(
        (get, set) => {
          const { record, original } = get(formAtom);

          if (isEqual(record, recordRef.current)) {
            return;
          }

          recordRef.current = record;

          // check if not changed then discard it.
          if (isEqual(record, original)) {
            return handleCancel();
          }
          return handleSave(true);
        },
        [formAtom, handleSave, handleCancel],
      ),
    );

    useEffect(() => {
      actionHandler.setRefreshHandler(
        parentActionHandler.refresh.bind(parentActionHandler),
      );
      actionHandler.setSaveHandler(async () => {
        await executeWithoutQueue(() => handleSave(true, undefined, true));
        return parentActionHandler.save.bind(parentActionHandler)();
      });
    }, [actionHandler, parentActionHandler, handleSave]);

    useTabShortcut({
      key: "g",
      altKey: true,
      action: useHandleFocus(containerRef),
    });

    const handleExpand = useCallback(async () => {
      await handleRecordCommit();
      onExpand?.(gridRow, !gridRow.expand);
    }, [gridRow, onExpand, handleRecordCommit]);

    const handleKeyDown = useAtomCallback(
      useCallback(
        function handleKeyDown(get, set, e: KeyboardEvent<HTMLDivElement>) {
          if (e.defaultPrevented && e.detail !== 1) return;
          if (e.key === `Escape`) {
            return handleCancel?.(
              findColumnIndexByNode(e.target as HTMLElement),
            );
          }
          if (e.key === `Enter`) {
            const shouldAddSubLine = e.ctrlKey && onAddSubLine;
            const savable = isRecordSavable();
            return handleSave?.(
              (!autoAddNewRow && savable) || expand || shouldAddSubLine
                ? true
                : undefined,
              findColumnIndexByNode(e.target as HTMLElement),
            ).then(async () => {
              if (shouldAddSubLine) {
                if (!isNew || autoAddNewRow) {
                  !expand && (await handleExpand());
                  onAddSubLine(record);
                }
              } else if (!autoAddNewRow && isLastRow && savable) {
                set(newItemAtom, { refId: record.id });
              }
            });
          }
        },
        [
          isNew,
          expand,
          record,
          autoAddNewRow,
          newItemAtom,
          isLastRow,
          isRecordSavable,
          onAddSubLine,
          handleExpand,
          handleSave,
          handleCancel,
        ],
      ),
    );

    const handleClickOutside = useCallback(
      (e: Event) => {
        if (e.defaultPrevented) return;
        const parent = getParent();

        if (parent && !parent?.contains?.(e.target as Node)) {
          return;
        }

        return handleRecordCommit();
      },
      [getParent, handleRecordCommit],
    );

    const handleCellClick = useCallback(
      (e: SyntheticEvent, col: GridColumn, colIndex: number) => {
        const shouldCommitAndAdd = !autoAddNewRow && (record.id ?? 0) <= 0;
        if (shouldCommitAndAdd && (col as Schema).widget === "new-icon") {
          // handle same way as saved with enter key press
          handleKeyDown({
            key: "Enter",
          } as KeyboardEvent<HTMLDivElement>);
        } else {
          onCellClick?.(e, col, colIndex, gridRow, rowIndex);
        }
      },
      [onCellClick, handleKeyDown, gridRow, rowIndex, autoAddNewRow, record.id],
    );

    const CustomLayout = useMemo(
      () => (props: LayoutProps) => (
        <FormLayoutComponent
          {...props}
          hasSaved={hasSaved}
          expandState={expandState}
          onCellClick={handleCellClick}
          onExpand={handleExpand}
          onSave={handleSave}
          onCancel={handleCancel}
        />
      ),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      [expandState, handleSave, handleCancel],
    );

    useImperativeHandle(ref, () => {
      return {
        formAtom,
        invalid: checkInvalid,
        onSave: handleSave,
        onCancel: handleCancel,
      };
    }, [checkInvalid, formAtom, handleSave, handleCancel]);

    useEffect(() => {
      return addWidgetValidator(checkInvalid);
    }, [addWidgetValidator, checkInvalid]);

    useEffect(() => {
      return addEditableWidget(handleRecordCommit);
    }, [addEditableWidget, handleRecordCommit]);

    useAsyncEffect(async () => {
      setCommit?.(handleRecordCommit);
      return () => {
        setCommit?.(null);
      };
    }, [handleRecordCommit, setCommit]);

    useAsyncEffect(async () => {
      if (onNewAction) {
        await actionExecutor.execute(onNewAction);
      }
    }, [onNewAction, actionExecutor]);

    useEffect(() => {
      onInit?.();
    }, [onInit]);

    return (
      <>
        {!(view as Schema).serverType && (
          <MainShortcuts handleSave={handleSave} />
        )}

        <FocusTrap initialFocus={false}>
          <Box
            ref={containerRef}
            className={clsx(className, styles.container)}
            d="flex"
            onKeyDown={handleKeyDown}
          >
            <ClickAwayListener onClickAway={handleClickOutside}>
              <Box d="flex">
                <FormComponent
                  {...({} as FormProps)}
                  schema={view}
                  fields={fields!}
                  layout={CustomLayout as unknown as FormLayout}
                  layoutProps={{ columns }}
                  readonly={false}
                  formAtom={formAtom}
                  actionHandler={actionHandler}
                  actionExecutor={actionExecutor}
                  recordHandler={recordHandler}
                />
              </Box>
            </ClickAwayListener>
          </Box>
        </FocusTrap>
      </>
    );
  },
);

function MainShortcuts({
  handleSave,
}: {
  handleSave?: (
    saveFromEdit?: boolean,
    columnIndex?: number,
  ) => Promise<unknown>;
}) {
  useTabShortcut({
    key: "s",
    ctrlKey: true,
    action: useCallback(() => {
      void (async () => {
        await handleSave?.();
      })();
    }, [handleSave]),
  });

  return null;
}
