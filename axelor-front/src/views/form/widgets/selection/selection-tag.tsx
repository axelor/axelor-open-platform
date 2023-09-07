import { Badge, Box, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { legacyClassNames } from "@/styles/legacy";

import styles from "./selection.module.scss";

export function SelectionTag({
  title,
  color,
  className,
  onRemove,
}: {
  title?: string;
  color?: string;
  className?: string;
  onRemove?: () => void;
}) {
  return (title && (
    <Badge
      px={2}
      className={clsx(
        styles["tag"],
        className,
        legacyClassNames(`hilite-${color}`),
      )}
    >
      <Box as="span" className={styles["tag-text"]}>
        {title}
      </Box>
      {onRemove && (
        <Box
          as="span"
          className={styles["tag-remove"]}
          onClick={onRemove}
          onMouseDown={(e) => {
            e.preventDefault();
            e.stopPropagation();
          }}
        >
          <MaterialIcon icon="close" fontSize={20} />
        </Box>
      )}
    </Badge>
  )) as React.ReactElement;
}
