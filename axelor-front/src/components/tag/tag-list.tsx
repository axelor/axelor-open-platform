import { useCallback, useMemo } from "react";

import { Box, Overflow, OverflowItem } from "@axelor/ui";

import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";
import { DataRecord } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";

import { RelationalTag } from "./relational-tag";

const getItemKey = (record: DataRecord) => record.id!;

type TagListProps = {
  items: DataRecord[];
  schema: Schema;
  onClick?: (record: DataRecord) => void;
  onRemove?: (record: DataRecord) => void;
};

export function TagList(props: TagListProps) {
  const { items: rawItems, schema, onClick, onRemove } = props;

  const renderItem = useCallback(
    (item: DataRecord) => (
      <RelationalTag
        schema={schema}
        value={item}
        onClick={onClick}
        onRemove={onRemove}
      />
    ),
    [schema, onClick, onRemove],
  );

  const items = useMemo(
    () =>
      (rawItems ?? []).map((item: DataRecord, ind: number) => ({
        ...item,
        id: item.id ?? `item_${ind}`,
      })) as DataRecord[],
    [rawItems],
  );

  return (
    <Overflow>
      <Box d="flex" flexWrap="nowrap" gap={4} w={100}>
        {items.map((item) => (
          <OverflowItem key={item.id} id={String(item.id!)}>
            <Box>{renderItem(item)}</Box>
          </OverflowItem>
        ))}
        <OverflowMenu<DataRecord>
          items={items}
          getItemKey={getItemKey}
          renderItem={renderItem}
        />
      </Box>
    </Overflow>
  );
}
