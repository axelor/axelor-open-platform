import React, { FocusEvent, KeyboardEvent, forwardRef, useEffect, useRef } from "react";
import ReactTextMask, { MaskedInputProps } from "react-text-mask";
import { Input, InputProps } from "@axelor/ui";

function moveCaretToStart(el: HTMLInputElement) {
  const { value } = el;
  if (typeof el.selectionStart == "number") {
    const ind = value.indexOf("_");
    if (ind > -1) {
      el.selectionStart = el.selectionEnd = ind;
    }
  } else if (typeof (el as any).createTextRange != "undefined") {
    el.focus();
    const range = (el as any).createTextRange();
    range.collapse(true);
    range.select();
  }
}

const PLACEHOLDER = "_";

export const MaskedInput = forwardRef<any, MaskedInputProps & InputProps>(
  (props, ref) => {
    const [showMask, setShowMask] = React.useState(false);
    const frameRef = useRef<number>(0);
    const { onFocus, onKeyDown, onBlur, onChange } = props;

    useEffect(() => {
      return () => cancelAnimationFrame(frameRef.current);
    }, []);

    function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
      if (["Delete", "Backspace"].includes(e.key)) {
        const el = e.target as HTMLInputElement;
        if (
          el &&
          el.selectionStart === 0 &&
          el.selectionEnd === (el.value || "").length
        ) {
          el.value = "";
          onChange?.(e as any);
        }
      }
      onKeyDown?.(e);
    }

    function handleFocus(event: FocusEvent<HTMLInputElement>) {
      const inputEl = event.currentTarget;
      setShowMask(true);
      frameRef.current = requestAnimationFrame(function () {
        moveCaretToStart(inputEl);
      });
      onFocus?.(event);
    }

    function handleBlur(event: any) {
      setShowMask(false);
      onBlur?.(event);
    }

    return (
      <ReactTextMask
        showMask={showMask}
        placeholderChar={PLACEHOLDER}
        keepCharPositions
        guide
        ref={ref}
        {...props}
        onKeyDown={handleKeyDown}
        onFocus={handleFocus}
        onBlur={handleBlur}
        render={(ref, props) => <Input ref={ref} {...(props as any)} />}
      />
    );
  },
);
