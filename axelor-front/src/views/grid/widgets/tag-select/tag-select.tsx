import { useCallback, useMemo } from "react";
import { Box, Overflow, OverflowItem } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";
import set from "lodash/set";

import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";
import { RelationalValue, SelectionTag, Tag } from "@/views/form/widgets";
import { DataRecord } from "@/services/client/data.types";
import { Field, Schema } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import formTagSelectStyles from "@/views/form/widgets/tag-select/tag-select.module.scss";

const getItemKey = (record: DataRecord) => record.id!;

export function TagSelect(props: GridColumnProps) {
  const { rawValue: value, data } = props;
  const schema = data as Schema;
  const hasToOne = toKebabCase(schema.serverType || schema.widget).endsWith(
    "-to-one",
  );

  if (hasToOne) {
    return value && <SingleTagSelect {...props} />;
  }

  return <MultipleTagSelect {...props} />;
}

function SingleTagSelect(props: GridColumnProps) {
  const { rawValue, record, data: schema } = props;

  const value = useMemo(() => {
    return Object.keys(record).reduce(
      (_value, key) => {
        const prefix = `${schema.name}.`;
        // To merge dotted field value from record
        // Like for colorField, as it's value is fetched as dotted field in grid search
        // ex: record: { "product": { "id": 1 }, "product.color": "red"} to { "product": { "id": 1, "color": "red"}}
        if (key.startsWith(prefix)) {
          const fieldName = key.slice(prefix.length);
          const fieldValue = record[key];
          return set(_value, fieldName, fieldValue);
        }
        return _value;
      },
      { ...rawValue },
    );
  }, [schema.name, rawValue, record]);

  return <Tag schema={schema} record={value} />;
}

function MultipleTagSelect(props: GridColumnProps) {
  const { rawValue, data: schema } = props;

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
