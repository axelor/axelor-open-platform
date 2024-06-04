import React, {useMemo} from "react";
import { Box, Input, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

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
        style={{ textAlign: "left" }}
        onClick={() => (!disabled || checked) && onCheck?.(filter, "click")}
        {...(disabled && !checked ? { color: "secondary" } : {})}
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

  const displayTitle = useMemo(() => {
    let counter = "";
    const activeFilterNbr = items?.filter((f) => f.checked).length;
    if (activeFilterNbr > 0) {
      counter = ` (${activeFilterNbr})`
    }
    return `${title}${counter}`
  }, [title, items]);
  
  return (
    <Box flexDirection="column" alignItems="baseline" w={100}>
      <Box d="flex" alignItems="center">
        <MaterialIcon icon="filter_list" />
        <Box as="p" mb={0} p={1} flex={1} fontWeight="bold">
          {displayTitle}
        </Box>
      </Box>
      <Box
        style={{ width: "fit-content", maxHeight: 300, overflow: "hidden", overflowY: "scroll" }}
        px={1}
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
