import { Box } from "@axelor/ui";
import { useCallback, useEffect, useRef } from "react";
import { useAtomValue } from "jotai";

import { FieldProps } from "../../builder";
import { TagSelect } from "./tag-select";
import { DataRecord } from "@/services/client/data.types";
import { useEditablePopup } from "../text";
import styles from "./tag-select.edit.module.scss";

export function TagSelectEdit(props: FieldProps<DataRecord[]>) {
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
          minHeight: 38,
          width,
          top,
          left,
          zIndex: 1,
        },
      });
    }
  }, [setPopup]);

  const handleBlur = useCallback(() => {
    setPopup(null);
  }, [setPopup]);

  useEffect(() => {
    focus && handleFocus();
  }, [focus, handleFocus]);

  return (
    <Box d="flex" flex={1}>
      <Box
        d={popup ? "none" : "flex"}
        ref={targetRef}
        className={styles.container}
      >
        <TagSelect
          {...props}
          selectProps={{
            className: styles.select,
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
          <Box w={100}>
            <TagSelect
              {...props}
              selectProps={{
                disablePortal: true,
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
