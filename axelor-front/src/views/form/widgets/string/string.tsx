import { Input, clsx } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { useMemo } from "react";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import styles from "./string.module.scss";
import { Translatable } from "./translatable";
import { ViewerInput } from "./viewer";

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

  const { text, onChange, onBlur } = useInput(valueAtom);

  const [trValue, setTranslateValue] = useAtom(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop(`$t:${name}`)),
      [name, formAtom],
    ),
  );

  return (
    <FieldControl
      {...props}
      className={clsx(styles.container, {
        [styles.translatable]: translatable && !readonly,
      })}
    >
      {readonly || trValue ? (
        <ViewerInput
          {...(inputProps?.type === "password" && { type: "password" })}
          value={trValue ?? text}
        />
      ) : (
        <Input
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
          {...inputProps}
        />
      )}
      {translatable && !readonly && (
        <Translatable value={text} onUpdate={setTranslateValue} />
      )}
    </FieldControl>
  );
}
