import { useCallback, useMemo } from "react";

import { Select, SelectValue } from "@/components/select";
import { i18n } from "@/services/client/i18n";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { ViewerInput } from "../string/viewer";

type ThemeOption = {
  name: string;
  title: string;
};

export function ThemeSelect(props: FieldProps<string>) {
  const themes = useMemo<ThemeOption[]>(() => {
    return [
      {
        name: "light",
        title: i18n.get("Light"),
      },
      {
        name: "dark",
        title: i18n.get("Dark"),
      },
      {
        name: "auto",
        title: i18n.get("Auto"),
      },
    ];
  }, []);

  const { schema, readonly, valueAtom } = props;
  const { placeholder } = schema;

  const { value, setValue } = useInput(valueAtom, { defaultValue: "", schema });
  const text = themes.find((x) => x.name === value)?.title ?? "";
  const selected = themes.find((x) => x.name === value) ?? null;

  const handleChange = useCallback(
    (value: SelectValue<ThemeOption, false>) => {
      setValue(value?.name ?? null, true);
    },
    [setValue],
  );

  return (
    <FieldControl {...props}>
      {readonly && <ViewerInput value={text} />}
      {readonly || (
        <Select
          autoComplete={false}
          placeholder={placeholder}
          onChange={handleChange}
          value={selected}
          options={themes}
          optionKey={(x) => x.name}
          optionLabel={(x) => x.title}
          optionEqual={(x, y) => x.name === y.name}
        />
      )}
    </FieldControl>
  );
}
