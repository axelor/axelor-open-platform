import clsx from "clsx";
import { useAtomCallback } from "jotai/utils";
import isEqual from "lodash/isEqual";
import setObjectValue from "lodash/set";
import clone from "lodash/cloneDeep";
import {
  KeyboardEvent,
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
} from "react";

import { Box, ClickAwayListener, FocusTrap } from "@axelor/ui";
import { GridRowProps } from "@axelor/ui/grid";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { alerts } from "@/components/alerts";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useTabShortcut } from "@/hooks/use-shortcut";
import { DataRecord } from "@/services/client/data.types";
import { MetaData, ViewData } from "@/services/client/meta";
import { FormView, Schema } from "@/services/client/meta.types";
import { useGetErrors, useHandleFocus } from "@/views/form";
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

import styles from "./form.module.scss";

export interface GridFormRendererProps extends GridRowProps {
  view: FormView;
  fields?: MetaData["fields"];
  onInit?: () => void;
}

type LayoutProps = Omit<WidgetProps, "widgetAtom"> &
  Pick<GridRowProps, "columns"> & {
    onSave?: () => void;
    onCancel?: () => void;
  };

const showErrors = (errors: WidgetErrors[]) => {
  const titles = Object.values(errors).flatMap((e) => Object.values(e));
  alerts.error({
    message: (
      <ul>
        {titles.map((title, i) => (
          <li key={i}>{title}</li>
        ))}
      </ul>
    ),
  });
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
  onCancel,
  columns = [],
}: LayoutProps) => {
  const items = useMemo<Schema[]>(
    () =>
      (schema.items || []).map((item) => ({
        ...item,
        editable: true,
        editIndex: columns.findIndex((c) => c.name === item.name),
        showTitle: false,
      })),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [schema.items],
  );

  return (
    <Box d="flex" alignItems="center">
      {columns.map((column, ind) => {
        const item = items.find((item) => item.name === column.name);
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
            {!item && ind === 0 && (
              <Box d="flex" justifyContent="center" alignItems="center">
                <MaterialIcon icon="undo" onClick={() => onCancel?.()} />
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
        setObjectValue($record, newKey.split("."), value);
      }
    }
  }
  return $record;
}

export const Form = forwardRef<GridFormHandler, GridFormRendererProps>(
  function Form(props, ref) {
    const {
      view,
      fields,
      className,
      columns,
      data: { record: _record },
      index: rowIndex,
      editCell: cellIndex,
      onInit,
      onSave,
      onCancel,
    } = props;
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

    const { add: addWidgetValidator } = useFormValidityScope();
    const { add: addEditableWidget } = useFormEditableScope();

    const { formAtom: parent, actionHandler: parentActionHandler } =
      useFormScope();
    const { formAtom, actionHandler, recordHandler, actionExecutor } =
      useFormHandlers(meta as unknown as ViewData<FormView>, record, {
        parent,
        states: initFormFieldsStates,
      });
    const onNewAction =
      (!record?.id || record?.id < 0) && !record?._dirty && view.onNew;

    useEffect(() => {
      actionHandler.setRefreshHandler(
        parentActionHandler.refresh.bind(parentActionHandler),
      );
    }, [actionHandler, parentActionHandler]);

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

    const handleSave = useAtomCallback(
      useCallback(
        async (get, set, saveFromEdit?: boolean, columnIndex?: number) => {
          const input = document.activeElement as HTMLInputElement;
          const elem = containerRef.current;
          if (input && elem?.contains(input)) {
            input.blur?.();
            input.focus?.();
          }

          await actionExecutor.waitFor();
          await actionExecutor.wait();

          const formState = get(formAtom);

          // check record changes
          if (isEqual(record, formState.record)) {
            return onSave?.(
              record,
              rowIndex,
              columnIndex ?? cellIndex!,
              false,
              saveFromEdit,
            );
          }

          const errors = getErrors(formState);

          if (errors) {
            showErrors(errors);
            return Promise.reject();
          }

          return await onSave?.(
            formState.record,
            rowIndex,
            columnIndex ?? cellIndex!,
            true,
            saveFromEdit,
          );
        },
        [
          actionExecutor,
          formAtom,
          record,
          getErrors,
          onSave,
          rowIndex,
          cellIndex,
        ],
      ),
    );

    const handleKeyDown = useCallback(
      function handleKeyDown(e: KeyboardEvent<HTMLDivElement>) {
        if (e.defaultPrevented && e.detail !== 1) return;
        if (e.key === `Escape`) {
          return handleCancel?.(findColumnIndexByNode(e.target as HTMLElement));
        }
        if (e.key === `Enter`) {
          return handleSave?.(
            undefined,
            findColumnIndexByNode(e.target as HTMLElement),
          );
        }
      },
      [handleSave, handleCancel],
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

    useTabShortcut({
      key: "g",
      altKey: true,
      action: useHandleFocus(containerRef),
    });

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

    const CustomLayout = useMemo(
      () => (props: LayoutProps) => (
        <FormLayoutComponent
          {...props}
          columns={columns}
          onSave={handleSave}
          onCancel={handleCancel}
        />
      ),
      [columns, handleSave, handleCancel],
    );

    useImperativeHandle(
      ref,
      () => {
        return {
          formAtom,
          invalid: checkInvalid,
          onSave: handleSave,
          onCancel: handleCancel,
        };
      },
      [checkInvalid, formAtom, handleSave, handleCancel],
    );

    useEffect(() => {
      return addWidgetValidator(checkInvalid);
    }, [addWidgetValidator, checkInvalid]);

    useEffect(() => {
      return addEditableWidget(handleRecordCommit);
    }, [addEditableWidget, handleRecordCommit]);

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
