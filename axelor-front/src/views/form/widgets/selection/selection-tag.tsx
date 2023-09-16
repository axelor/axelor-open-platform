import { Badge, Box, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { legacyClassNames } from "@/styles/legacy";

import styles from "./selection.module.scss";
import React from "react";

export function SelectionTag({
  title,
  color,
  className,
  onRemove,
}: {
  title?: React.ReactNode;
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
          onClick={(e) => {
            if (onRemove) {
              e.preventDefault();
              e.stopPropagation();
              onRemove();
            }
          }}
        >
          <MaterialIcon icon="close" fontSize="1rem" />
        </Box>
      )}
    </Badge>
  )) as React.ReactElement;
}
