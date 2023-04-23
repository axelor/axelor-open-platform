import { TreeView } from "@/services/client/meta.types";
import { TreeNode } from "@axelor/ui";

export function getNodeOfTreeRecord(
  view: TreeView,
  record: TreeNode,
  isNext = false
) {
  return view.nodes?.[
    Math.min((record.level ?? 0) + (isNext ? 1 : 0), view.nodes?.length - 1)
  ];
}
