import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import { SingleSelectText } from "@/views/form/widgets/single-select";

export function SingleSelect(props: GridColumnProps) {
  const { data, record } = props;
  const value = record?.[data?.name];
  return <SingleSelectText schema={data} value={value} />;
}
