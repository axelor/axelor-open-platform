import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import { ImageSelectText } from "@/views/form/widgets";

export function ImageSelect(props: GridColumnProps) {
  const { data, value } = props;
  return <ImageSelectText schema={data} value={value} />;
}
