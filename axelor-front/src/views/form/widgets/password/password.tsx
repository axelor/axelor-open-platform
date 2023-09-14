import { String } from "../string";
import { FieldProps } from "../../builder";

export function Password(props: FieldProps<string>) {
  return (
    <String
      {...props}
      inputProps={{ type: "password", autoComplete: "new-password" }}
    />
  );
}
