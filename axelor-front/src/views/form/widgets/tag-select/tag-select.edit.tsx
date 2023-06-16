import { Box } from "@axelor/ui";
import { useCallback, useEffect, useRef, useState } from "react";
import { useAtomValue } from "jotai";

import { FieldProps } from "../../builder";
import { TagSelect } from "./tag-select";
import { DataRecord } from "@/services/client/data.types";
import styles from "./tag-select.edit.module.scss";

export function TagSelectEdit(props: FieldProps<DataRecord[]>) {
  const { widgetAtom, schema } = props;
  const [popup, setPopup] = useState<any>(null);
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
          top,
          left: left - 8,
          zIndex: 1,
        },
      });
    }
  }, []);

  const handleBlur = useCallback(() => {
    setPopup(null);
  }, []);

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
          d="flex"
          bgColor="body"
          style={{ ...popup?.style }}
          shadow
          data-column-index={schema.editIndex}
        >
          <Box>
            <TagSelect
              {...props}
              selectProps={{
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
