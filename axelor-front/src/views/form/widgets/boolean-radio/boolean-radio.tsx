import { useAtom } from "jotai";

import { Box, Input } from "@axelor/ui";

import { i18n } from "@/services/client/i18n";

import { FieldControl, FieldProps } from "../../builder";

export function BooleanRadio(props: FieldProps<boolean | null>) {
  const { schema, readonly, valueAtom } = props;
  const { name, widgetAttrs, nullable, direction } = schema;
  const { falseText = /*$$(*/ "No" /*)*/, trueText = /*$$(*/ "Yes" /*)*/ } =
    widgetAttrs || {};
  const [value = nullable ? null : undefined, setValue] = useAtom(valueAtom);

  function renderRadio($value: boolean, label: string) {
    const checked = value === $value;
    return (
      <Box d="flex" alignItems="center">
        <Input
          data-input
          m={0}
          name={name}
          type="radio"
          value={`${$value}`}
          checked={checked}
          disabled={readonly}
          onChange={() => {}}
          onClick={() => setValue(nullable && checked ? null : $value, true)}
        />
        <Box as="span" d="inline-block" ms={1} me={3}>
          {i18n.get(label)}
        </Box>
      </Box>
    );
  }

  return (
    <FieldControl {...props}>
      <Box
        d="flex"
        flexDirection={direction === "vertical" ? "column" : "row"}
        className={"input"}
      >
        {renderRadio(true, trueText)}
        {renderRadio(false, falseText)}
      </Box>
    </FieldControl>
  );
}
