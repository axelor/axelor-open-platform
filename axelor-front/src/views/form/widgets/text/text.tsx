import { useAtomValue } from "jotai";
import React, { useCallback, useState } from "react";

import { clsx, Input } from "@axelor/ui";

import { useAppSettings } from "@/hooks/use-app-settings";
import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
import { Translatable, useTranslationValue } from "../string/translatable";

import styles from "./text.module.scss";

export function Text({
  inputProps,
  ...props
}: FieldProps<string> & {
  inputProps?: Pick<
    React.InputHTMLAttributes<HTMLTextAreaElement>,
    "onFocus" | "onBlur" | "autoFocus"
  >;
}) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, height, placeholder, translatable } = schema;
  const { onBlur } = inputProps || {};
  const { themeMode } = useAppSettings();

  const { attrs } = useAtomValue(widgetAtom);
  const { required } = attrs;

  const [_, setTranslateValue] = useTranslationValue(props);

  const [changed, setChanged] = useState(false);
  const {
    text,
    onChange,
    onBlur: onInputBlur,
    onKeyDown,
    setValue,
  } = useInput(valueAtom, { schema });

  const handleChange = useCallback<
    React.ChangeEventHandler<HTMLTextAreaElement>
  >(
    (e) => {
      onChange(e);
      setChanged(true);
    },
    [onChange],
  );

  const handleBlur = useCallback<React.FocusEventHandler<HTMLTextAreaElement>>(
    (e) => {
      if (changed) {
        setChanged(false);
        onInputBlur(e);
      }
      onBlur?.(e);
    },
    [changed, onBlur, onInputBlur],
  );

  return (
    <FieldControl
      {...props}
      className={clsx(styles.container, {
        [styles.translatable]: translatable && !readonly,
      })}
    >
      {readonly ? (
        <Input
          as="pre"
          bg={themeMode === "dark" ? "body" : "light"}
          mb={0}
          className={styles.pre}
        >
          {text}
        </Input>
      ) : (
        <Input
          data-input
          as="textarea"
          rows={height || 5}
          id={uid}
          invalid={invalid}
          placeholder={placeholder}
          value={text}
          required={required}
          {...inputProps}
          onChange={handleChange}
          onBlur={handleBlur}
          onKeyDown={onKeyDown}
          className={styles.textarea}
        />
      )}
      {translatable && !readonly && (
        <Translatable
          position="top"
          value={text}
          onValueChange={setValue}
          onUpdate={setTranslateValue}
        />
      )}
    </FieldControl>
  );
}
