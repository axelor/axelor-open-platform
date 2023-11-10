import { useMemo } from "react";
import { TreeNode } from "@axelor/ui";
import { get } from "lodash";

import {
  Field,
  TreeColumn,
  TreeField,
  TreeView,
} from "@/services/client/meta.types";
import { ActionExecutor } from "@/view-containers/action";
import { getNodeOfTreeRecord } from "../../utils";
import format from "@/utils/format";
import { useWidgetComp } from "../../hooks";
import { toKebabCase } from "@/utils/names";

export interface NodeTextProps {
  column: TreeColumn;
  data: TreeNode;
  view: TreeView;
  actionExecutor?: ActionExecutor;
}

export function NodeText({
  column,
  data,
  view,
  actionExecutor,
}: NodeTextProps) {
  const node = getNodeOfTreeRecord(view, data);
  const { data: record } = data;

  let value = get(record, column.name);
  const field = useMemo<Field | undefined>(() => {
    return {
      ...column,
      ...(node?.items as TreeField[])?.find(
        (item) => (item.as || item.name) === column.name,
      ),
    } as Field;
  }, [node, column]);
  const { data: Component } = useWidgetComp(
    toKebabCase(column.widget || column.type!),
  );

  if (Component && field) {
    return (
      <Component
        field={field}
        node={node}
        record={record}
        actionExecutor={actionExecutor}
      />
    );
  }

  if (field?.targetName) {
    return value && value[field.targetName];
  }

  if (field?.type) {
    value = format(value, {
      props: {
        ...field,
        ...column,
        ...column.widgetAttrs,
      } as Field,
      context: record,
    });
  }

  if ((!value && value !== 0) || typeof value === "object") {
    return "---";
  }

  return value;
}
