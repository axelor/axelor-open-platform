import { useCallback, useMemo, useRef, useState } from "react";

import {
  Alert,
  Box,
  Button,
  DialogHeader,
  DialogTitle,
  useControlled,
} from "@axelor/ui";
import { ThemeOptions } from "@axelor/ui/core/styles/theme/types";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { compactTheme } from "@/components/theme-builder/utils.ts";
import { DataSource } from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { useDialogContext } from "../dialogs";
import { PropertiesContextProvider } from "./scope";
import { ThemeDesigner } from "./theme-editor";

import styles from "./theme-builder.module.scss";

export type ThemeBuilderProps = {
  data?: DataRecord;
};

export function ThemeBuilder(props: ThemeBuilderProps) {
  const { data } = props;

  const [theme, error] = useMemo(() => {
    if (data?.content) {
      try {
        return [JSON.parse(data.content)];
      } catch (err) {
        return [{}, err];
      }
    }
    return [];
  }, [data?.content]);

  if (theme) {
    return <ThemeBuilderInner {...props} theme={theme} error={error} />;
  }
}

function ThemeBuilderInner(
  props: ThemeBuilderProps & { theme: ThemeOptions; error?: any },
) {
  const { error, data } = props;
  const { close } = useDialogContext();

  const [themeDiv, setThemeDiv] = useState<HTMLDivElement | null>(null);
  const [invalidProps, setInvalidProps] = useState<Record<string, boolean>>({});
  const [theme, setTheme] = useControlled({
    name: "ThemeBuilder",
    prop: "theme",
    state: undefined,
    defaultState: props.theme,
  });
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

  const handleClose = useCallback(() => close(false), [close]);
  const handleSave = useCallback(async () => {
    const ds = new DataSource(data?.model);
    await ds.save({
      ...data,
      content: JSON.stringify(compactTheme(theme), null, 2) || null,
    });
    close(true);
    location.reload();
  }, [close, data, theme]);

  const handleChange = useCallback(
    (newTheme: ThemeOptions) => setTheme(newTheme),
    [setTheme],
  );

  const invalid = useMemo(
    () => Object.values(invalidProps).some((v) => v),
    [invalidProps],
  );

  const themeMode = useMemo(() => theme?.palette?.mode, [theme]);

  return (
    <div className={styles.builder}>
      <Box d="none" ref={setThemeDiv} data-bs-theme={themeMode} />
      <DialogHeader className={styles.header}>
        <DialogTitle className={styles.title}>Theme Builder</DialogTitle>
        <div className={styles.buttons}>
          <Button variant="primary" onClick={handleSave} disabled={invalid}>
            {i18n.get("Save")}
          </Button>
          <MaterialIcon
            icon="close"
            className={styles.close}
            onClick={handleClose}
          />
        </div>
      </DialogHeader>
      {error && (
        <Alert m={2} p={2} variant="danger">
          {i18n.get(
            "Failed to parse the given JSON theme : incorrect syntax was encountered.",
          )}
        </Alert>
      )}
      <PropertiesContextProvider
        getCssVar={getCssVar}
        invalids={invalidProps}
        setInvalids={setInvalidProps}
      >
        {theme && <ThemeDesigner theme={theme} onChange={handleChange} />}
      </PropertiesContextProvider>
    </div>
  );
}
