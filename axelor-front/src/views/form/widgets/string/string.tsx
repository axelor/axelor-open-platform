import { Input } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { FieldProps } from "../../builder";
import styles from "./string.module.css";

export function String({
  schema,
  readonly,
  formAtom,
  widgetAtom,
  valueAtom,
}: FieldProps<string>) {
  const { uid, title } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { required } = attrs;

  const [value = "", setValue] = useAtom(valueAtom);

  return (
    <div className={styles.string}>
      <label htmlFor={uid}>{title}</label>
      {readonly && <div className={styles.value}>{value}</div>}
      {readonly || (
        <Input
          type="text"
          id={uid}
          value={value}
          required={required}
          onChange={(e) => setValue(e.target.value)}
        />
      )}
    </div>
  );
}
