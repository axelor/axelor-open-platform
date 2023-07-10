import { DataContext } from "@/services/client/data.types";
import { GridView } from "@/services/client/meta.types";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

export type GridCellProps = GridColumnProps & {
  view?: GridView;
  onAction?: (action: string, context?: DataContext) => Promise<any>;
};
