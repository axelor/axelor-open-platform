import { useId } from "react";
import { clsx } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";
import EditorComponent from "./editor";
import ViewerComponent from "./viewer";

import { useInput } from "../../builder/hooks";
import { Translatable, useTranslateModal, useTranslationValue } from "../string/translatable";
import styles from "./html.module.scss";

export function Html(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  
  const id = useId();
  const { lite, translatable, placeholder } = schema;
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
      inputId={id}
      className={clsx(styles.container, {
        [styles.translatable]: translatable && !readonly,
      })}
    >
      {readonly || trValue ? (
        <>
          <ViewerComponent
            className={styles.viewer}
            value={trValue ?? text}
          />
          {translatable && !readonly && (
            <Translatable
              className={styles.translate}
              value={text}
              onValueChange={setValue}
              onUpdate={setTranslateValue}
            />
          )}
        </>
      ) : (
        <EditorComponent
          id={id}
          data-testid="input"
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
          placeholder={placeholder}
          {...({} as any)}
        />
      )}
    </FieldControl>
  );
}
