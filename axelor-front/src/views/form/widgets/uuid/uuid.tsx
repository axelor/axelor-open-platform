import { useAtomValue } from "jotai";

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
  const { text, onChange, onBlur, onKeyDown } = useInput(valueAtom, {
    validate: isValid,
    schema,
  });

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
          onBlur={onBlur}
          onKeyDown={onKeyDown}
          mask={uuidMask}
        />
      )}
    </FieldControl>
  );
}
