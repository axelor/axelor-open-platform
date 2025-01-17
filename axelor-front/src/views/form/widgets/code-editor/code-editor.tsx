import Editor, { loader } from "@monaco-editor/react";
import { clsx, useTheme } from "@axelor/ui";
import { useAtom } from "jotai";
import { useCallback } from "react";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./code-editor.module.scss";

loader.config({ paths: { vs: "js/libs/monaco-editor/vs" } });

export function CodeEditorComponent(props: FieldProps<string>) {
  const { schema, invalid, readonly, valueAtom } = props;
  const { codeSyntax, width = "100%" } = schema;
  const height = schema.height ?? schema.widgetAttrs?.height ?? 400;

  const themeMode = useTheme().mode;

  const theme = themeMode === "dark" ? "vs-dark" : "light";

  const [value, setValue] = useAtom(valueAtom);

  const handleChange = useCallback(
    (value = "") => {
      setValue(value, true);
    },
    [setValue],
  );

  const h = height && /^(\d+)$/.test(height) ? `${height}px` : height;
  const w = width && /^(\d+)$/.test(width) ? `${width}px` : width;

  return (
    <Editor
      className={clsx(styles.container, {
        [styles.invalid]: invalid,
        [styles.readonly]: readonly,
      })}
      theme={theme}
      language={codeSyntax}
      value={value ?? ""}
      width={w}
      height={h}
      onChange={handleChange}
      options={{
        readOnly: readonly,
      }}
    />
  );
}

export function CodeEditor(props: FieldProps<string>) {
  return (
    <FieldControl {...props}>
      <CodeEditorComponent {...props} />
    </FieldControl>
  );
}
