import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { Box, clsx } from "@axelor/ui";

import { Icon } from "@/components/icon";
import { Selection } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { useSelectionList, useSelectionValue } from "../selection/hooks";

import styles from "./switch-select.module.scss";

type HighlightStyle = {
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

  const containerRef = useRef<HTMLDivElement>(null);
  const [highlightStyles, setHighlightStyles] =
    useState<HighlightStyle | null>();

  const [value, handleChange] = useSelectionValue(props, {
    disabled: readonly,
  });
  const selection = useSelectionList({ value, widgetAtom, schema });

  const isSelected = useCallback(
    (itemSelection: Selection) => {
      const selected = itemSelection.value;
      const val = typeof value === "object" && value ? value.id : value;

      return selected === val || String(selected) === String(val);
    },
    [value],
  );

  const items = useMemo(
    () =>
      selection.map((item, i) => ({
        id: String(item.title ?? i),
        selected: isSelected(item),
        selection: item,
      })),
    [selection, isSelected],
  );

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const listItems = Array.from(container.children).filter(
      (item) => !item.classList.contains(styles.highlighter),
    );
    const selectedIndex = items.findIndex((item) => item.selected);
    const selectElement = listItems[selectedIndex] as HTMLElement;
    setHighlightStyles(
      selectElement
        ? {
            width: selectElement.offsetWidth,
            height: selectElement.offsetHeight,
            top: selectElement.offsetTop,
            left: selectElement.offsetLeft,
          }
        : null,
    );
  }, [items]);

  const vertical = direction === "vertical";

  return (
    <FieldControl {...props} className={vertical ? styles.inline : styles.flex}>
      <Box
        ref={containerRef}
        d={vertical ? "inline-block" : "flex"}
        overflow={vertical ? "visible" : "auto"}
        position="relative"
      >
        {highlightStyles && (
          <Box className={styles.highlighter} style={highlightStyles} />
        )}
        {items.map((item, index) => {
          const { title, icon, data } = item.selection;
          const help = data?.description || (labels ? "" : title);
          return (
            <Box
              key={index}
              title={help}
              d="flex"
              textWrap={false}
              alignItems="center"
              justifyContent="center"
              g={2}
              py={vertical ? 2 : 1}
              px={3}
              className={clsx(styles.item, {
                [styles.vertical]: vertical,
                [styles.horizontal]: !vertical,
                [styles.readonly]: readonly,
                [styles.active]: item.selected,
                [styles.first]: index === 0,
                [styles.last]: index === items.length - 1,
              })}
              {...(!readonly && { onClick: () => handleChange(item) })}
            >
              {icon && <Icon icon={icon} className={styles.icon} />}
              {labels && title}
            </Box>
          );
        })}
      </Box>
    </FieldControl>
  );
}
