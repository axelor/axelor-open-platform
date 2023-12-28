import { useAtomValue } from "jotai";
import React, { useCallback, useState } from "react";

import { Input } from "@axelor/ui";

import { useAppTheme } from "@/hooks/use-app-theme";

import { FieldControl, FieldProps } from "../../builder";
import { useInput } from "../../builder/hooks";
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
  const { uid, height, placeholder } = schema;
  const { onBlur } = inputProps || {};
  const theme = useAppTheme();

  const { attrs } = useAtomValue(widgetAtom);
  const { required } = attrs;

  const [changed, setChanged] = useState(false);
  const {
    text,
    onChange,
    onBlur: onInputBlur,
    onKeyDown,
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
    <FieldControl {...props}>
      {readonly ? (
        <Input as="pre" bg={theme === "dark" ? "body" : "light"} mb={0} className={styles.pre}>
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
    </FieldControl>
  );
}
