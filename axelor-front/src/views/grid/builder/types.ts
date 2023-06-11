import { DataContext } from "@/services/client/data.types";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

export type GridCellProps = GridColumnProps & {
  onAction?: (action: string, context?: DataContext) => Promise<any>;
};
