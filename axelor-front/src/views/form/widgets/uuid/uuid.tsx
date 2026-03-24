import { useAtomValue } from "jotai";
import { useCallback } from "react";

import { MaskedInput } from "@/components/masked-input";
import { ViewerInput } from "@/views/form/widgets/string/viewer";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";

const hex = /[0-9a-fA-F]/;

// prettier-ignore
const uuidMask = [
  hex, hex, hex, hex, hex, hex, hex, hex,
  '-',
  hex, hex, hex, hex,
  '-',
  hex, hex, hex, hex,
  '-',
  hex, hex, hex, hex,
  '-',
  hex, hex, hex, hex, hex, hex, hex, hex, hex, hex, hex, hex
];

const isValid = (value: string) => !value.includes("_");

export function Uuid(props: FieldProps<string>) {
  const { schema, readonly, valueAtom, widgetAtom, invalid } = props;
  const { uid, placeholder } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;
  const { text, setText, onChange, onBlur, onKeyDown } = useInput(valueAtom, {
    validate: isValid,
    schema,
  });

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      onBlur(e);
      if (!isValid(e.target.value)) {
        setText("");
      }
    },
    [onBlur, setText],
  );

  return (
    <FieldControl {...props}>
      {readonly && <ViewerInput name={schema.name} value={text} />}
      {readonly || (
        <MaskedInput
          key={focus ? "focused" : "normal"}
          data-input
          type="text"
          id={uid}
          autoFocus={focus}
          placeholder={placeholder}
          value={text}
          invalid={invalid}
          required={required}
          onChange={onChange}
          onBlur={handleBlur}
          onKeyDown={onKeyDown}
          mask={uuidMask}
        />
      )}
    </FieldControl>
  );
}
