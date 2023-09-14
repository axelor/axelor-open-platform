import { useAtom, useAtomValue } from "jotai";
import { useCallback, useMemo } from "react";

import { Select, SelectValue } from "@/components/select";
import { i18n } from "@/services/client/i18n";

import { FieldControl, FieldProps } from "../../builder";

type SelectOption = {
  title: string;
  value: boolean | null;
};

export function BooleanSelect(props: FieldProps<boolean | null>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { widgetAttrs, nullable } = schema;
  const {
    nullText = "\u00A0",
    falseText = i18n.get("No"),
    trueText = i18n.get("Yes"),
  } = widgetAttrs || {};

  const [value = nullable ? null : undefined, setValue] = useAtom(valueAtom);

  const {
    attrs: { focus, required },
  } = useAtomValue(widgetAtom);

  const handleOnChange = useCallback(
    (option: SelectValue<SelectOption, false>) => {
      setValue(nullable && option?.value === null ? null : option?.value, true);
    },
    [nullable, setValue],
  );

  const options = useMemo<SelectOption[]>(
    () => [
      ...(nullable ? [{ title: i18n.get(nullText), value: null }] : []),
      { title: i18n.get(trueText), value: true },
      { title: i18n.get(falseText), value: false },
    ],
    [nullable, nullText, trueText, falseText],
  );

  const selected = options.find((option) => option.value === value);

  return (
    <FieldControl {...props}>
      <Select
        autoComplete={false}
        autoFocus={focus}
        required={required}
        readOnly={readonly}
        invalid={invalid}
        value={selected}
        onChange={handleOnChange}
        options={options}
        optionKey={(x) => x.title}
        optionLabel={(x) => x.title}
        optionEqual={(x, y) => x.value === y.value}
        clearIcon={false}
      />
    </FieldControl>
  );
}
