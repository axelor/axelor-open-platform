import { Switch } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";

export function BooleanSwitch({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<boolean>) {
  const { uid, showTitle = true } = schema;
  const [value = false, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <Switch
        data-input
        id={uid}
        checked={value ?? false}
        readOnly={readonly}
        onChange={() => setValue(!value, true)}
        value=""
      />
    </FieldContainer>
  );
}
