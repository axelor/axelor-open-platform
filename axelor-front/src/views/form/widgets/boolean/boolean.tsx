import { Input } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom } from "jotai";
import classes from './boolean.module.scss';

export function Boolean({ schema, readonly, valueAtom }: FieldProps<boolean>) {
  const { uid, name, title } = schema;
  const [value, setValue] = useAtom(valueAtom);
  return (
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      <Input
        type="checkbox"
        className={classes.input}
        checked={!!value}
        onChange={() => {
          setValue(!value, true);
        }}
        value={name}
        disabled={readonly}
      />
    </FieldContainer>
  );
}
