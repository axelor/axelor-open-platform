import { GridView } from "@/services/client/meta.types";
import { ActionExecutor } from "@/view-containers/action";
import { GridColumnProps } from "@axelor/ui/grid/grid-column";

export type GridCellProps = GridColumnProps & {
  view?: GridView;
  actionExecutor?: ActionExecutor;
};
