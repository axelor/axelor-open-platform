import { useAtom, useAtomValue } from "jotai";
import { useCallback, useState } from "react";

import { Input } from "@axelor/ui";

import { FieldContainer, FieldProps } from "../../builder";

export function String({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<string>) {
  const { uid, title } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { required } = attrs;

  const [value, setValue] = useAtom(valueAtom);
  const [changed, setChanged] = useState(false);

  const handleChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (e) => {
      setValue(e.target.value);
      setChanged(true);
    },
    [setValue]
  );

  const handleBlur = useCallback<React.FocusEventHandler<HTMLInputElement>>(
    (e) => {
      if (changed) {
        setChanged(false);
        setValue(e.target.value, true);
      }
    },
    [changed, setValue]
  );

  return (
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      {readonly && (
        <Input
          type="text"
          value={value || ""}
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
          value={value || ""}
          required={required}
          onChange={handleChange}
          onBlur={handleBlur}
        />
      )}
    </FieldContainer>
  );
}
