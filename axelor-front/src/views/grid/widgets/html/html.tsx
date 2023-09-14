import { GridColumnProps } from "@axelor/ui/grid/grid-column";
import { sanitize } from "@/utils/sanitize";

export function Html(props: GridColumnProps) {
  const { value } = props;
  return <span dangerouslySetInnerHTML={{ __html: sanitize(value) }} />;
}
