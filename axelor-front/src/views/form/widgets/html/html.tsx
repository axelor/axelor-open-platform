import clsx from "clsx";

import { i18n } from "@/services/client/i18n";

import { FieldControl, FieldProps } from "../../builder";
import EditorComponent from "./editor";
import ViewerComponent from "./viewer";

import { useInput } from "../../builder/hooks";
import styles from "./html.module.scss";

export function Html(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { lite } = schema;
  const { text, onChange, onBlur, onKeyDown } = useInput(valueAtom);
  const height = Math.max(100, schema.height);
  return (
    <FieldControl {...props}>
      {readonly && <ViewerComponent value={text} />}
      {readonly || (
        <EditorComponent
          t={i18n.get}
          lite={Boolean(lite)}
          height={height}
          value={text}
          onChange={onChange}
          onBlur={onBlur}
          onKeyDown={onKeyDown}
          className={clsx(styles.container, {
            [styles.invalid]: props.invalid,
          })}
          {...({} as any)}
        />
      )}
    </FieldControl>
  );
}
