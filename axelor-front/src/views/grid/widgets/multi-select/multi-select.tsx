import { useCallback, useMemo } from "react";

import { Box, Overflow, OverflowItem } from "@axelor/ui";
import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";

import { GridColumnProps } from "@axelor/ui/grid";
import { SingleSelectValue } from "../single-select/single-select-value";

export function MultiSelect(props: GridColumnProps) {
  const { data, record } = props;
  const value = record?.[data?.name];
  const items = useMemo(
    () => (value && typeof value === "string" ? value.split(",") : []),
    [value],
  );

  const renderItem = useCallback(
    (item: string) => <SingleSelectValue schema={data} value={item} />,
    [data],
  );

  return (
    <Overflow>
      <Box d="flex" flexWrap="nowrap" gap={4} w={100}>
        {items.map((item) => (
          <OverflowItem key={item} id={String(item)}>
            <Box>{renderItem(item)}</Box>
          </OverflowItem>
        ))}
        <OverflowMenu<string> items={items} renderItem={renderItem} />
      </Box>
    </Overflow>
  );
}
