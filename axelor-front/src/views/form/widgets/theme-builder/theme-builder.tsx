import { useAtom, useAtomValue, useSetAtom } from "jotai";
import { useCallback, useId, useMemo, useState } from "react";

import { Box, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { ThemeBuilder as ThemeBuilderComponent } from "@/components/theme-builder";
import { i18n } from "@/services/client/i18n";
import { focusAtom } from "@/utils/atoms";
import { FieldControl, FieldProps } from "../../builder";
import { CodeEditorComponent } from "../code-editor";

export function ThemeBuilder(props: FieldProps<string>) {
  const { readonly, schema, valueAtom, widgetAtom } = props;
  const id = useId();

  const [jsonMode, setJsonMode] = useState<boolean>(false);
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);

  const setInvalid = useSetAtom(
    useMemo(
      () =>
        focusAtom(
          widgetAtom,
          (w) => w.errors?.invalid,
          (w, v) => ({
            ...w,
            errors: v ? { ...w.errors, invalid: v } : undefined,
          }),
        ),
      [widgetAtom],
    ),
  );

  const handleInvalid = useCallback(
    (invalid: boolean) => {
      setInvalid(invalid ? i18n.get("{0} is invalid", title) : "");
    },
    [title, setInvalid],
  );

  const handleChange = useCallback(
    (value: string | null) => setValue(value, true),
    [setValue],
  );

  const codeEditorSchema = useMemo(
    () => ({
      ...schema,
      codeSyntax: "json",
    }),
    [schema],
  );

  return (
    <FieldControl {...props} inputId={id}>
      <Box px={2} d="flex" alignItems="center" justifyContent="flex-end">
        <Button
          variant="link"
          d={"inline-flex"}
          gap={3}
          data-testid="btn-toggle"
          onClick={() => setJsonMode(!jsonMode)}
        >
          {jsonMode ? (
            <>
              <MaterialIcon icon="design_services" />
              {i18n.get("Back to editor")}
            </>
          ) : (
            <>
              <MaterialIcon icon="data_object" />
              {i18n.get("Show json content")}
            </>
          )}
        </Button>
      </Box>

      {/* always render theme builder to validate json content */}
      <ThemeBuilderComponent
        readonly={readonly}
        hidden={jsonMode}
        value={value}
        onChange={handleChange}
        onInvalid={handleInvalid}
      />

      {jsonMode && <CodeEditorComponent {...props} schema={codeEditorSchema} />}
    </FieldControl>
  );
}
