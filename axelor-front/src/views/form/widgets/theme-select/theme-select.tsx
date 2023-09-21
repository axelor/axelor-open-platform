import { i18n } from "@/services/client/i18n";
import { Select } from "@axelor/ui";
import { useCallback, useMemo } from "react";
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

  const { value, setValue } = useInput(valueAtom, { defaultValue: "" });
  const text = themes.find((x) => x.name === value)?.title ?? "";
  const selected = themes.find((x) => x.name === value) ?? null;
  const hasValue = value !== null && value !== "";

  const handleChange = useCallback(
    (value: ThemeOption) => {
      setValue(value?.name ?? null, true);
    },
    [setValue]
  );

  const handleClear = useCallback(() => {
    setValue(null, true);
  }, [setValue]);

  return (
    <FieldControl {...props}>
      {readonly && <ViewerInput value={text} />}
      {readonly || (
        <Select
          placeholder={placeholder}
          onChange={handleChange}
          value={selected}
          options={themes}
          optionValue="name"
          optionLabel="title"
          icons={[
            {
              icon: "close",
              hidden: !hasValue,
              onClick: handleClear,
            },
            { icon: "arrow_drop_down" }
          ]}
        />
      )}
    </FieldControl>
  );
}
