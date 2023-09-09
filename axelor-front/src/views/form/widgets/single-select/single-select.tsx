import { MultiSelect } from "..";
import { FieldProps } from "../../builder";

export function SingleSelect(props: FieldProps<string | number | null>) {
  return <MultiSelect {...props} multiple={false} />;
}
