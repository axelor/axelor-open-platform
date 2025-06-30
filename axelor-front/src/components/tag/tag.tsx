import React, { useMemo } from "react";

import { Badge, Box, TVariant, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { legacyClassNames } from "@/styles/legacy";
import { useAppSettings } from "@/hooks/use-app-settings";

import styles from "./tag.module.scss";

type TagProps = {
  /**
   * Title content
   */
  title?: React.ReactNode;
  /**
   * Color to apply in background: can be either a variant (`primary`, `secondary`, ...),
   * a bult-in color name (`red`, `brown`, ...) or a hexadecimal value.
   */
  color?: string;
  /**
   * CSS class name
   */
  className?: string;
  /**
   * A callback function that is invoked when an item is removed. This adds the ability to remove the tag.
   */
  onRemove?: () => void;
};

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

const HEXADECIMAL_REGEX = /^#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6,8})$/;

/**
 * A component that renders a tag with optional styling, text, and a remove action.
 */
export function Tag(props: TagProps) {
  const { title, className, onRemove, color: _color } = props;

  const color = _color ?? "primary";
  const variant = COLOR_MAPS[color!] ?? (color as TVariant);
  const isVariant = variant && VARIANTS.includes(variant);
  const colorProps = isVariant ? { variant } : undefined;
  const colorClass = isVariant
    ? undefined
    : legacyClassNames(`hilite-${color}`);

  const { themeMode } = useAppSettings();

  const textColor = useMemo(() => {
    if (HEXADECIMAL_REGEX.test(color)) {
      const bgColor = color.substring(1);
      let r, g, b, a;

      if (bgColor.length >= 6) {
        r = parseInt(bgColor.substring(0, 2), 16);
        g = parseInt(bgColor.substring(2, 4), 16);
        b = parseInt(bgColor.substring(4, 6), 16);
        a = parseInt(bgColor.substring(6, 8), 16);
      } else {
        r = parseInt(bgColor[0] + bgColor[0], 16);
        g = parseInt(bgColor[1] + bgColor[1], 16);
        b = parseInt(bgColor[2] + bgColor[2], 16);
        a = parseInt(bgColor[3] + bgColor[3], 16);
      }

      const srgb = [r / 255, g / 255, b / 255];
      const [R, G, B] = srgb.map((i) =>
        i <= 0.04045 ? i / 12.92 : ((i + 0.055) / 1.055) ** 2.4,
      );

      if (a / 255 < 0.5) {
        return themeMode === "light" ? "black" : "white";
      } else {
        return 0.2126 * R + 0.7152 * G + 0.0722 * B > 0.179 ? "black" : "white";
      }
    }
  }, [color, themeMode]);

  if (title) {
    return (
      <Badge
        {...(HEXADECIMAL_REGEX.test(color)
          ? { style: { backgroundColor: color, color: textColor } }
          : { ...colorProps })}
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
            <MaterialIcon icon="close" />
          </Box>
        )}
      </Badge>
    );
  }
}
