import { Switch } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom } from "jotai";

export function BooleanSwitch({
  schema,
  readonly,
  valueAtom,
}: FieldProps<boolean>) {
  const { uid, title } = schema;
  const [value = false, setValue] = useAtom(valueAtom);
  return (
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      <Switch
        id={uid}
        checked={value ?? false}
        readOnly={readonly}
        onChange={() => setValue(!value, true)}
        value=""
      />
    </FieldContainer>
  );
}
