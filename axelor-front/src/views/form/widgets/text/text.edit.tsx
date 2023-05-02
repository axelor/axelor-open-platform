import { Box } from "@axelor/ui";
import { FocusEvent, useCallback, useEffect, useRef, useState } from "react";
import { useAtomValue } from "jotai";

import { FieldProps } from "../../builder";
import { String } from "../string";
import { Text } from "./text";

export function TextEdit(props: FieldProps<string>) {
  const { widgetAtom } = props;
  const [popup, setPopup] = useState<any>(null);
  const targetRef = useRef<HTMLDivElement>(null);

  const { attrs: { focus } } = useAtomValue(widgetAtom);

  const handleFocus = useCallback(() => {
    const target = targetRef.current;
    if (target) {
      const { top, left, width } = target.getBoundingClientRect();
      setPopup({
        style: {
          position: "fixed",
          minHeight: 100,
          width,
          top: top - 10,
          left: left - 8,
          zIndex: 1,
        },
      });
    }
  }, []);

  const handleBlur = useCallback((e: FocusEvent<HTMLTextAreaElement>) => {
    setPopup(null);
  }, []);

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
        <Box d="flex" bgColor="body" style={{ ...popup?.style }} shadow>
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
