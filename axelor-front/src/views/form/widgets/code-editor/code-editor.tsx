import { useAtom } from "jotai";
import {
  SyntheticEvent,
  memo,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

import * as monaco from "monaco-editor";
import editorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import cssWorker from "monaco-editor/esm/vs/language/css/css.worker?worker";
import htmlWorker from "monaco-editor/esm/vs/language/html/html.worker?worker";
import jsonWorker from "monaco-editor/esm/vs/language/json/json.worker?worker";
import tsWorker from "monaco-editor/esm/vs/language/typescript/ts.worker?worker";

import { Box } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./code-editor.module.scss";

(window as any).MonacoEnvironment = {
  getWorker(_: any, label: string) {
    if (label === "json") {
      return new jsonWorker();
    }
    if (label === "css" || label === "scss" || label === "less") {
      return new cssWorker();
    }
    if (label === "html" || label === "handlebars" || label === "razor") {
      return new htmlWorker();
    }
    if (label === "typescript" || label === "javascript") {
      return new tsWorker();
    }
    return new editorWorker();
  },
};

type ManacoEditor = any;

const SUPPORTED_MODE = ["javascript", "xml", "css"];

const addEvent = (
  editor: ManacoEditor,
  eventName: string,
  cb: (event: SyntheticEvent) => void
) => {
  if (editor && editor[eventName]) {
    const listener = editor[eventName](cb);
    return () => {
      listener.dispose();
    };
  }
};

const Editor = memo(function Editor({
  mode,
  theme,
  readonly,
  value,
  onChange,
  onKeyDown,
  onBlur,
}: {
  mode?: string;
  theme?: string;
  readonly?: boolean;
  value?: string;
  onChange?: (value: string, shouldCallOnChange?: boolean) => void;
  onKeyDown?: (e: SyntheticEvent) => void;
  onBlur?: (value: string) => void;
}) {
  const [container, setContainer] = useState<HTMLElement | null>(null);
  const [editor, setEditor] = useState<ManacoEditor>(null);
  const valueRef = useRef("");

  // set editor theme
  useEffect(() => {
    theme && monaco.editor.setTheme(theme);
  }, [theme]);

  // create editor
  useEffect(() => {
    if (container) {
      const editor = monaco.editor.create(container, {
        value: "",
        language: SUPPORTED_MODE.includes(mode!) ? mode : "markdown",
      });
      setEditor(editor);
      return () => editor && editor.dispose();
    }
  }, [container, mode]);

  // set value in editor
  useEffect(() => {
    if (editor && valueRef.current !== value) {
      editor.setValue((valueRef.current = value!));
    }
  }, [editor, value]);

  // trigger readonly
  useEffect(() => {
    editor && editor.updateOptions({ readOnly: readonly });
  }, [editor, readonly]);

  // trigger onChange on editor changes
  useEffect(() => {
    return addEvent(editor, "onDidChangeModelContent", () => {
      const value = editor.getValue();
      if (valueRef.current !== value) {
        onChange && onChange((valueRef.current = editor.getValue()), false);
      }
    });
  }, [editor, onChange]);

  // trigger onBlur on editor
  useEffect(() => {
    return addEvent(editor, "onDidBlurEditorText", () => {
      onBlur && onBlur(editor.getValue());
    });
  }, [editor, onBlur]);

  // trigger onKeyDown
  useEffect(() => {
    return addEvent(editor, "onKeyDown", (event?: SyntheticEvent) => {
      onKeyDown && onKeyDown((event as any)?.browserEvent);
    });
  }, [editor, onKeyDown]);

  return <Box dir="ltr" ref={setContainer} />;
});

const THEMES: Record<string, string> = {
  dark: "vs-dark",
  light: "vs-light",
};

export function CodeEditor(props: FieldProps<string>) {
  const { schema, readonly, valueAtom } = props;
  const { mode, codeSyntax, codeTheme, height = 400, width = "100%" } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const $mode = mode || codeSyntax;
  const themeType = "light";

  const handleBlur = useCallback(
    (value: string) => {
      setValue(value, true);
    },
    [setValue]
  );

  return (
    <FieldControl {...props}>
      <Box className={styles.container} style={{ height: +height, width }}>
        <Editor
          mode={$mode}
          readonly={readonly}
          value={value || ""}
          theme={codeTheme || THEMES[themeType]}
          onChange={setValue}
          onBlur={handleBlur}
        />
      </Box>
    </FieldControl>
  );
}
