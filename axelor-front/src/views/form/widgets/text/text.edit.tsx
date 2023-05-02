import { Box } from "@axelor/ui";
import { FocusEvent, useCallback, useRef, useState } from "react";

import { FieldProps } from "../../builder";
import { String } from "../string";
import { Text } from "./text";

export function TextEdit(props: FieldProps<string>) {
  const [popup, setPopup] = useState<any>(null);
  const targetRef = useRef<HTMLDivElement>(null);

  const handleFocus = useCallback((e: FocusEvent<HTMLInputElement>) => {
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
