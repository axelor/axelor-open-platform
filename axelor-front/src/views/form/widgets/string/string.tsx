import { useAtomValue } from "jotai";
import { Fragment, useCallback, useMemo } from "react";

import { AdornedInput, clsx } from "@axelor/ui";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { Translatable, useTranslationValue } from "./translatable";
import { ViewerInput } from "./viewer";

import styles from "./string.module.scss";

export function String({
  inputProps,
  inputEndAdornment,
  onChange,
  ...props
}: FieldProps<string> & {
  inputProps?: Pick<
    React.InputHTMLAttributes<HTMLInputElement>,
    "type" | "autoComplete" | "placeholder" | "onFocus"
  >;
  inputEndAdornment?: JSX.Element;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
}) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, placeholder, translatable } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { focus, required } = attrs;

  const {
    text,
    onChange: _onChange,
    onBlur,
    onKeyDown,
    setValue,
  } = useInput(valueAtom, {
    schema,
  });

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange?.(e);
      _onChange(e);
    },
    [onChange, _onChange],
  );

  const [trValue, setTranslateValue] = useTranslationValue(props);

  const translatableAdornment = useMemo(
    () =>
      translatable && !readonly ? (
        <Translatable
          value={text}
          onValueChange={setValue}
          onUpdate={setTranslateValue}
        />
      ) : undefined,
    [readonly, setTranslateValue, setValue, text, translatable],
  );

  const endAdornment = useMemo(() => {
    const elements = [inputEndAdornment, translatableAdornment].filter(Boolean);
    return elements.length ? (
      <>
        {elements.map((element, index) => (
          <Fragment key={index}>{element}</Fragment>
        ))}
      </>
    ) : undefined;
  }, [inputEndAdornment, translatableAdornment]);

  return (
    <FieldControl {...props} className={clsx(styles.container)}>
      {readonly || trValue ? (
        <ViewerInput
          name={schema.name}
          {...(inputProps?.type === "password" && { type: "password" })}
          value={trValue ?? text}
          endAdornment={translatableAdornment}
        />
      ) : (
        <AdornedInput
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
          onChange={handleInputChange}
          onBlur={onBlur}
          endAdornment={endAdornment}
          {...inputProps}
        />
      )}
    </FieldControl>
  );
}
