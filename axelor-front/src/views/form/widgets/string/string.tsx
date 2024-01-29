import { useAtomValue } from "jotai";

import { Input, clsx } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { Translatable, useTranslationValue } from "./translatable";
import { ViewerInput } from "./viewer";

import styles from "./string.module.scss";

export function String({
  inputProps,
  ...props
}: FieldProps<string> & {
  inputProps?: Pick<
    React.InputHTMLAttributes<HTMLInputElement>,
    "type" | "autoComplete" | "placeholder" | "onFocus"
  >;
}) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, placeholder, translatable } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const { text, onChange, onBlur, onKeyDown, setValue } = useInput(valueAtom, {
    schema,
  });

  const [trValue, setTranslateValue] = useTranslationValue(props);

  return (
    <FieldControl
      {...props}
      className={clsx(styles.container, {
        [styles.translatable]: translatable && !readonly,
      })}
    >
      {readonly || trValue ? (
        <ViewerInput
          name={schema.name}
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
          onKeyDown={onKeyDown}
          onChange={onChange}
          onBlur={onBlur}
          {...inputProps}
        />
      )}
      {translatable && !readonly && (
        <Translatable
          value={text}
          onValueChange={setValue}
          onUpdate={setTranslateValue}
        />
      )}
    </FieldControl>
  );
}
