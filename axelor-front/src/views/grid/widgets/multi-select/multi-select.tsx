import { useCallback, useMemo } from "react";
import { GridColumnProps } from "@axelor/ui/grid";

import { Box, Overflow, OverflowItem } from "@axelor/ui";
import { OverflowMenu } from "@/components/overflow-menu/overflow-menu";
import { getMultiValues } from "@/views/form/widgets/selection/utils";

import { SingleSelectValue } from "../single-select/single-select-value";

export function MultiSelect(props: GridColumnProps) {
  const { data, rawValue } = props;
  const items = useMemo(() => getMultiValues(rawValue), [rawValue]);

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
