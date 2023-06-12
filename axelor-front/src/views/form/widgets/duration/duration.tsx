import { useAtom, useAtomValue } from "jotai";

import { FieldControl, FieldProps } from "../../builder";
import { ViewerInput } from "../string/viewer";
import { MaskedInput } from "../date/mask-input";
import { useCallback, useEffect, useMemo, useState } from "react";
import padStart from "lodash/padStart";

function getValue(
  value: number | string,
  { big = false, seconds = false } = {}
) {
  const timestamp = parseInt(String(value));
  const [secs, mins, hrs] = [
    timestamp % 60,
    Math.floor(timestamp / 60) % 60,
    Math.floor(timestamp / (60 * 60)),
  ];

  return timestamp > 0
    ? `
    ${padStart(`${hrs}`, big ? 3 : 2, "0")}
    :${padStart(`${mins}`, 2, "0")}
    ${seconds ? `:${padStart(`${secs}`, 2, "0")}` : ""}`
    : "";
}

function toValue(value: string) {
  const toNumber = (val: string | number) =>
    (isNaN(val as number) ? 0 : val) as number;
  const [hr, min, secs = 0] = value
    .replace(/_/g, "0")
    .split(":")
    .map((x) => toNumber(parseInt(x)));

  return hr * 60 * 60 + min * 60 + secs;
}

const isValid = (value: string | number | null) =>
  !String(value)?.includes("_");

export function Duration(props: FieldProps<string | number>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, placeholder, widgetAttrs } = schema;
  const { big, seconds } = widgetAttrs;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;
  const [changed, setChanged] = useState(false);
  const [_value = "", setValue] = useAtom(valueAtom);

  const value = useMemo(
    () =>
      (isValid(_value)
        ? getValue(_value ?? "", { big, seconds })
        : _value) as string,
    [_value, big, seconds]
  );
  const [maskValue, setMaskValue] = useState(value);

  const update = useCallback(
    (value: string, fireOnChange = false) => {
      const valid = isValid(value);
      if (valid || fireOnChange) {
        setChanged(true);
        setValue(valid ? toValue(value) : value, fireOnChange);
      }
    },
    [setValue]
  );

  const onChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (e) => {
      const value = e.target.value;
      setMaskValue(value);
      update(e.target.value);
    },
    [update]
  );

  const onBlur = useCallback<React.FocusEventHandler<HTMLInputElement>>(
    (e) => {
      update(e.target.value, true);
      setChanged(false);
    },
    [changed, update]
  );

  useEffect(() => {
    setMaskValue(value);
  }, [value]);

  return (
    <FieldControl {...props}>
      {readonly && <ViewerInput value={value} />}
      {readonly || (
        <MaskedInput
          data-input
          type="text"
          id={uid}
          autoFocus={focus}
          placeholder={placeholder}
          value={maskValue}
          invalid={invalid}
          required={required}
          onChange={onChange}
          onBlur={onBlur}
          mask={[
            ...(big ? [/\d/] : []),
            /\d/,
            /\d/,
            ":",
            /[0-5]/,
            /\d/,
            ...(seconds ? [":", /[0-5]/, /\d/] : []),
          ]}
        />
      )}
    </FieldControl>
  );
}
