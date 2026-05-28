import { type ReactNode, useCallback } from "react";

import { Box, Overflow, OverflowItem } from "@axelor/ui";

import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";
import { Selection } from "@/services/client/meta.types";

import { Tag } from "./tag";

const getItemKey = (item: Selection) => String(item.value!);

type SelectionListProps = {
  items: Selection[];
  renderValue?: (props: { option: Selection }) => ReactNode;
};

export function SelectionList(props: SelectionListProps) {
  const { items, renderValue } = props;

  const renderItem = useCallback(
    (item: Selection) =>
      renderValue ? (
        <>{renderValue({ option: item })}</>
      ) : (
        <Tag title={item.title} color={item.color} />
      ),
    [renderValue],
  );

  return (
    <Overflow>
      <Box d="flex" flexWrap="nowrap" gap={4} w={100}>
        {items.map((item) => (
          <OverflowItem key={getItemKey(item)} id={getItemKey(item)}>
            <Box>{renderItem(item)}</Box>
          </OverflowItem>
        ))}
        <OverflowMenu<Selection>
          items={items}
          getItemKey={getItemKey}
          renderItem={renderItem}
        />
      </Box>
    </Overflow>
  );
}
