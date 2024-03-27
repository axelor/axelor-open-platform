import { useCallback, useMemo } from "react";
import { Box, Overflow, OverflowItem } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";
import { Field } from "@/services/client/meta.types";
import { SelectionTag } from "@/views/form/widgets";
import { DataRecord } from "@/services/client/data.types";

const getItemKey = (record: DataRecord) => record.id!;

export function TagSelect(props: GridColumnProps) {
  const { rawValue, data } = props;
  const { targetName = "" } = data as Field;
  const list = useMemo(
    () =>
      (rawValue || []).map((item: DataRecord, ind: number) => ({
        ...item,
        id: item.id ?? `item_${ind}`,
      })) as DataRecord[],
    [rawValue],
  );

  const getTitle = useCallback(
    (record: DataRecord) => record[`$t:${targetName}`] ?? record[targetName],
    [targetName],
  );

  const renderItem = useCallback(
    (item: DataRecord) => (
      <SelectionTag title={getTitle(item)} color={"primary"} />
    ),
    [getTitle],
  );

  return (
    <Overflow>
      <Box d="flex" flexWrap="nowrap" gap={4} w={100}>
        {list.map((item) => (
          <OverflowItem key={item.id} id={String(item.id!)}>
            <Box>{renderItem(item)}</Box>
          </OverflowItem>
        ))}
        <OverflowMenu<DataRecord>
          items={list}
          getItemKey={getItemKey}
          renderItem={renderItem}
        />
      </Box>
    </Overflow>
  );
}
