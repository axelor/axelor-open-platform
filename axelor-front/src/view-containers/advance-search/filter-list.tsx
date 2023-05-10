import React from "react";
import { Box, Input, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { SavedFilter, SearchFilter } from "@/services/client/meta.types";

interface FilterListProps {
  title: string;
  active?: (string | number)[];
  items?: SavedFilter[] | SearchFilter[];
  disabled?: boolean;
  onFilterCheck?: (
    filter: SavedFilter | SearchFilter,
    type?: "click" | "change"
  ) => void;
}

const FilterListItem = React.memo(function FilterListItem({
  filter,
  disabled,
  onCheck,
}: {
  filter: SavedFilter | SearchFilter;
  disabled?: boolean;
  onCheck?: FilterListProps["onFilterCheck"];
}) {
  const { title, checked } = filter;

  return (
    <Box d="flex" alignItems="center">
      <Input
        type="checkbox"
        checked={checked ?? false}
        onChange={() => onCheck?.(filter, "change")}
        disabled={disabled}
        m={0}
      />
      <Button
        variant="link"
        position="relative"
        size="sm"
        onClick={() => (!disabled || checked) && onCheck?.(filter, "click")}
        {...(disabled && !checked ? { color: "muted" } : {})}
      >
        {title}
      </Button>
    </Box>
  );
});

export function FilterList({
  title,
  items = [],
  disabled,
  onFilterCheck,
}: FilterListProps) {
  return (
    <Box flexDirection="column" alignItems="baseline" w={100}>
      <Box d="flex" alignItems="center">
        <MaterialIcon icon="filter_list" />
        <Box as="p" mb={0} p={1} flex={1} fontWeight="bold">
          {title}
        </Box>
      </Box>
      <Box
        px={1}
        w={100}
        d="flex"
        flexDirection="column"
        alignItems="flex-start"
      >
        {items.map((filter, ind) => (
          <FilterListItem
            key={(filter as SavedFilter).id ?? `filter_${ind}`}
            filter={filter}
            disabled={disabled}
            onCheck={onFilterCheck}
          />
        ))}
      </Box>
    </Box>
  );
}

export default React.memo(FilterList);
