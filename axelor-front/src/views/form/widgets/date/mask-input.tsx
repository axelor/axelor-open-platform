import React, { FocusEvent, KeyboardEvent, forwardRef } from "react";
import ReactTextMask, { MaskedInputProps } from "react-text-mask";
import { Input, InputProps } from "@axelor/ui";

function moveCaretToStart(el: HTMLInputElement) {
  const { value } = el;
  if (typeof el.selectionStart == "number") {
    const ind = value.indexOf("_");
    ind > -1 && (el.selectionStart = el.selectionEnd = ind);
  } else if (typeof (el as any).createTextRange != "undefined") {
    el.focus();
    var range = (el as any).createTextRange();
    range.collapse(true);
    range.select();
  }
}

export const MaskedInput = forwardRef<any, MaskedInputProps & InputProps>(
  (props, ref) => {
    const [showMask, setShowMask] = React.useState(false);
    const { onFocus, onKeyDown, onBlur, onChange } = props;

    function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
      if (["Delete", "Backspace"].includes(e.key)) {
        const el = e.target as HTMLInputElement;
        if (
          el &&
          el.selectionStart === 0 &&
          el.selectionEnd === (el.value || "").length
        ) {
          el.value = "";
          onChange && onChange(e as any);
        }
      }
      onKeyDown && onKeyDown(e);
    }

    function handleFocus(event: FocusEvent<HTMLInputElement>) {
      const inputEl = event.currentTarget;
      setShowMask(true);
      setTimeout(function () {
        moveCaretToStart(inputEl);
      }, 10);
      onFocus && onFocus(event);
    }

    function handleBlur(event: any) {
      setShowMask(false);
      onBlur && onBlur(event);
    }

    return (
      <ReactTextMask
        showMask={showMask}
        placeholderChar={"_"}
        guide
        ref={ref}
        {...props}
        onKeyDown={handleKeyDown}
        onFocus={handleFocus}
        onBlur={handleBlur}
        render={(ref, props) => <Input ref={ref} {...(props as any)} />}
      />
    );
  }
);
