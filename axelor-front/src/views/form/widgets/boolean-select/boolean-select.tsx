import { Input, Select } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";
import { useCallback, useMemo } from "react";
import { i18n } from "@/services/client/i18n";

type SelectOption = {
  title: string;
  value: boolean | null;
};

export function BooleanSelect({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<boolean | null>) {
  const { uid, showTitle = true, widgetAttrs, nullable } = schema;
  const {
    nullText = " ",
    falseText = i18n.get("No"),
    trueText = i18n.get("Yes"),
  } = widgetAttrs || {};
  const [value = false, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
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
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
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
    </FieldContainer>
  );
}
