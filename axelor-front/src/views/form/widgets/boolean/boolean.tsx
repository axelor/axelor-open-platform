import { Box, Input, InputLabel } from "@axelor/ui";
import { useAtom } from "jotai";
import { FieldContainer, FieldProps } from "../../builder";
import classes from "./boolean.module.scss";

export function Boolean({ schema, readonly, valueAtom }: FieldProps<boolean>) {
  const { uid, name, title } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const inline = schema.widget === "inline-checkbox";
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
        <InputLabel htmlFor={uid} m={0}>
          {title}
        </InputLabel>
        <Input
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
