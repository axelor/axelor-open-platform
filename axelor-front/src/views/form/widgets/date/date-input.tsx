import { AdornedInput, Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { forwardRef, useEffect } from "react";
import { MaskedInput } from "./mask-input";

const CHAR_MASK: Record<string, RegExp> = {
  M: /[0-1]/,
  H: /[0-2]/,
  m: /[0-5]/,
  s: /[0-5]/,
};

function getMaskFromFormat(str: string) {
  const mask = [];
  for (let i = 0; i < str.length; i++) {
    const ch = str[i];
    if ([" ", "/", ":", "-", "."].includes(ch)) {
      mask.push(ch);
    } else {
      if (CHAR_MASK[ch] && str.indexOf(ch) === i) {
        mask.push(CHAR_MASK[ch]);
      } else {
        mask.push(/\d/);
      }
    }
  }
  return mask;
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

    function handleBlur({ target: { name, value } }: any) {
      if (open) return;
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
        onKeyDown && onKeyDown(e);
      }, 100);
    }

    // sync date input with inputValue
    useEffect(() => {
      onChange?.({
        target: {
          name,
          value: inputValue,
        },
      });
    }, [name, inputValue, onChange]);

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
        mask={getMaskFromFormat(format)}
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
