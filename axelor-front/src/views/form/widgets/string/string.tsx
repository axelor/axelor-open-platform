import { Input } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { useCallback } from "react";
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

  const [value, setValue] = useAtom(valueAtom);
  const defaultValue = value ?? "";

  const handleBlur = useCallback<React.FocusEventHandler<HTMLInputElement>>(
    (e) => {
      setValue(e.target.value, true);
    },
    [setValue]
  );

  return (
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      {readonly && (
        <Input
          type="text"
          defaultValue={defaultValue}
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
          defaultValue={defaultValue}
          required={required}
          onBlur={handleBlur}
        />
      )}
    </FieldContainer>
  );
}
