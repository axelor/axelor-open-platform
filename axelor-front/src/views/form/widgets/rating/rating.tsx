import clsx from "clsx";
import { Box } from "@axelor/ui";
import { FieldControl, FieldProps } from "@/views/form/builder";
import { useAtom } from "jotai/index";
import {
  BootstrapIcon,
  BootstrapIconName,
} from "@axelor/ui/icons/bootstrap-icon";
import styles from "./rating.module.scss";

export function Rating(props: FieldProps<number>) {
  const { schema, readonly, valueAtom } = props;
  const { maxSize = 5, widgetAttrs, required } = schema;
  const {
    ratingIcon = "star",
    ratingColor,
    ratingFill = true,
    ratingHighlightSelected = false,
  } = widgetAttrs || {};
  const [value, setValue] = useAtom(valueAtom);

  function handleClick(position: number, checked: boolean) {
    if (readonly) return;
    if (checked && position == value) {
      setValue(required ? null : 0);
    } else {
      setValue(position);
    }
  }

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
    <FieldControl {...props}>
      <Box
        m={0}
        d="flex"
        className={clsx([styles.container], {
          [styles.pointer]: !readonly,
        })}
      >
        {Array.from({ length: maxSize }, (v, k) => k + 1).map((position, i) => {
          const checked = position <= (value ?? 0);
          const posIcon = getIcon(position);
          const highlightMe = ratingHighlightSelected
            ? value == position
            : true;
          const color = getColor(position);
          const style =
            (color && { style: { color: color } }) ??
            PREDEFINED_ICONS[posIcon] ??
            {};

          return (
            <Box
              key={position}
              {...(!readonly && {
                onClick: () => handleClick(position, checked),
              })}
              style={{ ...(checked && highlightMe ? style.style : {}) }}
            >
              <BootstrapIcon
                icon={posIcon}
                fill={ratingFill && checked}
                className={clsx([styles.icon], {
                  [styles.iconHover]: !readonly,
                })}
              />
            </Box>
          );
        })}
      </Box>
    </FieldControl>
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
