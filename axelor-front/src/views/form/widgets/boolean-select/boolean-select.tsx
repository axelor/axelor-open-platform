import { useAtom, useAtomValue } from "jotai";
import { useCallback, useMemo } from "react";

import { Input, Select } from "@axelor/ui";

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
    nullText = " ",
    falseText = i18n.get("No"),
    trueText = i18n.get("Yes"),
  } = widgetAttrs || {};
  const [value = nullable ? null : false, setValue] = useAtom(valueAtom);
  const {
    attrs: { focus },
  } = useAtomValue(widgetAtom);

  const handleOnChange = useCallback(
    (option: SelectOption) => {
      setValue(nullable && option.value === null ? null : option.value, true);
    },
    [nullable, setValue]
  );

  const options = useMemo<SelectOption[]>(
    () => [
      ...(nullable ? [{ title: i18n.get(nullText), value: null }] : []),
      { title: i18n.get(trueText), value: true },
      { title: i18n.get(falseText), value: false },
    ],
    [nullable, nullText, trueText, falseText]
  );

  const selected = options.find((option) => option.value === value);

  return (
    <FieldControl {...props}>
      {readonly ? (
        <Input
          type="text"
          defaultValue={(selected?.value === null ? "" : selected?.title) || ""}
          disabled
          readOnly
          bg="body"
          border={false}
        />
      ) : (
        <Select
          {...(focus && { key: "focused" })}
          autoFocus={focus}
          invalid={invalid}
          value={selected}
          onChange={handleOnChange}
          options={options}
          optionLabel="title"
          optionValue="value"
          isSearchable={false}
          isClearable={false}
          isClearOnDelete={false}
        />
      )}
    </FieldControl>
  );
}
