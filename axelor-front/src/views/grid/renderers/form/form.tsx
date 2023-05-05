import { Box, ClickAwayListener, FocusTrap } from "@axelor/ui";
import { GridRowProps } from "@axelor/ui/grid";
import {
  KeyboardEvent,
  forwardRef,
  useCallback,
  useImperativeHandle,
  useMemo,
  useRef,
} from "react";
import clsx from "clsx";
import { useAtomCallback } from "jotai/utils";
import { isEqual } from "lodash";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { MetaData, ViewData } from "@/services/client/meta";
import { FormView, GridView, Schema } from "@/services/client/meta.types";
import {
  Form as FormComponent,
  FormLayout,
  FormProps,
  FormWidget,
  WidgetErrors,
  WidgetProps,
  useFormHandlers,
} from "@/views/form/builder";
import { alerts } from "@/components/alerts";
import { DataRecord } from "@/services/client/data.types";
import { checkErrors } from "@/views/form/builder/utils";
import styles from "./form.module.scss";

export interface GridFormRendererProps extends GridRowProps {
  view: GridView;
  fields?: MetaData["fields"];
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
    [schema.items]
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

export const Form = forwardRef<GridFormHandler, GridFormRendererProps>(
  function Form(props, ref) {
    const {
      view,
      fields,
      className,
      columns,
      data: { record },
      index: rowIndex,
      editCell: cellIndex,
      onSave,
      onCancel,
    } = props;
    const containerRef = useRef<HTMLDivElement>(null);
    const recordRef = useRef<DataRecord>({});
    const parentRef = useRef<Element>();
    const meta = {
      view,
      fields,
      model: (view as Schema).target || view?.model,
    };
    const editColumnName = columns?.[cellIndex ?? -1]?.name;
    const initFormFieldsStates = useMemo(() => {
      const name =
        editColumnName || view.items?.find((item) => !item.readonly)?.name;
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

    const { formAtom, actionHandler, recordHandler, actionExecutor } =
      useFormHandlers(
        meta as unknown as ViewData<FormView>,
        record,
        undefined,
        initFormFieldsStates
      );

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

    const getErrors = useAtomCallback(
      useCallback(
        (get) => {
          const { states } = get(formAtom);
          return checkErrors(states);
        },
        [formAtom]
      )
    );

    const handleCancel = useCallback(
      (columnIndex?: number) =>
        onCancel?.(record, rowIndex, columnIndex ?? cellIndex!),
      [onCancel, record, rowIndex, cellIndex]
    );

    const handleSave = useAtomCallback(
      useCallback(
        async (get, set, saveFromEdit?: boolean, columnIndex?: number) => {
          const { record: saveRecord } = get(formAtom);

          // check record changes
          if (isEqual(record, saveRecord)) {
            return onSave?.(
              record,
              rowIndex,
              columnIndex ?? cellIndex!,
              false,
              saveFromEdit
            );
          }

          const errors = getErrors();

          if (errors) {
            showErrors(errors);
            return Promise.reject();
          }

          const input = document.activeElement as HTMLInputElement;
          const elem = containerRef.current;
          if (input && elem?.contains(input)) {
            input.blur?.();
            input.focus?.();
          }

          await actionExecutor.wait();

          return await onSave?.(
            saveRecord,
            rowIndex,
            columnIndex ?? cellIndex!,
            true,
            saveFromEdit
          );
        },
        [
          formAtom,
          record,
          rowIndex,
          cellIndex,
          getErrors,
          onSave,
          actionExecutor,
        ]
      )
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
            findColumnIndexByNode(e.target as HTMLElement)
          );
        }
      },
      [handleSave, handleCancel]
    );

    const handleClickOutside = useAtomCallback(
      useCallback(
        (get, set, e: Event) => {
          if (e.defaultPrevented) return;
          const parent = getParent();

          if (parent && !parent?.contains?.(e.target as Node)) {
            return;
          }

          const { record } = get(formAtom);

          if (isEqual(record, recordRef.current)) {
            return;
          }

          recordRef.current = record;

          // except id, other fields is exist
          if (Object.keys(record).length > 1) {
            handleSave(true);
          } else {
            handleCancel();
          }
        },
        [getParent, formAtom, handleSave, handleCancel]
      )
    );

    const CustomLayout = useMemo(
      () => (props: LayoutProps) =>
        (
          <FormLayoutComponent
            {...props}
            columns={columns}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        ),
      [columns, handleSave, handleCancel]
    );

    useImperativeHandle(
      ref,
      () => {
        return {
          invalid: getErrors,
          onSave: handleSave,
          onCancel: handleCancel,
        };
      },
      [getErrors, handleSave, handleCancel]
    );

    return (
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
    );
  }
);
