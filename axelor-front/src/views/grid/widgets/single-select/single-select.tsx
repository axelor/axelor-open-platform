import { GridColumnProps } from "@axelor/ui/grid";
import { SingleSelectValue } from "./single-select-value";

export function SingleSelect(props: GridColumnProps) {
  const { data, record } = props;
  const value = record?.[data?.name];
  return <SingleSelectValue schema={data} value={value} />;
}
