import { Input } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { FieldContainer, FieldProps } from "../../builder";

export function String({
  schema,
  readonly,
  formAtom,
  widgetAtom,
  valueAtom,
}: FieldProps<string>) {
  const { uid, title } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { required } = attrs;

  const [value = "", setValue] = useAtom(valueAtom);

  return (
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      {readonly && (
        <Input
          type="text"
          defaultValue={value}
          disabled
          readOnly
          bg="body"
          border={false}
        />
      )}
      {readonly || (
        <Input
          type="text"
          id={uid}
          value={value}
          required={required}
          onChange={(e) => setValue(e.target.value)}
        />
      )}
    </FieldContainer>
  );
}
