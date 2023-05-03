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
import { useAtomCallback } from "jotai/utils";
import { isEqual } from "lodash";

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

export type GridFormHandler = {
  onSave?: () => void;
  onCancel?: () => void;
};

export const FormLayoutComponent = ({
  schema,
  formAtom,
  readonly,
  onSave,
  onCancel,
  columns = [],
}: LayoutProps) => {
  const items = useMemo<Schema[]>(() => {
    return (schema.items || []).map((item) => ({
      ...item,
      editable: true,
      showTitle: false,
    }));
  }, [schema.items]);

  const handleKeyDown = useCallback(
    function handleKeyDown(e: KeyboardEvent<HTMLDivElement>) {
      if (e.defaultPrevented && e.detail !== 1) return;
      if (e.key === `Escape`) {
        return onCancel?.();
      }
      if (e.key === `Enter`) {
        return onSave?.();
      }
    },
    [onSave, onCancel]
  );

  return (
    <Box d="flex" onKeyDown={handleKeyDown}>
      {columns.map((column, ind) => {
        const item = items.find((item) => item.name === column.name);
        return (
          <Box
            p={1}
            key={item?.uid ?? `column_${ind}`}
            {...(column.width && {
              style: {
                width: column.width,
                minWidth: column.width,
              },
            })}
          >
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

    const handleCancel = useCallback(
      () => onCancel?.(record, rowIndex, cellIndex!),
      [onCancel, record, rowIndex, cellIndex]
    );

    const handleSave = useAtomCallback(
      useCallback(
        async (get) => {
          const { record: saveRecord, states } = get(formAtom);

          // check record changes
          if (isEqual(record, saveRecord)) {
            return onSave?.(record, rowIndex, cellIndex!, false);
          }

          const errors = Object.values(states)
            .map((s) => s.errors ?? {})
            .filter((x) => Object.keys(x).length > 0);

          if (errors.length > 0) {
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

          return await onSave?.(saveRecord, rowIndex, cellIndex!, true);
        },
        [formAtom, record, rowIndex, cellIndex, onSave, actionExecutor]
      )
    );

    const handleClickOutside = useAtomCallback(
      useCallback(
        (get, set, e: Event) => {
          if (e.defaultPrevented) return;

          const { record } = get(formAtom);

          if (isEqual(record, recordRef.current)) {
            return;
          }

          recordRef.current = record;

          // except id, other fields is exist
          if (Object.keys(record).length > 1) {
            handleSave();
          } else {
            handleCancel();
          }
        },
        [formAtom, handleSave, handleCancel]
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
          onSave: handleSave,
          onCancel: handleCancel,
        };
      },
      [handleSave, handleCancel]
    );

    return (
      <FocusTrap>
        <Box ref={containerRef} d="flex" className={className}>
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
