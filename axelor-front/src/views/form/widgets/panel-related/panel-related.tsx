import { FieldProps } from "../../builder";
import { OneToMany } from "../one-to-many";

export function PanelRelated(props: FieldProps<any[]>) {
  return <OneToMany {...props} />;
}
