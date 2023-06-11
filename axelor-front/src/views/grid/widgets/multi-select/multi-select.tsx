import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import { MultiSelectText } from "@/views/form/widgets/multi-select";

export function MultiSelect(props: GridColumnProps) {
  const { data, value } = props;
  return <MultiSelectText schema={data} value={value} />;
}
