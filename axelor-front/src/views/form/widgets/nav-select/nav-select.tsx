import { useAtom } from "jotai";
import { useCallback, useMemo } from "react";

import {
  OverflowList,
  OverflowListItemProps,
  OverflowMenuTriggerProps,
  clsx,
} from "@axelor/ui";

import { Selection } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { useSelectionList } from "../selection/hooks";

import styles from "./nav-select.module.scss";

type SelectItem = {
  id: string;
  selection: Selection;
};

type ItemProps = OverflowListItemProps<SelectItem>;

function Item(props: ItemProps) {
  const { item, active } = props;
  const { selection } = item;
  return (
    <div className={clsx(styles.item, [{ [styles.active]: active }])}>
      <div className={styles.text}>{selection.title}</div>
    </div>
  );
}

function MenuTrigger({ count, open }: OverflowMenuTriggerProps) {
  return (
    <div className={clsx(styles.item, [{ [styles.open]: open }])}>
      <div className={styles.text}>+{count}</div>
    </div>
  );
}

function MenuItem({ item }: ItemProps) {
  const { selection } = item;
  return <>{selection.title}</>;
}

function isSelected(selection: Selection, value: any) {
  const val = selection.value;
  return val === value || String(val) === String(value);
}

export function NavSelect(props: FieldProps<string | number>) {
  const { schema, readonly, widgetAtom, valueAtom } = props;
  const [value, setValue] = useAtom(valueAtom);

  const selection = useSelectionList({ value, widgetAtom, schema });
  const items: SelectItem[] = useMemo(() => {
    return selection.map((selection, i) => {
      return {
        id: String(selection.title ?? i),
        selection,
        priority: isSelected(selection, value) ? 1 : 0,
      };
    });
  }, [selection, value]);

  const onItemClick = useCallback(
    ({ selection }: SelectItem) => {
      if (readonly) return;
      setValue(selection.value, true);
    },
    [readonly, setValue]
  );

  const isItemActive = useCallback(
    ({ selection }: SelectItem) => isSelected(selection, value),
    [value]
  );

  return (
    <FieldControl {...props}>
      <OverflowList
        className={styles.container}
        items={items}
        isItemActive={isItemActive}
        onItemClick={onItemClick}
        renderItem={Item}
        renderMenuTrigger={MenuTrigger}
        renderMenuItem={MenuItem}
      />
    </FieldControl>
  );
}
