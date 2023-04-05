import { Input } from "@axelor/ui";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom } from "jotai";
import { toKebabCase } from "@/utils/names";
import classes from "./boolean.module.scss";

export function Boolean({ schema, readonly, valueAtom }: FieldProps<boolean>) {
  const { uid, name, title } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const showTitle = toKebabCase(schema.widget) !== "inline-checkbox";
  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
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
