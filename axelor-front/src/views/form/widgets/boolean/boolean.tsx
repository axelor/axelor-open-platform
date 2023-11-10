import { useAtom, useAtomValue } from "jotai";

import { Input } from "@axelor/ui";

import { toKebabCase } from "@/utils/names";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./boolean.module.scss";

export function Boolean(props: FieldProps<boolean>) {
  const { schema, readonly, widgetAtom, valueAtom, invalid } = props;
  const { uid, name, widget } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { focus },
  } = useAtomValue(widgetAtom);

  const inline = toKebabCase(widget) === "inline-checkbox";
  const className = inline ? styles.inline : styles.checkbox;

  return (
    <FieldControl {...props} className={className}>
      <Input
        {...(focus && { key: "focused" })}
        data-input
        autoFocus={focus}
        m={0}
        id={uid}
        invalid={invalid}
        type="checkbox"
        className={styles.input}
        checked={!!value}
        onChange={() => {
          setValue(!value, true);
        }}
        value={name}
        disabled={readonly}
      />
    </FieldControl>
  );
}
