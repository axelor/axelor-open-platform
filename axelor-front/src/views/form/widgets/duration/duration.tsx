import { useAtomValue } from "jotai";
import padStart from "lodash/padStart";
import { FocusEventHandler, useCallback, useId, useMemo } from "react";

import { moment } from "@/services/client/l10n";
import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { MaskedInput } from "@/components/masked-input";
import { ViewerInput } from "../string/viewer";

function toText(
  value?: number | string | null,
  { big = false, seconds = false } = {},
) {
  if (value === null || value === undefined) return "";
  const timestamp = parseInt(String(value));
  let [secs, mins, hrs] = [
    timestamp % 60,
    Math.floor(timestamp / 60) % 60,
    Math.floor(timestamp / (60 * 60)),
  ];

  try {
    if (typeof value === "string") {
      const d = moment(value);
      if (d.isValid()) {
        mins = d.minute();
        secs = d.second();
        hrs = d.hour();
      }
    }
  } catch {
    // ignore
  }

  const result: string[] = [];
  if (timestamp >= 0) {
    result.push(padStart(String(hrs), big ? 3 : 2, "0"));
    result.push(padStart(String(mins), 2, "0"));
    if (seconds) {
      result.push(padStart(String(secs), 2, "0"));
    }
  }

  return result.join(":");
}

const toNumber = (val: string | number) =>
  (isNaN(val as number) ? 0 : val) as number;

function toValue(value: string) {
  if (!value || value.includes("_")) return null;
  const [hr, min, secs = 0] = value
    .split(":")
    .map((x) => toNumber(parseInt(x)));
  return hr * 60 * 60 + min * 60 + secs;
}

export function Duration(props: FieldProps<string | number>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { placeholder, widgetAttrs } = schema;
  const { big, seconds } = widgetAttrs;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const id = useId();

  const format = useCallback(
    (value?: string | number | null) => toText(value, { big, seconds }),
    [big, seconds],
  );

  const { value, text, setText, onChange, onBlur, onKeyDown } = useInput(
    valueAtom,
    {
      format,
      parse: toValue,
      schema,
    },
  );

  const handleBlur = useCallback<FocusEventHandler<HTMLInputElement>>(
    (e) => {
      onBlur(e);
      const val = e.target.value;
      if (!val || val.includes("_")) {
        setText("");
      }
    },
    [onBlur, setText],
  );

  const displayText = useMemo(
    () => toText(value, { big, seconds }) ?? "",
    [big, seconds, value],
  );

  const mask = useMemo(
    () => [
      ...(big ? [/\d/] : []),
      /\d/,
      /\d/,
      ":",
      /[0-5]/,
      /\d/,
      ...(seconds ? [":", /[0-5]/, /\d/] : []),
    ],
    [big, seconds],
  );

  return (
    <FieldControl {...props} inputId={id}>
      {readonly && <ViewerInput id={id} name={schema.name} value={displayText} />}
      {readonly || (
        <MaskedInput
          key={focus ? "focused" : "normal"}
          data-input
          data-testid="input"
          type="text"
          id={id}
          autoFocus={focus}
          placeholder={placeholder}
          value={text}
          invalid={invalid}
          required={required}
          onChange={onChange}
          onBlur={handleBlur}
          onKeyDown={onKeyDown}
          mask={mask}
        />
      )}
    </FieldControl>
  );
}
