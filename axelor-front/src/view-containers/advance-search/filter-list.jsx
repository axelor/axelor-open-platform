import React from "react";

import { Box } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { BooleanCheckBox, ButtonLink } from "./editor/components";

const FilterListItem = React.memo(function FilterListItem({
  filter,
  disabled,
  isChecked,
  onClick,
  onChange,
}) {
  const { title } = filter;

  function handleChange(value) {
    onChange(filter, value);
  }

  return (
    <Box d="flex" alignItems="center">
      <BooleanCheckBox
        name={title.replace(" ", "_").toLowerCase()}
        value={isChecked}
        onChange={handleChange}
        inline
        isDisabled={disabled}
      />
      <ButtonLink
        title={title}
        position="relative"
        onClick={() => (!disabled || isChecked) && onClick(filter, !isChecked)}
        {...(disabled && !isChecked ? { color: "muted" } : {})}
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
}) {
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
            isChecked={active.includes(filter.id)}
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
