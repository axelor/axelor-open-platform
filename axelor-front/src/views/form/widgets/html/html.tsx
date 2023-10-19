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
  const { text, onChange, onBlur } = useInput(valueAtom);
  return (
    <FieldControl {...props}>
      {readonly && <ViewerComponent value={text} />}
      {readonly || (
        <EditorComponent
          t={i18n.get}
          lite={Boolean(lite)}
          height={schema.height}
          value={text}
          onChange={onChange}
          onBlur={onBlur}
          className={clsx(styles.container, {
            [styles.invalid]: props.invalid,
          })}
          {...({} as any)}
        />
      )}
    </FieldControl>
  );
}
