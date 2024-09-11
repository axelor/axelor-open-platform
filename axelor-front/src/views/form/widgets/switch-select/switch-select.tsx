import { useAtom } from "jotai";
import { useCallback, useMemo, useState } from "react";

import { Icon } from "@/components/icon";
import { Box, clsx } from "@axelor/ui";

import { Selection } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { isReferenceField } from "../../builder/utils";
import { useSelectionList } from "../selection/hooks";

import styles from "./switch-select.module.scss";

type SelectItem = {
  id: string;
  selection: Selection;
};

type ButtonSize = {
  width: number;
  height: number;
  top: number;
  left: number;
};

export function SwitchSelect(
  props: FieldProps<string | number | Record<string, number>>,
) {
  const { schema, readonly, widgetAtom, valueAtom } = props;
  const { labels = true } = schema;
  const [value, setValue] = useAtom(valueAtom);

  const [borderStyles, setBorderStyles] = useState<ButtonSize>();
  const [buttonsSize, setButtonsSize] = useState<ButtonSize[]>([]);

  const isReference = isReferenceField(schema);
  const selection = useSelectionList({ value, widgetAtom, schema });

  const buttonRefs = useCallback((e: HTMLDivElement) => {
    if (e !== null) {
      setButtonsSize((btnSizes) => [
        ...btnSizes,
        {
          width: e.offsetWidth,
          height: e.offsetHeight,
          top: e.offsetTop,
          left: e.offsetLeft,
        },
      ]);
    }
  }, []);

  const isSelected = useCallback(
    (itemSelection: Selection) => {
      const selected = itemSelection.value;
      const val = typeof value === "object" && value ? value.id : value;

      return selected === val || String(selected) === String(val);
    },
    [value],
  );

  const items: SelectItem[] = useMemo(() => {
    return selection.map((item, i) => {
      if (isSelected(item)) setBorderStyles({ ...buttonsSize[i] });

      return {
        id: String(item.title ?? i),
        selection: item,
      };
    });
  }, [buttonsSize, isSelected, selection]);

  const onItemClick = useCallback(
    ({ selection }: SelectItem) => {
      if (readonly) return;
      if (isReference) {
        const id = +selection.value!;
        setValue({ id }, true);
      } else {
        setValue(selection.value, true);
      }
    },
    [readonly, isReference, setValue],
  );

  return (
    <FieldControl {...props}>
      <Box d="flex" position="relative" flexDirection="row" flex={1}>
        {borderStyles && (
          <Box className={styles.activeBorder} style={borderStyles} />
        )}
        {items.map((item, index) => (
          <Box
            key={index}
            {...(!readonly && { onClick: () => onItemClick(item) })}
            ref={buttonRefs}
            title={item.selection.data?.description || (!labels && item.selection.title)}
            d="flex"
            textWrap={false}
            justifyContent="center"
            g={2}
            px={3}
            className={clsx(styles.item, [
              {
                [styles.active]: isSelected(item.selection),
                [styles.readonly]: readonly,
                [styles.first]: index === 0,
                [styles.last]: index === items.length - 1,
              },
            ])}
          >
            {item.selection.icon && (
              <Icon icon={item.selection.icon} className={styles.icon} />
            )}
            {labels && item.selection.title}
          </Box>
        ))}
      </Box>
    </FieldControl>
  );
}
