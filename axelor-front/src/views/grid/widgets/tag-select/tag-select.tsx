import { useCallback, useMemo } from "react";
import { Box, Overflow, OverflowItem } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";

import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";
import { RelationalValue, SelectionTag } from "@/views/form/widgets";
import { DataRecord } from "@/services/client/data.types";
import { Field } from "@/services/client/meta.types";
import formTagSelectStyles from "@/views/form/widgets/tag-select/tag-select.module.scss";

const getItemKey = (record: DataRecord) => record.id!;

export function TagSelect(props: GridColumnProps) {
  const { rawValue, data: schema } = props;
  const list = useMemo(
    () =>
      (rawValue || []).map((item: DataRecord, ind: number) => ({
        ...item,
        id: item.id ?? `item_${ind}`,
      })) as DataRecord[],
    [rawValue],
  );
  const renderItem = useCallback(
    (item: DataRecord) => (
      <SelectionTag
        {...((schema as Field).imageField && {
          className: formTagSelectStyles.imageTag,
        })}
        title={<RelationalValue schema={schema} value={item} />}
        color={"primary"}
      />
    ),
    [schema],
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
