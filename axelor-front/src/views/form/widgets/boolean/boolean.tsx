import { Box, Input } from "@axelor/ui";
import { useAtom, useAtomValue } from "jotai";
import { FieldContainer, FieldProps } from "../../builder";
import { toKebabCase } from "@/utils/names";
import classes from "./boolean.module.scss";

export function Boolean({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<boolean>) {
  const { uid, name, widget, showTitle = true } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { title, focus },
  } = useAtomValue(widgetAtom);

  const inline = toKebabCase(widget) === "inline-checkbox";
  return (
    <FieldContainer readonly={readonly}>
      <Box
        d="flex"
        g={2}
        flexDirection={inline ? "row-reverse" : "column"}
        {...(inline && {
          alignItems: "center",
          justifyContent: "flex-end",
        })}
      >
        {showTitle && <label htmlFor={uid}>{title}</label>}
        <Input
          data-input
          autoFocus={focus}
          m={0}
          id={uid}
          type="checkbox"
          className={classes.input}
          checked={!!value}
          onChange={() => {
            setValue(!value, true);
          }}
          value={name}
          disabled={readonly}
        />
      </Box>
    </FieldContainer>
  );
}
