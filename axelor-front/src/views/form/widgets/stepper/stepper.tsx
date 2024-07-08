import { useAtom, useAtomValue } from "jotai";
import { uniqueId } from "lodash";
import { useCallback, useMemo } from "react";

import { Box, Button, clsx, OverflowList } from "@axelor/ui";

import { useResizeDetector } from "@/hooks/use-resize-detector";
import { DataRecord } from "@/services/client/data.types";
import { Selection } from "@/services/client/meta.types";

import { FieldControl, FieldProps, ValueAtom } from "../../builder";
import { createWidgetAtom } from "../../builder/atoms";
import { isReferenceField } from "../../builder/utils";
import { ManyToOne } from "../many-to-one";
import { useSelectionList } from "../selection/hooks";
import { Step } from "./step";

import styles from "./step.module.scss";

const STEP_MIN_WIDTH = 100;

type SelectItem = {
  id: any;
  selection: Selection;
};

export function Stepper(
  props: FieldProps<string | number | Record<string, number>>,
) {
  const { schema, widgetAtom, formAtom, valueAtom } = props;
  const { widgetAttrs } = schema;
  const {
    stepperCompleted = true,
    stepperType = "numeric",
    stepperShowDescription = false,
  } = widgetAttrs;
  const [value, setValue] = useAtom(valueAtom);

  const { attrs } = useAtomValue(widgetAtom);
  const { readonly } = attrs;

  const isReference = isReferenceField(schema);
  const selection = useSelectionList({ value, widgetAtom, schema });
  const items: SelectItem[] = useMemo(() => {
    return selection.map((item, i) => {
      return {
        id: i + 1,
        selection: item,
      };
    });
  }, [selection]);

  const { ref, width } = useResizeDetector();
  const stepWidth = useMemo(() => {
    if (!width || width / items.length < STEP_MIN_WIDTH) {
      return STEP_MIN_WIDTH;
    }
    return width / items.length;
  }, [items.length, width]);

  const selectedIndex = useMemo(() => {
    const val = typeof value === "object" && value ? value.id : value;
    return items.findIndex((item) => item.selection.value == val) + 1;
  }, [items, value]);

  const handleOnClick = useCallback(
    (itemProps: SelectItem) => {
      if (readonly) return;
      if (isReference) {
        const id = +itemProps.selection.value!;
        setValue({ id }, true);
      } else {
        setValue(itemProps.selection.value, true);
      }
    },
    [isReference, readonly, setValue],
  );

  const Item = useCallback(
    (item: SelectItem) => (
      <Step
        width={stepWidth}
        index={
          isReference
            ? selection.findIndex((i) => i.value == item.selection.value) + 1
            : item.id
        }
        selectedIndex={selectedIndex}
        label={item.selection.title}
        description={item.selection.data?.description}
        icon={item.selection.icon}
        completed={stepperCompleted}
        stepperType={stepperType}
        showDescription={stepperShowDescription}
        readonly={readonly}
      />
    ),
    [
      isReference,
      readonly,
      selectedIndex,
      selection,
      stepperShowDescription,
      stepWidth,
      stepperType,
      stepperCompleted,
    ],
  );

  const hiddenWidgetAtom = useMemo(
    () =>
      isReference
        ? createWidgetAtom({
            schema: {
              ...schema,
              uid: uniqueId("w"),
              name: uniqueId("$nav"),
              hideIf: undefined,
              showIf: undefined,
              hidden: true,
            },
            formAtom,
          })
        : null,
    [isReference, formAtom, schema],
  );

  return (
    <FieldControl {...props}>
      {hiddenWidgetAtom && (
        <ManyToOne
          schema={schema}
          widgetAtom={hiddenWidgetAtom}
          valueAtom={valueAtom as ValueAtom<DataRecord>}
          formAtom={formAtom}
        />
      )}
      <Box ref={ref} d="flex" flexDirection="row">
        {stepWidth === STEP_MIN_WIDTH ? (
          <OverflowList
            items={items}
            isItemActive={(item) => selectedIndex === item.id}
            onItemClick={handleOnClick}
            renderItem={({ item }) => <Item {...item} />}
            renderMenuTrigger={({ count }) => (
              <Step
                width={STEP_MIN_WIDTH}
                index={items.length - count + 1}
                selectedIndex={selectedIndex}
                remainingCount={count}
                completed={stepperCompleted}
              />
            )}
            renderMenuItem={({ item }) => (
              <Box>
                {stepperType === "numeric" && `${item.id} - `}
                {item.selection.title}
              </Box>
            )}
          />
        ) : (
          items.map((item, index) => (
            <Button
              className={clsx(styles.stepButton, [
                { [styles.readonly]: readonly },
              ])}
              key={index}
              d="flex"
              p={0}
              border={false}
              onClick={() => handleOnClick(item)}
            >
              <Item {...item} />
            </Button>
          ))
        )}
      </Box>
    </FieldControl>
  );
}
