import { useCallback, useMemo, useState } from "react";

import { Icon } from "@/components/icon";
import { Box, clsx } from "@axelor/ui";

import { Selection } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import {
  SelectItem,
  useSelectionList,
  useSelectionValue,
} from "../selection/hooks";

import styles from "./switch-select.module.scss";

type ButtonSize = {
  width: number;
  height: number;
  top: number;
  left: number;
};

export function SwitchSelect(
  props: FieldProps<string | number | Record<string, number>>,
) {
  const { schema, readonly, widgetAtom } = props;
  const { labels = true, direction = "horizontal" } = schema;
  const [value, handleChange] = useSelectionValue(props, {
    disabled: readonly,
  });

  const [borderStyles, setBorderStyles] = useState<ButtonSize>();
  const [buttonsSize, setButtonsSize] = useState<ButtonSize[]>([]);

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

  return (
    <FieldControl
      {...props}
      className={direction === "vertical" ? styles.inlineBlock : styles.flex}
    >
      <Box
        d={direction === "vertical" ? "inline-block" : "flex"}
        overflow={direction === "vertical" ? "auto" : "scroll"}
        position="relative"
      >
        {borderStyles && (
          <Box className={styles.activeBorder} style={borderStyles} />
        )}
        {items.map((item, index) => (
          <Box
            key={index}
            {...(!readonly && { onClick: () => handleChange(item) })}
            ref={buttonRefs}
            title={
              item.selection.data?.description ||
              (!labels ? item.selection.title : undefined)
            }
            d="flex"
            textWrap={false}
            alignItems="center"
            justifyContent="center"
            g={2}
            py={direction === "vertical" ? 2 : 1}
            px={3}
            className={clsx(styles.item, [
              {
                [styles.vertical]: direction === "vertical",
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
