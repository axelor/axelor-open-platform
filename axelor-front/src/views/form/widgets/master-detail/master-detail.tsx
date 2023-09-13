import { FieldProps } from "../../builder";
import { OneToMany } from "../one-to-many";

export function MasterDetail(props: FieldProps<any[]>) {
  return <OneToMany {...props} />;
}
