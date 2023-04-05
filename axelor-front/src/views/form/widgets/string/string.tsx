import { useAtomValue } from "jotai";

import { InputHTMLAttributes, useCallback, useState } from "react";
import { Input, InputProps } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";

export function String({
  inputProps,
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<string> & {
  inputProps?: InputHTMLAttributes<HTMLInputElement>;
}) {
  const { uid, title } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { required } = attrs;

  const { value, onChange, onBlur } = useInput(valueAtom, {
    defaultValue: "",
  });

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
          {...(inputProps as InputProps)}
        />
      )}
      {readonly || (
        <Input
          type="text"
          id={uid}
          value={value || ""}
          required={required}
          onChange={onChange}
          onBlur={onBlur}
          {...(inputProps as InputProps)}
        />
      )}
    </FieldContainer>
  );
}
