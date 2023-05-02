import { useAtomValue } from "jotai";

import { Input } from "@axelor/ui";

import { FieldContainer, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { ViewerInput } from "./viewer";

export function String({
  inputProps,
  schema,
  readonly,
  widgetAtom,
  valueAtom,
  invalid,
}: FieldProps<string> & {
  inputProps?: Pick<
    React.InputHTMLAttributes<HTMLInputElement>,
    "type" | "autoComplete" | "placeholder" | "onFocus"
  >;
}) {
  const { uid, placeholder, showTitle = true } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { title, required } = attrs;

  const { value, onChange, onBlur } = useInput(valueAtom, {
    defaultValue: "",
  });

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      {readonly && <ViewerInput value={value} />}
      {readonly || (
        <Input
          data-input
          type="text"
          id={uid}
          placeholder={placeholder}
          value={value}
          invalid={invalid}
          required={required}
          onChange={onChange}
          onBlur={onBlur}
          {...inputProps}
        />
      )}
    </FieldContainer>
  );
}
