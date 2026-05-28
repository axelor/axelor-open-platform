import Editor, { EditorProps, loader } from "@monaco-editor/react";
import type * as monaco from "monaco-editor";
import { useCallback, useEffect, useRef } from "react";

import { useAppSettings } from "@/hooks/use-app-settings";

const monacoPath = import.meta.env.MONACO_PATH;

if (monacoPath) {
  const baseUrl = location.origin + location.pathname.replace(/\/$/, "");
  loader.config({ paths: { vs: `${baseUrl}/${monacoPath}` } });
}

export interface BaseCodeEditorProps extends EditorProps {
  lite?: boolean;
  autoSize?: boolean;
  placeholder?: string;
  onBlur?: (value: string | undefined) => void;
}

const MIN_HEIGHT = 36;
const MAX_HEIGHT_AUTO = 360;

const baseOptions: monaco.editor.IStandaloneEditorConstructionOptions = {
  minimap: { enabled: false },
  stickyScroll: { enabled: false },
  tabSize: 2,
  insertSpaces: true,
  padding: { top: 6, bottom: 6 },
  scrollbar: {
    horizontalScrollbarSize: 6,
    verticalScrollbarSize: 6,
    alwaysConsumeMouseWheel: false,
  },
};

const liteOptions: monaco.editor.IStandaloneEditorConstructionOptions = {
  ...baseOptions,
  lineNumbers: "off",
  folding: false,
  renderLineHighlight: "none",
  quickSuggestions: false,
  parameterHints: { enabled: false },
  inlineSuggest: { enabled: false },
  suggestOnTriggerCharacters: false,
  acceptSuggestionOnEnter: "off",
  acceptSuggestionOnCommitCharacter: false,
  snippetSuggestions: "none",
  wordBasedSuggestions: "off",
  hover: { enabled: false },
  contextmenu: false,
  overviewRulerLanes: 0,
  scrollBeyondLastLine: false,
};

export function BaseCodeEditor(props: BaseCodeEditorProps) {
  const {
    options,
    onMount,
    onBlur,
    height: maxHeight = "100%",
    placeholder,
    autoSize: autoSizeProp,
    lite = false,
    ...rest
  } = props;

  const { themeMode } = useAppSettings();
  const theme = themeMode === "dark" ? "vs-dark" : "light";

  const onBlurRef = useRef(onBlur);
  const containerRef = useRef<HTMLDivElement>(null);
  const parsedHeight =
    typeof maxHeight === "number"
      ? maxHeight
      : typeof maxHeight === "string" && maxHeight.endsWith("px")
        ? parseInt(maxHeight)
        : 0;
  const maxHeightPx =
    autoSizeProp && parsedHeight <= 0
      ? MAX_HEIGHT_AUTO
      : parsedHeight > 0 && parsedHeight < MIN_HEIGHT
        ? MIN_HEIGHT
        : parsedHeight;

  const autoSize = autoSizeProp === true && maxHeightPx > 0;

  const opts = lite
    ? { ...liteOptions, ...options }
    : { ...baseOptions, ...options };

  useEffect(() => {
    onBlurRef.current = onBlur;
  }, [onBlur]);

  const handleMount = useCallback(
    (
      editor: monaco.editor.IStandaloneCodeEditor,
      monacoInstance: typeof monaco,
    ) => {
      // disable auto-completion trigger
      editor.addCommand(
        monacoInstance.KeyMod.CtrlCmd | monacoInstance.KeyCode.Space,
        () => {},
      );

      editor.onDidBlurEditorText(() => {
        onBlurRef.current?.(editor.getValue());
      });

      // auto-size: resize DOM directly to avoid React re-render flash
      if (autoSize && containerRef.current) {
        const container = containerRef.current;
        const paddingTop = opts.padding?.top ?? 0;
        const paddingBottom = opts.padding?.bottom ?? 0;
        const update = () => {
          const contentHeight =
            editor.getContentHeight() + paddingTop + paddingBottom;
          const overflow = contentHeight > maxHeightPx;
          const height = Math.max(
            MIN_HEIGHT,
            Math.min(contentHeight, maxHeightPx),
          );
          container.style.height = `${height}px`;
          editor.updateOptions({
            scrollbar: {
              vertical: overflow ? "auto" : "hidden",
            },
          });
          editor.layout();
        };
        editor.onDidContentSizeChange(update);
        update();
      }

      onMount?.(editor, monacoInstance);
    },
    [autoSize, onMount, opts.padding?.top, opts.padding?.bottom, maxHeightPx],
  );

  return (
    <div
      ref={containerRef}
      style={autoSize ? { height: MIN_HEIGHT } : undefined}
    >
      <Editor
        theme={theme}
        onMount={handleMount}
        options={{ ...opts, placeholder }}
        height={autoSize ? "100%" : maxHeight}
        {...rest}
      />
    </div>
  );
}
