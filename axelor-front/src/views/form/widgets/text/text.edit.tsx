import { Box } from "@axelor/ui";
import { FocusEvent, useCallback, useEffect, useRef, useState } from "react";
import { useAtomValue } from "jotai";

import { FieldProps } from "../../builder";
import { String } from "../string";
import { Text } from "./text";

export function useEditablePopup() {
  const [popup, setPopup] = useState<any>(null);
  const popupRef = useRef<any>(null);

  const hasPopup = Boolean(popup);

  useEffect(() => {
    if (hasPopup) {
      const handler = (e: any) => {
        const popupEl = popupRef.current;
        if (
          popupEl &&
          (e?.target === popupEl || popupEl?.contains?.(e?.target))
        ) {
          return;
        }
        setPopup(null);
      };
      const wheelEvent =
        "onwheel" in document.createElement("div") ? "wheel" : "mousewheel";
      window.addEventListener("DOMMouseScroll", handler, false); // older FF
      window.addEventListener(wheelEvent, handler, { passive: false }); // modern desktop

      return () => {
        window.removeEventListener("DOMMouseScroll", handler);
        window.removeEventListener(wheelEvent, handler);
      };
    }
  }, [hasPopup]);

  return [popup, setPopup, popupRef] as const;
}

export function TextEdit(props: FieldProps<string>) {
  const { widgetAtom, schema } = props;
  const [popup, setPopup, popupRef] = useEditablePopup();
  const targetRef = useRef<HTMLDivElement>(null);

  const {
    attrs: { focus },
  } = useAtomValue(widgetAtom);

  const handleFocus = useCallback(() => {
    const target = targetRef.current;
    if (target) {
      const { top, left, width } = target.getBoundingClientRect();
      setPopup({
        style: {
          position: "fixed",
          minHeight: 100,
          width,
          top: top,
          left: left,
          zIndex: 1,
        },
      });
    }
  }, [setPopup]);

  const handleBlur = useCallback(
    (e: FocusEvent<HTMLTextAreaElement>) => {
      setPopup(null);
    },
    [setPopup]
  );

  useEffect(() => {
    focus && handleFocus();
  }, [focus, handleFocus]);

  return (
    <Box d="flex" flex={1}>
      <Box d={popup ? "none" : "flex"} ref={targetRef}>
        <String
          {...props}
          inputProps={{
            onFocus: handleFocus,
          }}
        />
      </Box>
      {popup && (
        <Box
          ref={popupRef}
          d="flex"
          bgColor="body"
          style={{ ...popup?.style }}
          shadow
          data-column-index={schema.editIndex}
        >
          <Box>
            <Text
              {...props}
              inputProps={{
                autoFocus: true,
                onBlur: handleBlur,
              }}
            />
          </Box>
        </Box>
      )}
    </Box>
  );
}
