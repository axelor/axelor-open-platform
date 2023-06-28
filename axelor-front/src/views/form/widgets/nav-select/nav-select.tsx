import { useAtom } from "jotai";
import { forwardRef, useCallback, useMemo, useState } from "react";

import {
  Menu,
  MenuItem,
  Overflow,
  OverflowItem,
  clsx,
  useIsOverflowItemVisible,
  useOverflowMenu,
  useRefs,
} from "@axelor/ui";

import { Selection } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { useSelectionList } from "../selection/hooks";

import styles from "./nav-select.module.scss";

type SelectItem = {
  id: string;
  selection: Selection;
};

type ItemProps = {
  item: SelectItem;
  onClick: (item: SelectItem) => void;
  className?: string;
};

type ItemsProps = {
  items: SelectItem[];
  onClick: (item: SelectItem) => void;
};

const Item = forwardRef<HTMLDivElement, ItemProps>((props, ref) => {
  const { item, onClick, className } = props;
  const { selection } = item;
  const handleClick = useCallback(() => onClick(item), [item, onClick]);
  return (
    <div
      ref={ref}
      className={clsx(styles.item, className)}
      onClick={handleClick}
    >
      <div className={styles.text}>{selection.title}</div>
    </div>
  );
});

function OverflowMenuItem({ item, onClick }: ItemProps) {
  const { id, selection } = item;
  const isVisible = useIsOverflowItemVisible(id);
  const handleClick = useCallback(() => onClick(item), [item, onClick]);

  if (isVisible) {
    return null;
  }

  return <MenuItem onClick={handleClick}>{selection.title}</MenuItem>;
}

function OverflowMenu({ items, onClick }: ItemsProps) {
  const { ref, isOverflowing, overflowCount } =
    useOverflowMenu<HTMLDivElement>();

  const [show, setShow] = useState(false);
  const [target, setTarget] = useState<HTMLDivElement | null>(null);

  const iconRef = useRefs(ref, (el: HTMLDivElement | null) => setTarget(el));

  const showMenu = useCallback(() => setShow(true), []);
  const hideMenu = useCallback(() => setShow(false), []);

  const handleClick = useCallback(
    (item: SelectItem) => {
      onClick(item);
      setShow(false);
    },
    [onClick]
  );

  if (!isOverflowing) {
    return null;
  }

  return (
    <div className={styles.itemWrapper}>
      <Item
        ref={iconRef}
        onClick={() => showMenu()}
        className={clsx(styles.more, {
          [styles.open]: show,
        })}
        item={{
          id: "",
          selection: {
            title: `+${overflowCount}`,
          },
        }}
      ></Item>
      <Menu target={target} show={show} onHide={hideMenu} navigation>
        {items.map((item) => (
          <OverflowMenuItem key={item.id} item={item} onClick={handleClick} />
        ))}
      </Menu>
    </div>
  );
}

export function NavSelect(props: FieldProps<string | number>) {
  const { schema, readonly, widgetAtom, valueAtom } = props;
  const [value, setValue] = useAtom(valueAtom);

  const selection = useSelectionList({ value, widgetAtom, schema });
  const items: SelectItem[] = useMemo(() => {
    return selection.map((selection, i) => {
      return { id: String(selection.title ?? i), selection };
    });
  }, [selection]);

  const onItemClick = useCallback(
    (item: SelectItem) => {
      if (readonly) return;
      const { selection } = item;
      setValue(selection.value, true);
    },
    [readonly, setValue]
  );

  const isActive = useCallback(
    (selection: Selection) => {
      const val = selection.value;
      return val === value || String(val) === String(value);
    },
    [value]
  );

  return (
    <FieldControl {...props}>
      <Overflow>
        <div className={styles.container}>
          {items.map((item) => (
            <OverflowItem key={item.id} id={item.id}>
              <div className={styles.itemWrapper}>
                <Item
                  item={item}
                  onClick={onItemClick}
                  className={clsx({
                    [styles.active]: isActive(item.selection),
                  })}
                />
              </div>
            </OverflowItem>
          ))}
          <OverflowMenu items={items} onClick={onItemClick} />
        </div>
      </Overflow>
    </FieldControl>
  );
}
