import { useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { get } from "lodash";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Field } from "@/services/client/meta.types";
import convert from "@/utils/convert";
import format, { DEFAULT_SCALE } from "@/utils/format";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { ViewerInput } from "../string/viewer";

import styles from "./decimal.module.scss";

const NUM_PATTERN = /^(-)?\d*(\.(\d+)?)?$/;

const isNumberLike = (text: string) => NUM_PATTERN.test(text);

export function Decimal(props: FieldProps<string | number>) {
  const { schema, readonly, invalid, widgetAtom, valueAtom, formAtom } = props;
  const { uid, minSize: min, maxSize: max, placeholder } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required, scale: scaleAttr } = attrs;

  const isDecimal =
    schema.widget === "decimal" || schema.serverType === "DECIMAL";

  const scaleNum = useMemo(() => {
    if (!isDecimal) {
      return 0;
    }
    if (typeof scaleAttr === "number") {
      return Math.floor(scaleAttr);
    }
    if (scaleAttr == null || scaleAttr === "") {
      return DEFAULT_SCALE;
    }
    return parseInt(scaleAttr);
  }, [isDecimal, scaleAttr]);

  const scale = useAtomValue(
    useMemo(
      () =>
        selectAtom(formAtom, (form) => {
          if (!isNaN(scaleNum)) {
            return scaleNum;
          }
          const value = parseInt(get(form.record, scaleAttr ?? ""));
          return isNaN(value) ? DEFAULT_SCALE : value;
        }),
      [formAtom, scaleAttr, scaleNum],
    ),
  );

  const inputRef = useRef<HTMLInputElement>(null);
  const timerRef = useRef<number>();

  const parse = useCallback(
    (value?: string | number | null) =>
      convert(value, { props: { ...schema, scale } }),
    [schema, scale],
  );

  const {
    text: textValue,
    setText: setTextValue,
    value,
    setChanged,
    onChange,
    onBlur,
    onKeyDown,
  } = useInput(valueAtom, {
    validate: isNumberLike,
    parse,
    format: parse,
  });
  const textRef = useRef(textValue);

  const checkRange = useCallback((value: string, min: any, max: any) => {
    if (min != null && Number(value) < Number(min)) return min;
    if (max != null && Number(value) > Number(max)) return max;
    return value;
  }, []);

  const increment = useCallback(
    (step: bigint, useTextRef?: boolean) => {
      const text = String(
        (useTextRef ? textRef.current : textValue) ?? 0,
      ).trim();
      const nums = text.split(".");

      const int = nums[0];
      const dec = nums[1] || "";

      const bigInt = BigInt(int) + step;
      const num = dec ? `${bigInt}.${dec}` : `${bigInt}`;
      const res = checkRange(num, min, max);

      setChanged(true);
      setTextValue((textRef.current = parse(res)));
    },
    [checkRange, max, min, parse, setChanged, setTextValue, textValue],
  );

  const handleKeyDown = useCallback<
    React.KeyboardEventHandler<HTMLInputElement>
  >(
    (e) => {
      if (e.key === "ArrowUp" || e.key === "ArrowDown") e.preventDefault();
      if (e.key === "ArrowUp") increment(1n);
      if (e.key === "ArrowDown") increment(-1n);
      onKeyDown(e);
    },
    [increment, onKeyDown],
  );

  const setTimer = useCallback((fn: () => void) => {
    timerRef.current = window.setTimeout(() => {
      timerRef.current = window.setInterval(() => {
        fn();
      }, 25);
    }, 200);
  }, []);

  const clearTimer = useCallback(() => {
    window.clearInterval(timerRef.current);
  }, []);

  const handleUp = useCallback(
    (e: React.SyntheticEvent) => {
      e.preventDefault();
      if (inputRef.current) inputRef.current.focus();
      increment(1n);
      setTimer(() => increment(1n, true));
    },
    [increment, setTimer],
  );

  const handleDown = useCallback(
    (e: React.SyntheticEvent) => {
      e.preventDefault();
      if (inputRef.current) inputRef.current.focus();
      increment(-1n);
      setTimer(() => increment(-1n, true));
    },
    [increment, setTimer],
  );

  const text = useMemo(
    () =>
      value != null
        ? format(value, { props: { ...schema, scale } as Field })
        : "",
    [scale, schema, value],
  );

  const step = useMemo(
    () => (scale > 0 ? Math.pow(10, -Math.floor(scale)).toFixed(scale) : 1),
    [scale],
  );

  useEffect(() => {
    return () => clearTimer();
  }, [clearTimer]);

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
            value={textValue ?? ""}
            invalid={invalid}
            required={required}
            onChange={onChange}
            onBlur={onBlur}
            onKeyDown={handleKeyDown}
          />
          <div className={styles.buttons}>
            <span
              onTouchStart={handleUp}
              onTouchEnd={clearTimer}
              onMouseDown={handleUp}
              onMouseUp={clearTimer}
            >
              <MaterialIcon icon="arrow_drop_up" />
            </span>
            <span
              onTouchStart={handleDown}
              onTouchEnd={clearTimer}
              onMouseDown={handleDown}
              onMouseUp={clearTimer}
            >
              <MaterialIcon icon="arrow_drop_down" />
            </span>
          </div>
        </div>
      )}
    </FieldControl>
  );
}
