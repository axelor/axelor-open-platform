import { useAtomValue } from "jotai";
import { useCallback, useMemo, useState } from "react";

import { Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import format from "@/utils/format";

import { FieldContainer, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";

import { Field } from "@/services/client/meta.types";
import styles from "./decimal.module.scss";

const NUM_PATTERN = /^(-)?\d*(\.(\d+)?)?$/;

export function Decimal({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<string>) {
  const { uid, min, max, showTitle = true } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { title, required, scale } = attrs;

  const { value, setValue } = useInput(valueAtom, {
    defaultValue: "",
  });

  const [changed, setChanged] = useState(false);

  const parse = useCallback((value: string, scale?: number) => {
    if (scale) {
      const nums = value.split(".");
      // scale the decimal part
      const dec = parseFloat(`0.${nums[1] || 0}`).toFixed(scale);
      // increment the integer part if decimal part is greater than 0 (due to rounding)
      const num = BigInt(nums[0]) + BigInt(parseInt(dec));
      // append the decimal part
      return num + dec.substring(1);
    }
    return value;
  }, []);

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
        setValue(value ? parse(value, scale) : value, true);
      }
    },
    [changed, parse, scale, setValue, value]
  );

  const checkRange = useCallback((value: string, min: any, max: any) => {
    if (min && value < min) return min;
    if (max && value > max) return max;
    return value;
  }, []);

  const increment = useCallback(
    (step: bigint) => {
      const text = value.trim();
      const nums = text.split(".");

      const int = nums[0];
      const dec = nums[1] || "";

      const bigInt = BigInt(int) + step;
      const num = dec ? `${bigInt}.${dec}` : `${bigInt}`;
      const res = checkRange(num, min, max);

      setValue(res);
    },
    [checkRange, max, min, setValue, value]
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

  const onUp = useCallback(() => increment(1n), [increment]);
  const onDown = useCallback(() => increment(-1n), [increment]);

  const text = useMemo(
    () => format(value, { props: { ...schema, scale } as Field }),
    [scale, schema, value]
  );

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      {readonly && (
        <Input
          type="text"
          value={text}
          disabled
          readOnly
          bg="body"
          border={false}
        />
      )}
      {readonly || (
        <div className={styles.container}>
          <Input
            className={styles.numberInput}
            type="text"
            id={uid}
            value={value}
            required={required}
            onChange={handleChange}
            onBlur={handleBlur}
            onKeyDown={handleKeyDown}
          />
          <div className={styles.buttons}>
            <MaterialIcon icon="arrow_drop_up" onClick={onUp} />
            <MaterialIcon icon="arrow_drop_down" onClick={onDown} />
          </div>
        </div>
      )}
    </FieldContainer>
  );
}
