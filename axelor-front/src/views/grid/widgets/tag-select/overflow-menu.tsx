import { useRef, useState } from "react";
import {
  Box,
  Menu,
  MenuItem,
  OverflowItemProps,
  clsx,
  useIsOverflowItemVisible,
  useOverflowMenu,
  useRefs,
} from "@axelor/ui";

import { SelectionTag } from "@/views/form/widgets";
import { useAppTheme } from "@/hooks/use-app-theme";
import styles from "./overflow-menu.module.scss";

const OverflowMenuItem: React.FC<OverflowItemProps> = (props) => {
  const { id, children } = props;
  const isVisible = useIsOverflowItemVisible(id);

  if (isVisible) {
    return null;
  }

  return <MenuItem>{children}</MenuItem>;
};

export function OverflowMenu<T>({
  items,
  getItemKey,
  renderItem,
}: {
  getItemKey?: (item: T) => string | number;
  items: T[];
  renderItem: (item: T) => JSX.Element;
}) {
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
            <OverflowMenuItem key={i} id={String(getItemKey?.(item) ?? item)}>
              {renderItem(item)}
            </OverflowMenuItem>
          );
        })}
      </Menu>
    </Box>
  );
}
