import React from "react";
import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { BooleanCheckBox, ButtonLink } from "./editor/components";
import { SavedFilter } from "@/services/client/meta.types";

interface FilterListProps {
  title: string;
  active?: number[];
  items?: SavedFilter[];
  disabled?: boolean;
  onFilterClick?: (filter: SavedFilter, isChecked: boolean) => void;
  onFilterChange?: (filter: SavedFilter, isChecked: boolean) => void;
}

const FilterListItem = React.memo(function FilterListItem({
  filter,
  disabled,
  checked,
  onClick,
  onChange,
}: {
  filter: SavedFilter;
  disabled?: boolean;
  checked?: boolean;
  onClick?: FilterListProps["onFilterClick"];
  onChange?: FilterListProps["onFilterChange"];
}) {
  const { title } = filter;

  function handleChange(value: boolean) {
    onChange?.(filter, value);
  }

  return (
    <Box d="flex" alignItems="center">
      <BooleanCheckBox
        name={title.replace(" ", "_").toLowerCase()}
        value={checked}
        onChange={handleChange}
        inline
        isDisabled={disabled}
        {...({} as any)}
      />
      <ButtonLink
        title={title}
        position="relative"
        onClick={() => (!disabled || checked) && onClick?.(filter, !checked)}
        {...(disabled && !checked ? { color: "muted" } : {})}
      />
    </Box>
  );
});

export function FilterList({
  title,
  active = [],
  items = [],
  disabled,
  onFilterClick,
  onFilterChange,
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
            key={filter.id ?? `filter_${ind}`}
            filter={filter}
            checked={active.includes(filter.id)}
            disabled={disabled}
            onClick={onFilterClick}
            onChange={onFilterChange}
          />
        ))}
      </Box>
    </Box>
  );
}

export default React.memo(FilterList);
