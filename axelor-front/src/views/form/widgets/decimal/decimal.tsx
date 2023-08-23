import { useAtomValue } from "jotai";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Field, Property } from "@/services/client/meta.types";
import format, { DEFAULT_SCALE } from "@/utils/format";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { parseDecimal } from "../../builder/utils";
import { ViewerInput } from "../string/viewer";

import styles from "./decimal.module.scss";

const NUM_PATTERN = /^(-)?\d*(\.(\d+)?)?$/;

export function Decimal(props: FieldProps<string | number>) {
  const { schema, readonly, invalid, widgetAtom, valueAtom } = props;
  const { uid, min, max, placeholder, nullable } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required, scale: scaleAttr } = attrs;

  const isDecimal =
    schema.widget === "decimal" || schema.serverType === "DECIMAL";
  const scale = isDecimal ? scaleAttr ?? DEFAULT_SCALE : 0;

  const { value, setValue } = useInput(valueAtom, {
    defaultValue: "",
  });

  const inputRef = useRef<HTMLInputElement>(null);

  const [changed, setChanged] = useState(false);

  const parse = useCallback(
    (value: string | number): string | number => {
      if (value == null || value === "") {
        return nullable ? "" : parse("0");
      }
      return isDecimal
        ? parseDecimal(value, { scale } as Property)
        : parseInt(String(value));
    },
    [isDecimal, scale, nullable]
  );

  const parsedValue = useMemo(() => parse(value), [parse, value]);

  const handleChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (e) => {
      const text = e.target.value.trim();
      if (NUM_PATTERN.test(text)) {
        setValue(text);
        setChanged(true);
      }
    },
    [setValue]
  );

  const handleBlur = useCallback<React.FocusEventHandler<HTMLInputElement>>(
    (e) => {
      if (changed) {
        setChanged(false);
        setValue(parsedValue, true);
      }
    },
    [changed, parsedValue, setValue]
  );

  const checkRange = useCallback((value: string, min: any, max: any) => {
    if (min && value < min) return min;
    if (max && value > max) return max;
    return value;
  }, []);

  const increment = useCallback(
    (step: bigint) => {
      const text = String(value).trim();
      const nums = text.split(".");

      const int = nums[0];
      const dec = nums[1] || "";

      const bigInt = BigInt(int) + step;
      const num = dec ? `${bigInt}.${dec}` : `${bigInt}`;
      const res = checkRange(num, min, max);

      setChanged(true);
      setValue(parse(res));
    },
    [checkRange, max, min, parse, setValue, value]
  );

  const handleKeyDown = useCallback<
    React.KeyboardEventHandler<HTMLInputElement>
  >(
    (e) => {
      if (e.key === "ArrowUp" || e.key === "ArrowDown") e.preventDefault();
      if (e.key === "ArrowUp") increment(1n);
      if (e.key === "ArrowDown") increment(-1n);
    },
    [increment]
  );

  const handleUp = useCallback<React.MouseEventHandler<HTMLSpanElement>>(
    (e) => {
      e.preventDefault();
      if (inputRef.current) inputRef.current.focus();
      increment(1n);
    },
    [increment]
  );

  const handleDown = useCallback<React.MouseEventHandler<HTMLSpanElement>>(
    (e) => {
      e.preventDefault();
      if (inputRef.current) inputRef.current.focus();
      increment(-1n);
    },
    [increment]
  );

  const text = useMemo(
    () =>
      nullable && !value
        ? value
        : format(value, { props: { ...schema, scale } as Field }),
    [nullable, scale, schema, value]
  );

  const step = scale != null ? 1 / Math.pow(10, scale) : 1;

  return (
    <FieldControl {...props}>
      {readonly && <ViewerInput value={text} />}
      {readonly || (
        <div className={styles.container}>
          <Input
            {...(focus && { key: "focused" })}
            data-input
            className={styles.numberInput}
            autoFocus={focus}
            type="number"
            step={step}
            id={uid}
            ref={inputRef}
            placeholder={placeholder}
            value={changed ? value : parsedValue}
            invalid={invalid}
            required={required}
            onChange={handleChange}
            onBlur={handleBlur}
            onKeyDown={handleKeyDown}
          />
          <div className={styles.buttons}>
            <span onMouseDown={handleUp}>
              <MaterialIcon icon="arrow_drop_up" />
            </span>
            <span onMouseDown={handleDown}>
              <MaterialIcon icon="arrow_drop_down" />
            </span>
          </div>
        </div>
      )}
    </FieldControl>
  );
}
