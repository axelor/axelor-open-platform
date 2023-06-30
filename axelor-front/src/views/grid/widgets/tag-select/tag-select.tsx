import {
  Box,
  Menu,
  MenuItem,
  Overflow,
  OverflowItem,
  OverflowItemProps,
  useIsOverflowItemVisible,
  useOverflowMenu,
  useRefs,
} from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { Field } from "@/services/client/meta.types";
import { Chip } from "@/views/form/widgets";
import { DataRecord } from "@/services/client/data.types";
import { useCallback, useMemo, useState } from "react";
import { useAppTheme } from "@/hooks/use-app-theme";

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
  const [target, setTarget] = useState<HTMLDivElement | null>(null);

  const iconRef = useRefs(ref, (el: HTMLDivElement | null) => {
    setTarget(el);
  });

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
    <Box>
      <Box ref={iconRef} onClick={showMenu}>
        <Chip
          title={`+${overflowCount ?? ""}`}
          color={theme === "dark" ? "gray" : "white"}
        />
      </Box>
      <Menu target={target} show={show} onHide={hideMenu} navigation>
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
    [record, name]
  );

  const renderItem = useCallback(
    (item: DataRecord) => <Chip title={item[targetName]} color={"indigo"} />,
    [targetName]
  );

  return (
    <Overflow>
      <Box d="flex" flexWrap="nowrap" gap={4}>
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
