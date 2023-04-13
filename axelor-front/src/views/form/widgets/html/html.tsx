import clsx from "clsx";
import { useAtom, useAtomValue } from "jotai";
import { useCallback, useState } from "react";

import { i18n } from "@/services/client/i18n";

import { FieldContainer, FieldProps } from "../../builder";
import EditorComponent from "./editor";
import ViewerComponent from "./viewer";

import styles from "./html.module.scss";

export function Html({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<string>) {
  const { uid, showTitle = true, lite } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);
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
    <FieldContainer
      className={clsx(styles.container, {
        [styles.readonly]: readonly,
      })}
      readonly={readonly}
    >
      {showTitle && <label htmlFor={uid}>{title}</label>}
      {readonly ? (
        <ViewerComponent value={value || ""} />
      ) : (
        <EditorComponent
          t={i18n.get}
          lite={Boolean(lite)}
          value={value || ""}
          onChange={handleChange}
          onBlur={handleBlur}
          {...({} as any)}
        />
      )}
    </FieldContainer>
  );
}
