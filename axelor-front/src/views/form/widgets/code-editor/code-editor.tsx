import Editor, { loader } from "@monaco-editor/react";
import { clsx } from "@axelor/ui";
import { useAtom } from "jotai";
import { useCallback, useId } from "react";

import { FieldControl, FieldProps } from "../../builder";
import { useAppSettings } from "@/hooks/use-app-settings";

import styles from "./code-editor.module.scss";

const monacoPath = import.meta.env.MONACO_PATH;

if (monacoPath) {
  const baseUrl = location.origin + location.pathname.replace(/\/$/, "");
  loader.config({ paths: { vs: `${baseUrl}/${monacoPath}` } });
}

export function CodeEditorComponent(props: FieldProps<string>) {
  const { schema, invalid, readonly, valueAtom } = props;
  const { codeSyntax, width = "100%" } = schema;
  const height = schema.height ?? schema.widgetAttrs?.height ?? 400;

  const { themeMode } = useAppSettings();

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
  const id = useId();
  
  return (
    <FieldControl {...props} inputId={id}>
      <CodeEditorComponent {...props} id={id} data-testid="input" />
    </FieldControl>
  );
}
