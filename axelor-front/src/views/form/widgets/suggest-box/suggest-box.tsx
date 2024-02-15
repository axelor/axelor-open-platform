import { DataRecord } from "@/services/client/data.types";
import { FieldProps } from "../../builder";
import { ManyToOne } from "../many-to-one";

export function SuggestBox(props: FieldProps<DataRecord>) {
  return <ManyToOne {...props} isSuggestBox />;
}
