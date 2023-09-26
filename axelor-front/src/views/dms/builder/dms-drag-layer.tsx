import { useMemo } from "react";
import { useAtomValue } from "jotai";
import { Badge, Box, useDragLayer } from "@axelor/ui";

import { DataRecord } from "@/services/client/data.types";
import { DMS_NODE_TYPE } from "./index";
import { useDMSGridHandlerAtom } from "./handler";
import styles from "./dms-drag-layer.module.scss";

function getItemStyles(offset: { x: number; y: number } | null) {
  if (!offset) {
    return { display: "none" };
  }
  const { x, y } = offset;
  const transform = `translate(${x - 5}px, ${y - 5}px)`;
  return {
    transform,
    WebkitTransform: transform,
  };
}
function DMSGridRowPreview({ record }: { record?: DataRecord }) {
  const { getSelectedDocuments } = useAtomValue(useDMSGridHandlerAtom());
  const docs = useMemo(() => {
    const docs = getSelectedDocuments?.();
    return docs?.some((d) => d.id === record?.id)
      ? docs
      : [...(docs ?? []), record];
  }, [getSelectedDocuments, record]);
  const total = docs?.length;
  return (
    <Box>
      {record?.fileName}
      {total > 1 && (
        <Badge as="span" className={styles.badge} rounded bg="danger">
          {total}
        </Badge>
      )}
    </Box>
  );
}

export const DMSCustomDragLayer = () => {
  const { itemType, isDragging, item, offset } = useDragLayer((monitor) => ({
    item: monitor.getItem(),
    itemType: monitor.getItemType(),
    offset: monitor.getClientOffset(),
    isDragging: monitor.isDragging(),
  }));

  function renderItem() {
    switch (itemType) {
      case DMS_NODE_TYPE:
        return <DMSGridRowPreview record={item?.data?.record} />;
      default:
        return null;
    }
  }

  if (!isDragging) {
    return null;
  }

  const style = getItemStyles(offset);
  return (
    <div className={styles.container}>
      <Box className={styles.preview} bgColor="body" shadow="sm" style={style}>
        {renderItem()}
      </Box>
    </div>
  );
};
