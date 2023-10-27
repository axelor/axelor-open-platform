import { useAtom, useAtomValue } from "jotai";
import { selectAtom } from "jotai/utils";
import { get } from "lodash";
import { useCallback, useMemo, useRef, useState } from "react";

import { Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { Field } from "@/services/client/meta.types";
import convert from "@/utils/convert";
import format, { DEFAULT_SCALE } from "@/utils/format";

import { FieldControl, FieldProps } from "../../builder";
import { ViewerInput } from "../string/viewer";

import styles from "./decimal.module.scss";

const NUM_PATTERN = /^(-)?\d*(\.(\d+)?)?$/;

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

  const [value = null, setValue] = useAtom(valueAtom);
  const [changed, setChanged] = useState(false);

  const parse = useCallback(
    (value: string | number | null) =>
      convert(value, { props: { ...schema, scale } }),
    [schema, scale],
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
    [setValue],
  );

  const handleBlur = useCallback<
    React.FocusEventHandler<HTMLInputElement>
  >(() => {
    if (changed) {
      setChanged(false);
      setValue(parsedValue, true);
    }
  }, [changed, parsedValue, setValue]);

  const checkRange = useCallback((value: string, min: any, max: any) => {
    if (min != null && Number(value) < Number(min)) return min;
    if (max != null && Number(value) > Number(max)) return max;
    return value;
  }, []);

  const increment = useCallback(
    (step: bigint) => {
      const text = String(value ?? 0).trim();
      const nums = text.split(".");

      const int = nums[0];
      const dec = nums[1] || "";

      const bigInt = BigInt(int) + step;
      const num = dec ? `${bigInt}.${dec}` : `${bigInt}`;
      const res = checkRange(num, min, max);

      setChanged(true);
      setValue(parse(res));
    },
    [checkRange, max, min, parse, setValue, value],
  );

  const handleKeyDown = useCallback<
    React.KeyboardEventHandler<HTMLInputElement>
  >(
    (e) => {
      if (e.key === "ArrowUp" || e.key === "ArrowDown") e.preventDefault();
      if (e.key === "ArrowUp") increment(1n);
      if (e.key === "ArrowDown") increment(-1n);
    },
    [increment],
  );

  const handleUp = useCallback<React.MouseEventHandler<HTMLSpanElement>>(
    (e) => {
      e.preventDefault();
      if (inputRef.current) inputRef.current.focus();
      increment(1n);
    },
    [increment],
  );

  const handleDown = useCallback<React.MouseEventHandler<HTMLSpanElement>>(
    (e) => {
      e.preventDefault();
      if (inputRef.current) inputRef.current.focus();
      increment(-1n);
    },
    [increment],
  );

  const text = useMemo(
    () =>
      value != null ? format(value, { props: { ...schema, scale } as Field }) : "",
    [scale, schema, value],
  );

  const step = useMemo(
    () => (scale > 0 ? Math.pow(10, -Math.floor(scale)).toFixed(scale) : 1),
    [scale],
  );

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
            value={changed ? value ?? "" : parsedValue ?? ""}
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
