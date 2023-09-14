import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import { ImageSelectText } from "@/views/form/widgets";

export function ImageSelect(props: GridColumnProps) {
  const { data, record } = props;
  const value = record?.[data?.name];
  return <ImageSelectText schema={data} value={value} />;
}
