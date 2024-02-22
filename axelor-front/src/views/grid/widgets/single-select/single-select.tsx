import { GridColumnProps } from "@axelor/ui/grid";
import { SingleSelectValue } from "./single-select-value";

export function SingleSelect(props: GridColumnProps) {
  const { data, rawValue } = props;
  return <SingleSelectValue schema={data} value={rawValue} />;
}
