import { clsx } from "@axelor/ui";
import { useAtom } from "jotai";
import { useCallback, useId } from "react";

import { FieldControl, FieldProps } from "../../builder";
import { BaseCodeEditor } from "./base-code-editor";

import styles from "./code-editor.module.scss";

export function CodeEditorComponent(props: FieldProps<string>) {
  const { schema, invalid, readonly, valueAtom } = props;
  const { codeSyntax, width = "100%", placeholder, widgetAttrs } = schema;

  const lite = schema?.lite === true || widgetAttrs?.lite === true;
  const autoSize = schema?.autoSize === true || widgetAttrs?.autoSize === true;

  const height = schema.height ?? widgetAttrs?.height ?? 360;

  const [value, setValue] = useAtom(valueAtom);

  const handleChange = useCallback(
    (nextValue = "") => {
      setValue(nextValue, true);
    },
    [setValue],
  );

  const h = height && /^(\d+)$/.test(height) ? `${height}px` : height;
  const w = width && /^(\d+)$/.test(width) ? `${width}px` : width;

  return (
    <BaseCodeEditor
      className={clsx(styles.container, {
        [styles.invalid]: invalid,
        [styles.readonly]: readonly,
      })}
      language={codeSyntax}
      value={value ?? ""}
      placeholder={placeholder}
      width={w}
      height={h}
      lite={lite}
      autoSize={autoSize}
      onChange={handleChange}
      options={{ readOnly: readonly }}
    />
  );
}

export function CodeEditor(props: FieldProps<string>) {
  const id = useId();
  return (
    <FieldControl {...props} inputId={id}>
      <CodeEditorComponent {...props} id={id} data-testid="input" />
    </FieldControl>
  );
}
