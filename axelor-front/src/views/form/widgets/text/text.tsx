import { useAtom, useAtomValue } from "jotai";
import { useCallback, useState } from "react";
import { Box, Input } from "@axelor/ui";
import classes from "./text.module.scss";
import { FieldContainer, FieldProps } from "../../builder";

export function Text({
  schema,
  readonly,
  widgetAtom,
  valueAtom,
}: FieldProps<string>) {
  const { uid, height, showTitle = true } = schema;

  const { attrs } = useAtomValue(widgetAtom);
  const { title, required } = attrs;

  const [value, setValue] = useAtom(valueAtom);
  const [changed, setChanged] = useState(false);

  const handleChange = useCallback<
    React.ChangeEventHandler<HTMLTextAreaElement>
  >(
    (e) => {
      setValue(e.target.value);
      setChanged(true);
    },
    [setValue]
  );

  const handleBlur = useCallback<React.FocusEventHandler<HTMLTextAreaElement>>(
    (e) => {
      if (changed) {
        setChanged(false);
        setValue(e.target.value, true);
      }
    },
    [changed, setValue]
  );

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      {readonly ? (
        <Box
          p={1}
          w={100}
          className={classes.content}
          dangerouslySetInnerHTML={{ __html: value ?? "" }}
          {...(value ? { as: "pre" } : {})}
        />
      ) : (
        <Input
          as="textarea"
          rows={height || 5}
          id={uid}
          value={value || ""}
          required={required}
          onChange={handleChange}
          onBlur={handleBlur}
        />
      )}
    </FieldContainer>
  );
}
