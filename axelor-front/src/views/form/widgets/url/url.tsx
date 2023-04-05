import { useAtomValue } from "jotai";
import { Box } from "@axelor/ui";
import { String } from "../string";
import { FieldContainer, FieldProps } from "../../builder";

export function Url(props: FieldProps<string>) {
  const {
    schema: { uid, title },
    readonly,
    valueAtom,
  } = props;
  const value = useAtomValue(valueAtom);
  if (readonly) {
    return (
      value && (
        <FieldContainer readonly={readonly}>
          <label htmlFor={uid}>{title}</label>
          <Box as="a" target="_blank" href={value}>
            {value}
          </Box>
        </FieldContainer>
      )
    );
  }
  return <String {...props} inputProps={{ type: "url" }} />;
}
