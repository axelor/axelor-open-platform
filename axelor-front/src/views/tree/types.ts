import { TreeField, TreeNode } from "@/services/client/meta.types";
import { ActionExecutor } from "@/view-containers/action";
import { TreeNode as TreeRecord } from "@axelor/ui";

export interface WidgetProps {
  field: TreeField;
  node: TreeNode;
  record: TreeRecord["data"];
  actionExecutor?: ActionExecutor;
}
