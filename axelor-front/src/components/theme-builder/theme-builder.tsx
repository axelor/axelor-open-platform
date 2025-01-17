import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import isEqual from "lodash/isEqual";

import { Alert, Box, clsx } from "@axelor/ui";
import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";

import { compactTheme } from "@/components/theme-builder/utils.ts";
import { i18n } from "@/services/client/i18n";
import { PropertiesContextProvider } from "./scope";
import { ThemeDesigner } from "./theme-editor";

import styles from "./theme-builder.module.scss";

export type ThemeBuilderProps = {
  readonly?: boolean;
  hidden?: boolean;
  value?: string | null;
  onChange?: (value: string | null) => void;
  onInvalid?: (invalid: boolean) => void;
};

export function ThemeBuilder(props: ThemeBuilderProps) {
  const { value } = props;

  const [theme, error] = useMemo(() => {
    try {
      return [JSON.parse(value || "{}") || {}];
    } catch (err) {
      return [{}, err];
    }
  }, [value]);

  return <ThemeBuilderInner {...props} theme={theme} error={error} />;
}

function ThemeBuilderInner(
  props: ThemeBuilderProps & { theme: ThemeOptions; error?: unknown },
) {
  const { readonly = false, hidden, error, onChange, onInvalid } = props;

  const [themeDiv, setThemeDiv] = useState<HTMLDivElement | null>(null);
  const [invalidProps, setInvalidProps] = useState<Record<string, boolean>>({});
  const [theme, setTheme] = useState<ThemeOptions>(props.theme);
  const styleRef = useRef<CSSStyleDeclaration>();

  const getCssVar = useCallback(
    (name: string) => {
      if (themeDiv) {
        const style =
          styleRef.current ??
          (styleRef.current = window.getComputedStyle(themeDiv));
        return style.getPropertyValue(name);
      }
    },
    [themeDiv],
  );

  // sync updated theme
  useEffect(() => {
    setTheme((currTheme) =>
      isEqual(currTheme, props.theme) ? currTheme : props.theme,
    );
  }, [props.theme]);

  const handleChange = useCallback(
    (newTheme: ThemeOptions) => {
      const json = JSON.stringify(compactTheme(newTheme), null, 2) || null;
      onChange?.(json);
      setTheme(newTheme);
    },
    [setTheme, onChange],
  );

  const invalid = useMemo(
    () => Object.values(invalidProps).some((v) => v),
    [invalidProps],
  );

  useEffect(() => {
    onInvalid?.(invalid);
  }, [invalid, onInvalid]);

  const themeMode = useMemo(() => theme?.palette?.mode, [theme]);

  return (
    <div
      className={clsx(styles.builder, {
        [styles.hidden]: hidden,
      })}
    >
      <Box d="none" ref={setThemeDiv} data-bs-theme={themeMode} />
      {error ? (
        <Alert m={2} p={2} variant="danger">
          {i18n.get(
            "Failed to parse the given JSON theme : incorrect syntax was encountered.",
          )}
        </Alert>
      ) : (
        <PropertiesContextProvider
          getCssVar={getCssVar}
          readonly={readonly}
          invalids={invalidProps}
          setInvalids={setInvalidProps}
        >
          {theme && <ThemeDesigner theme={theme} onChange={handleChange} />}
        </PropertiesContextProvider>
      )}
    </div>
  );
}
