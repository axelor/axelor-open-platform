import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import { MultiSelectText } from "@/views/form/widgets/multi-select";

export function MultiSelect(props: GridColumnProps) {
  const { data, record } = props;
  const value = record?.[data?.name];
  return <MultiSelectText schema={data} value={value} />;
}
