import { useAtomValue } from "jotai";
import { useCallback } from "react";

import { Field } from "@/services/client/meta.types";
import format from "@/utils/format";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { MaskedInput } from "../date/mask-input";
import { ViewerInput } from "../string/viewer";

const isValid = (value: string) => !value.includes("_");

export function Time(props: FieldProps<string | number>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, placeholder, widgetAttrs } = schema;
  const { seconds } = widgetAttrs;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const formatValue = useCallback(
    (value?: string | number | null) =>
      format(value, {
        props: schema as Field,
      }),
    [schema],
  );

  const { text, onChange, onBlur, onKeyDown } = useInput(valueAtom, {
    validate: isValid,
    format: formatValue,
    schema,
  });

  const toMask = useCallback(
    (value: string) => {
      const shouldMaxHrLastDigit = value?.startsWith("2");
      return [
        /[0-2]/,
        shouldMaxHrLastDigit ? /[0-3]/ : /\d/,
        ":",
        /[0-5]/,
        /\d/,
        ...(seconds ? [":", /[0-5]/, /\d/] : []),
      ];
    },
    [seconds],
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
          onBlur={onBlur}
          onKeyDown={onKeyDown}
          mask={toMask}
        />
      )}
    </FieldControl>
  );
}
