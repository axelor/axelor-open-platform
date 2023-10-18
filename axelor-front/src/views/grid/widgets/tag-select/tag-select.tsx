import { useCallback, useMemo, useRef, useState } from "react";
import {
  Box,
  Menu,
  MenuItem,
  Overflow,
  OverflowItem,
  OverflowItemProps,
  clsx,
  useIsOverflowItemVisible,
  useOverflowMenu,
  useRefs,
} from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { Field } from "@/services/client/meta.types";
import { SelectionTag } from "@/views/form/widgets";
import { DataRecord } from "@/services/client/data.types";
import { useAppTheme } from "@/hooks/use-app-theme";
import styles from "./tag-select.module.scss";

const OverflowMenuItem: React.FC<OverflowItemProps> = (props) => {
  const { id, children } = props;
  const isVisible = useIsOverflowItemVisible(id);

  if (isVisible) {
    return null;
  }

  return <MenuItem>{children}</MenuItem>;
};

const OverflowMenu: React.FC<{
  items: DataRecord[];
  renderItem: (item: DataRecord) => JSX.Element;
}> = ({ items, renderItem }) => {
  const theme = useAppTheme();
  const { ref, overflowCount, isOverflowing } =
    useOverflowMenu<HTMLDivElement>();

  const [show, setShow] = useState(false);
  const targetRef = useRef<HTMLDivElement | null>(null);

  const iconRef = useRefs(ref, targetRef);

  function showMenu() {
    setShow(true);
  }

  function hideMenu() {
    setShow(false);
  }

  if (!isOverflowing) {
    return null;
  }

  return (
    <Box onMouseLeave={hideMenu}>
      <Box ref={iconRef} onMouseEnter={showMenu}>
        <SelectionTag
          title={`+${overflowCount ?? ""}`}
          color={theme === "dark" ? "gray" : "white"}
          className={clsx(styles.count, styles[theme])}
        />
      </Box>
      <Menu
        className={styles.menu}
        target={targetRef.current}
        show={show}
        onHide={hideMenu}
      >
        {items.map((item, i) => {
          return (
            <OverflowMenuItem key={i} id={String(item.id!)}>
              {renderItem(item)}
            </OverflowMenuItem>
          );
        })}
      </Menu>
    </Box>
  );
};

export function TagSelect(props: GridColumnProps) {
  const { record, data } = props;
  const { name, targetName = "" } = data as Field;
  const list = useMemo(
    () =>
      (record?.[name] || []).map((item: DataRecord, ind: number) => ({
        ...item,
        id: item.id ?? `item_${ind}`,
      })) as DataRecord[],
    [record, name],
  );

  const getTitle = useCallback(
    (record: DataRecord) => record[`$t:${targetName}`] ?? record[targetName],
    [targetName],
  );

  const renderItem = useCallback(
    (item: DataRecord) => (
      <SelectionTag title={getTitle(item)} color={"indigo"} />
    ),
    [getTitle],
  );

  return (
    <Overflow>
      <Box d="flex" flexWrap="nowrap" gap={4} w={100}>
        {list.map((item) => (
          <OverflowItem key={item.id} id={String(item.id!)}>
            <Box>{renderItem(item)}</Box>
          </OverflowItem>
        ))}
        <OverflowMenu items={list} renderItem={renderItem} />
      </Box>
    </Overflow>
  );
}
