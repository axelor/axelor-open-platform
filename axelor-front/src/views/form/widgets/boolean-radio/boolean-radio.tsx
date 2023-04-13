import { Box, Input } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";
import { i18n } from "@/services/client/i18n";

export function BooleanRadio({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<boolean | null>) {
  const {
    uid,
    name,
    widgetAttrs,
    nullable,
    direction,
    showTitle = true,
  } = schema;
  const { falseText = i18n.get("No"), trueText = i18n.get("Yes") } =
    widgetAttrs || {};
  const [value = false, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);

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
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <Box
        d="flex"
        flexDirection={direction === "vertical" ? "column" : "row"}
        className="input"
      >
        {renderRadio(true, trueText)}
        {renderRadio(false, falseText)}
      </Box>
    </FieldContainer>
  );
}
