import Editor from "@monaco-editor/react";
import clsx from "clsx";
import { useAtom } from "jotai";
import { useCallback } from "react";

import { useAppTheme } from "@/hooks/use-app-theme";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./code-editor.module.scss";

export function CodeEditor(props: FieldProps<string>) {
  const { schema, invalid, readonly, valueAtom } = props;
  const { codeSyntax, width = "100%" } = schema;
  const height = schema.height ?? schema.widgetAttrs?.height ?? 400;

  const appTheme = useAppTheme();

  const theme = appTheme === "dark" ? "vs-dark" : "light";

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
    <FieldControl {...props}>
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
    </FieldControl>
  );
}
