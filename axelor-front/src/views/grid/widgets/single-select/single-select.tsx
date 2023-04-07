import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";
import { SingleSelectText } from "@/views/form/widgets/single-select";

export function SingleSelect(props: GridColumnProps) {
  const { data, value } = props;
  return <SingleSelectText schema={data} value={value} />;
}
