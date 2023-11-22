import clsx from "clsx";
import { useAtom, useAtomValue } from "jotai";
import { CSSProperties, useCallback, useMemo } from "react";
import { selectAtom } from "jotai/utils";

import { Box } from "@axelor/ui";
import {
  BootstrapIcon,
  BootstrapIconName,
} from "@axelor/ui/icons/bootstrap-icon";
import { Field } from "@/services/client/meta.types";
import format from "@/utils/format";
import { FieldControl, FieldProps } from "@/views/form/builder";
import styles from "./rating.module.scss";

export function Rating(props: FieldProps<number>) {
  const { schema, readonly, valueAtom, formAtom } = props;
  const { maxSize = 5, widgetAttrs, required } = schema;
  const {
    ratingIcon = "star",
    ratingColor,
    ratingFill = true,
    ratingHighlightSelected = false,
  } = widgetAttrs || {};

  const ready = useAtomValue(
    useMemo(() => selectAtom(formAtom, (form) => form.ready), [formAtom]),
  );
  const [value, setValue] = useAtom(valueAtom);

  const handleClick = useCallback(
    (position: number, checked: boolean) => {
      if (readonly) return;
      if (checked && position === value) {
        setValue(required ? null : 0);
      } else {
        setValue(position);
      }
    },
    [readonly, value, setValue, required],
  );

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

  const text = useMemo(
    () =>
      value != null ? format(value, { props: { ...schema } as Field }) : "",
    [schema, value],
  );

  return (
    <FieldControl {...props}>
      <Box
        m={0}
        d="flex"
        className={clsx([styles.container], {
          [styles.pointer]: !readonly,
        })}
      >
        {ready &&
          Array.from({ length: maxSize }, (v, k) => k + 1).map((position) => {
            const partialWidth = getPartialWidth(position);
            const checked = position <= Math.ceil(value ?? 0);
            const posIcon = getIcon(position);
            const highlightMe = ratingHighlightSelected
              ? value === position
              : true;
            const color = getColor(position);
            const style =
              (color ? { style: { color: color } } : null) ??
              PREDEFINED_ICONS[posIcon] ??
              {};

            return (
              <Box
                key={position}
                {...(!readonly && {
                  onClick: () => handleClick(position, checked),
                })}
                style={{ ...(checked && highlightMe ? style.style : {}) }}
                title={text}
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
                      <BootstrapIcon
                        icon={posIcon}
                        fill={ratingFill}
                        className={clsx([styles.icon], {
                          [styles.iconHover]: !readonly,
                        })}
                      />
                    </Box>
                    <Box
                      style={{
                        position: "absolute",
                        top: "0",
                        left: "0",
                      }}
                    >
                      <BootstrapIcon
                        icon={posIcon}
                        fill={false}
                        className={clsx([styles.icon], {
                          [styles.iconHover]: !readonly,
                        })}
                      />
                    </Box>
                  </Box>
                ) : (
                  <BootstrapIcon
                    icon={posIcon}
                    fill={ratingFill && checked}
                    className={clsx([styles.icon], {
                      [styles.iconHover]: !readonly,
                    })}
                  />
                )}
              </Box>
            );
          })}
      </Box>
    </FieldControl>
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
