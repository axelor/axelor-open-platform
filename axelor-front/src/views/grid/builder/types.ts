import { DataContext, DataRecord } from "@/services/client/data.types";
import { GridView } from "@/services/client/meta.types";
import { ActionExecutor } from "@/view-containers/action";
import { GridColumnProps } from "@axelor/ui/grid";

export type GridCellProps = GridColumnProps & {
  view?: GridView;
  viewContext?: DataContext;
  onUpdate?: (record: DataRecord) => void;
  actionExecutor?: ActionExecutor;
};
