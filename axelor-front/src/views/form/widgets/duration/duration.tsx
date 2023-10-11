import { useAtomValue } from "jotai";
import padStart from "lodash/padStart";
import { useCallback, useMemo } from "react";

import { moment } from "@/services/client/l10n";
import { toKebabCase } from "@/utils/names";
import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { MaskedInput } from "../date/mask-input";
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
  const [hr, min, secs = 0] = value
    .replace(/_/g, "0")
    .split(":")
    .map((x) => toNumber(parseInt(x)));
  return hr * 60 * 60 + min * 60 + secs;
}

const isValid = (value: string) => !value.includes("_");

export function Duration(props: FieldProps<string | number>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, placeholder, widgetAttrs } = schema;
  const { big, seconds } = widgetAttrs;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const isTime = toKebabCase(schema.widget!) === "time";

  const format = useCallback(
    (value?: string | number | null) => toText(value, { big, seconds }),
    [big, seconds],
  );

  const { value, text, onChange, onBlur } = useInput(valueAtom, {
    validate: isValid,
    ...(!isTime && {
      format,
      parse: toValue,
    }),
  });

  const displayText = useMemo(
    () => (isTime ? value : toText(value, { big, seconds })) ?? "",
    [isTime, big, seconds, value],
  );

  const toMask = useCallback(
    (value: string) => {
      const shouldMaxHrLastDigit = isTime && value?.startsWith("2");
      return [
        ...(big && !isTime ? [/\d/] : []),
        isTime ? /[0-2]/ : /\d/,
        shouldMaxHrLastDigit ? /[0-3]/ : /\d/,
        ":",
        /[0-5]/,
        /\d/,
        ...(seconds ? [":", /[0-5]/, /\d/] : []),
      ];
    },
    [big, seconds, isTime],
  );

  return (
    <FieldControl {...props}>
      {readonly && <ViewerInput value={displayText} />}
      {readonly || (
        <MaskedInput
          {...(focus && { key: "focused" })}
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
          mask={toMask}
        />
      )}
    </FieldControl>
  );
}
