import { forwardRef } from "react";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { Box } from "@axelor/ui";
import { MaskedInput } from "./mask-input";
import classes from "./date-input.module.scss";

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
    if ([" ", "/", ":"].includes(ch)) {
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
    { className, format, open, onOpen, onClose, onKeyDown, ...props }: any,
    ref
  ) => {
    const { eventOnBlur: onBlur, onFocus } = props;

    function handleBlur({ target: { name, value } }: any) {
      if (open) return;
      onBlur && onBlur({ target: { name, value } });
    }

    function handleKeyDown(e: any) {
      if (e.key === "ArrowDown") {
        onOpen(true);
      }
      setTimeout(() => {
        onKeyDown && onKeyDown(e);
      }, 100);
    }

    return (
      <Box className={classes.inputWrapper}>
        <MaskedInput
          {...props}
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
        />
        <Box className={classes.inputIcon} onClick={open ? onClose : onOpen}>
          <MaterialIcon icon="calendar_today" variant="outlined" />
        </Box>
      </Box>
    );
  }
);
