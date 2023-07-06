import { focusAtom } from "jotai-optics";
import { useAtom, useAtomValue } from "jotai";
import { useMemo } from "react";
import { Input } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { ViewerInput } from "./viewer";
import { Translatable } from "./translatable";
import styles from "./string.module.css";

export function String({
  inputProps,
  ...props
}: FieldProps<string> & {
  inputProps?: Pick<
    React.InputHTMLAttributes<HTMLInputElement>,
    "type" | "autoComplete" | "placeholder" | "onFocus"
  >;
}) {
  const { schema, readonly, widgetAtom, formAtom, valueAtom, invalid } = props;
  const { uid, name, placeholder, translatable } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const { value, onChange, onBlur } = useInput(valueAtom, {
    defaultValue: "",
  });

  const [trValue, setTranslateValue] = useAtom(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop(`$t:${name}`)),
      [name, formAtom]
    )
  );

  return (
    <FieldControl {...props} className={styles.container}>
      {readonly || trValue ? (
        <ViewerInput value={trValue ?? value} />
      ) : (
        <Input
          {...(focus && { key: "focused" })}
          data-input
          type="text"
          id={uid}
          autoFocus={focus}
          placeholder={placeholder}
          value={value}
          invalid={invalid}
          required={required}
          onChange={onChange}
          onBlur={onBlur}
          {...inputProps}
        />
      )}
      {translatable && !readonly && (
        <Translatable value={value} onUpdate={setTranslateValue} />
      )}
    </FieldControl>
  );
}
