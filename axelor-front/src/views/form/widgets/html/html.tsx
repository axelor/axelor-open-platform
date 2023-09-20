import clsx from "clsx";
import { useAtom } from "jotai";
import { useCallback, useState } from "react";

import { i18n } from "@/services/client/i18n";

import { FieldControl, FieldProps } from "../../builder";
import EditorComponent from "./editor";
import ViewerComponent from "./viewer";

import styles from "./html.module.scss";

export function Html(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { lite } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const [changed, setChanged] = useState(false);

  const handleChange = useCallback(
    (value: string) => {
      setChanged(true);
      setValue(value);
    },
    [setValue]
  );

  const handleBlur = useCallback(
    (value: string) => {
      if (changed) {
        setChanged(false);
        setValue(value, true);
      }
    },
    [changed, setValue]
  );

  return (
    <FieldControl {...props}>
      {readonly && <ViewerComponent value={value || ""} />}
      {readonly || (
        <EditorComponent
          t={i18n.get}
          lite={Boolean(lite)}
          height={schema.height}
          value={value || ""}
          onChange={handleChange}
          onBlur={handleBlur}
          className={clsx(styles.container, {
            [styles.invalid]: props.invalid,
          })}
          {...({} as any)}
        />
      )}
    </FieldControl>
  );
}
