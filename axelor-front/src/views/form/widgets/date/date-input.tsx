import { AdornedInput, Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { forwardRef, useCallback, useRef } from "react";
import { MaskedInput } from "./mask-input";
import { useAsyncEffect } from "@/hooks/use-async-effect";

const CHAR_MASK: Record<string, (RegExp | ((ch: string) => RegExp))[]> = {
  M: [/[0-1]/, (prev) => (prev === "1" ? /[0-2]/ : /\d/)],
  D: [/[0-3]/, (prev) => (prev === "3" ? /[0-1]/ : /\d/)],
  H: [/[0-2]/, (prev) => (prev === "2" ? /[0-4]/ : /\d/)],
  m: [/[0-5]/],
  s: [/[0-5]/],
};

const SEPARATORS = new Set([" ", "/", ":", "-", "."]);

function getMaskFromFormat(
  value: string,
  format: string,
  placeholderChar = "_",
) {
  const mask: (string | RegExp)[] = [];

  for (let i = 0; i < format.length; i++) {
    const char = format[i];

    if (SEPARATORS.has(char)) {
      mask.push(char);
      continue;
    }

    const rules = CHAR_MASK[char];
    if (!rules) {
      mask.push(/\d/);
      continue;
    }

    if (format.indexOf(char) === i) {
      mask.push(rules[0] as RegExp);
    } else if (rules.length > 1 && typeof rules[1] === "function") {
      const prevChar = value[i - 1];
      mask.push(prevChar !== placeholderChar ? rules[1](prevChar) : /\d/);
    } else {
      mask.push(/\d/);
    }
  }

  return mask as RegExp[];
}

export const DateInput = forwardRef<any, any>(
  (
    {
      className,
      inputValue,
      format,
      open,
      onOpen,
      onClose,
      onKeyDown,
      ...props
    }: any,
    ref,
  ) => {
    const { name, eventOnBlur: onBlur, onChange, onFocus } = props;
    const mountRef = useRef(false);
    function handleBlur({ target: { name, value } }: any) {
      const changed = value !== inputValue;
      if (open || !changed) return;
      const event = {
        target: { name, value: value?.includes?.("_") ? "" : value },
      };
      onChange?.(event);
      onBlur?.(event);
    }

    function handleKeyDown(e: any) {
      if (e.key === "ArrowDown") {
        onOpen(true);
      }
      setTimeout(() => {
        onKeyDown?.(e);
      }, 100);
    }

    // sync date input with inputValue
    useAsyncEffect(async () => {
      if (mountRef.current) {
        onChange?.({
          target: {
            name,
            value: inputValue,
          },
        });
      }
      mountRef.current = true;
    }, [name, inputValue, onChange]);

    const mask = useCallback(
      (value: string) => getMaskFromFormat(value, format),
      [format],
    );

    return (
      <AdornedInput
        {...props}
        data-input
        className={
          open
            ? className
            : className &&
              className.replace("react-datepicker-ignore-onclickoutside", "")
        }
        onKeyDown={open ? onKeyDown : handleKeyDown}
        onFocus={onFocus}
        onBlur={handleBlur}
        mask={mask}
        ref={ref}
        InputComponent={MaskedInput}
        endAdornment={
          <Box onClick={open ? onClose : onOpen}>
            <MaterialIcon icon="calendar_today" variant="outlined" />
          </Box>
        }
      />
    );
  },
);
