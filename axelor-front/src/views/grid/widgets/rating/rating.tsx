import { Box } from "@axelor/ui";
import {
  BootstrapIcon,
  BootstrapIconName,
} from "@axelor/ui/icons/bootstrap-icon";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";
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

  function getIcon(position: number): BootstrapIconName {
    const icons = ratingIcon.trim().split(/\s*,\s*/);
    if (icons.length <= 1) {
      return ratingIcon;
    }
    return icons[position - 1];
  }

  function getColor(position: number): string | null {
    const colors = ratingColor ? ratingColor.trim().split(/\s*,\s*/) : [];
    if (colors.length <= 0) {
      return null;
    }
    return colors[position - 1];
  }

  return (
    <Box d="inline-flex">
      {Array.from({ length: maxSize }, (v, k) => k + 1).map((position, i) => {
        const checked = position <= (value ?? 0);
        const posIcon = getIcon(position);
        const highlightMe = ratingHighlightSelected ? value == position : true;
        const color = getColor(position);
        const style =
          (color && { style: { color: color } }) ??
          PREDEFINED_ICONS[posIcon] ??
          {};

        return (
          <Box
            key={position}
            style={{ ...(checked && highlightMe ? style.style : {}) }}
          >
            <BootstrapIcon icon={posIcon} fill={ratingFill && checked} />
          </Box>
        );
      })}
    </Box>
  );
}

const PREDEFINED_ICONS: Record<string, any> = {
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
