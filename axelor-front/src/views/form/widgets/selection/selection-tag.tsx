import React from "react";

import { Badge, Box, TVariant, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { legacyClassNames } from "@/styles/legacy";

import styles from "./selection.module.scss";

const VARIANTS: TVariant[] = [
  "primary",
  "secondary",
  "success",
  "danger",
  "warning",
  "info",
  "light",
  "dark",
];

const COLOR_MAPS: Record<string, TVariant> = {
  white: "light",
  black: "dark",
};

export function SelectionTag({
  title,
  color: _color,
  className,
  onRemove,
}: {
  title?: React.ReactNode;
  color?: string;
  className?: string;
  onRemove?: () => void;
}) {
  const color = _color ?? "primary";
  const variant = COLOR_MAPS[color!] ?? (color as TVariant);
  const isVariant = variant && VARIANTS.includes(variant);
  const colorProps = isVariant ? { variant } : undefined;
  const colorClass = isVariant
    ? undefined
    : legacyClassNames(`hilite-${color}`);

  if (title) {
    return (
      <Badge
        {...colorProps}
        px={2}
        className={clsx(styles["tag"], className, colorClass)}
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
    );
  }
}
