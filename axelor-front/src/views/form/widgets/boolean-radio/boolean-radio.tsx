import { Box, Input } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom } from "jotai";
import { i18n } from "@/services/client/i18n";

export function BooleanRadio({
  schema,
  readonly,
  valueAtom,
}: FieldProps<boolean | null>) {
  const { uid, name, title, widgetAttrs, nullable, direction } = schema;
  const { falseText = i18n.get("No"), trueText = i18n.get("Yes") } =
    widgetAttrs || {};
  const [value = false, setValue] = useAtom(valueAtom);

  function renderRadio($value: boolean, label: string) {
    const checked = value === $value;
    return (
      <Box d="flex" alignItems="center">
        <Input
          m={0}
          name={name}
          type="radio"
          value={`${$value}`}
          checked={checked}
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
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      <Box
        d="flex"
        flexDirection={direction === 'vertical' ? "column" : "row"}
        className="input"
      >
        {renderRadio(true, trueText)}
        {renderRadio(false, falseText)}
      </Box>
    </FieldContainer>
  );
}
