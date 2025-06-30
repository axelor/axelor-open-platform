import { useCallback, useMemo } from "react";
import { Box, Overflow, OverflowItem } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";
import { Tag } from "@/views/form/widgets";
import { DataRecord } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import { ManyToOne } from "@/views/grid/widgets";

const getItemKey = (record: DataRecord) => record.id!;

export function TagSelect(props: GridColumnProps) {
  const { data } = props;
  const schema = data as Schema;
  const hasToOne = toKebabCase(schema.serverType || schema.widget).endsWith(
    "-to-one",
  );

  if (hasToOne) {
    return <ManyToOne {...props} />;
  }

  return <MultipleTagSelect {...props} />;
}

function MultipleTagSelect(props: GridColumnProps) {
  const { rawValue, data: schema } = props;

  const renderItem = useCallback(
    (item: DataRecord) => <Tag schema={schema as Schema} record={item} />,
    [schema],
  );

  const list = useMemo(
    () =>
      (rawValue && !Array.isArray(rawValue) ? [rawValue] : rawValue || []).map(
        (item: DataRecord, ind: number) => ({
          ...item,
          id: item.id ?? `item_${ind}`,
        }),
      ) as DataRecord[],
    [rawValue],
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
