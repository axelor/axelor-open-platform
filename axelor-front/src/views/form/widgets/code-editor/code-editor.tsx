import Editor from "@monaco-editor/react";
import clsx from "clsx";
import { useCallback } from "react";

import { useAppTheme } from "@/hooks/use-app-theme";
import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";

import styles from "./code-editor.module.scss";

export function CodeEditor(props: FieldProps<string>) {
  const { schema, invalid, readonly, valueAtom } = props;
  const { mode, codeSyntax, height = 400, width = "100%" } = schema;

  const language = mode || codeSyntax;
  const appTheme = useAppTheme();

  const theme = appTheme === "dark" ? "vs-dark" : "light";

  const { value, setValue } = useInput(valueAtom, {
    defaultValue: "",
  });

  const handleChange = useCallback(
    (value: string = "") => {
      setValue(value, true);
    },
    [setValue]
  );

  const h = height && /^(\d+)$/.test(height) ? `${height}px` : height;
  const w = width && /^(\d+)$/.test(width) ? `${width}px` : width;

  return (
    <FieldControl
      {...props}
      className={clsx(styles.container, {
        [styles.invalid]: invalid,
        [styles.readonly]: readonly,
      })}
    >
      <Editor
        theme={theme}
        language={language}
        value={value}
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
