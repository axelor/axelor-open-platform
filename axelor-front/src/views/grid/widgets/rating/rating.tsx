import { CSSProperties, useCallback } from "react";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import {
  BootstrapIcon,
  BootstrapIconName,
} from "@axelor/ui/icons/bootstrap-icon";

import { Schema } from "@/services/client/meta.types.ts";

export function Rating(props: GridColumnProps) {
  const { record, data } = props;
  const { maxSize = 5, widgetAttrs } = data as Schema;
  const {
    ratingIcon = "star",
    ratingColor,
    ratingFill = true,
    ratingHighlightSelected = false,
  } = widgetAttrs || {};
  const value = record?.[data?.name];

  const getIcon = useCallback(
    (position: number): BootstrapIconName => {
      const icons = ratingIcon.trim().split(/\s*,\s*/);
      if (icons.length <= 1) {
        return ratingIcon;
      }
      return icons[position - 1];
    },
    [ratingIcon],
  );

  const getColor = useCallback(
    (position: number): string | null => {
      const colors = ratingColor ? ratingColor.trim().split(/\s*,\s*/) : [];
      if (colors.length <= 0) {
        return null;
      }
      return colors[position - 1];
    },
    [ratingColor],
  );

  const getPartialWidth = useCallback(
    (position: number): number | null => {
      const intValue = Math.floor(value ?? 0);
      const decimalValue = (value ?? 0) - intValue;
      return position === intValue + 1 && decimalValue > 0
        ? Math.min(Math.max(decimalValue * 100 - 1, 25), 75)
        : null;
    },
    [value],
  );

  return (
    <Box d="inline-flex">
      {Array.from({ length: maxSize }, (v, k) => k + 1).map((position) => {
        const partialWidth = getPartialWidth(position);
        const checked = position <= Math.ceil(value ?? 0);
        const posIcon = getIcon(position);
        const highlightMe = ratingHighlightSelected ? value === position : true;
        const color = getColor(position);
        const style =
          (color ? { style: { color: color } } : null) ??
          PREDEFINED_ICONS[posIcon] ??
          {};

        return (
          <Box
            key={position}
            style={{ ...(checked && highlightMe ? style.style : {}) }}
          >
            {partialWidth !== null ? (
              <Box
                style={{
                  overflow: "hidden",
                  position: "relative",
                }}
              >
                <Box
                  style={{
                    overflow: "hidden",
                    position: "relative",
                    width: `${partialWidth}%`,
                  }}
                >
                  <BootstrapIcon icon={posIcon} fill={ratingFill} />
                </Box>
                <Box
                  style={{
                    position: "absolute",
                    top: "0",
                    left: "0",
                  }}
                >
                  <BootstrapIcon icon={posIcon} fill={false} />
                </Box>
              </Box>
            ) : (
              <BootstrapIcon icon={posIcon} fill={ratingFill && checked} />
            )}
          </Box>
        );
      })}
    </Box>
  );
}

const PREDEFINED_ICONS: Record<string, { style: CSSProperties }> = {
  star: {
    style: {
      color: "#faaf00",
    },
  },
  heart: {
    style: {
      color: "#ff6d75",
    },
  },
};
