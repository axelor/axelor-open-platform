import clsx from "clsx";

import { i18n } from "@/services/client/i18n";

import { FieldControl, FieldProps } from "../../builder";
import EditorComponent from "./editor";
import ViewerComponent from "./viewer";

import { useInput } from "../../builder/hooks";
import { useTranslateModal, useTranslationValue } from "../string/translatable";
import styles from "./html.module.scss";

export function Html(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { lite, translatable } = schema;
  const { text, onChange, onBlur, onKeyDown, setValue } = useInput(valueAtom, {
    schema,
  });
  const height = Math.max(100, schema.height);

  const [trValue, setTranslateValue] = useTranslationValue(props);

  const showTranslationModal = useTranslateModal({
    value: text,
    onValueChange: setValue,
    onUpdate: setTranslateValue,
  });

  return (
    <FieldControl
      {...props}
      className={clsx(styles.container, {
        [styles.translatable]: translatable && !readonly,
      })}
    >
      {readonly ? (
        <ViewerComponent
          className={clsx({
            [styles.viewer]: translatable,
          })}
          value={text}
        />
      ) : (
        <EditorComponent
          t={i18n.get}
          translatable={translatable}
          lite={Boolean(lite)}
          height={height}
          value={text}
          onChange={onChange}
          onBlur={onBlur}
          onKeyDown={onKeyDown}
          onTranslate={showTranslationModal}
          className={clsx(styles.editorContainer, {
            [styles.invalid]: props.invalid,
          })}
          {...({} as any)}
        />
      )}
    </FieldControl>
  );
}
