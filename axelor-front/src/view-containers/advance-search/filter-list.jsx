import React from "react";
import { Box } from "@axelor/ui";
import { ButtonLink, BooleanCheckBox } from "./editor/components";
import { legacyClassNames } from "@/styles/legacy";

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
      <Box d="flex" alignItems="center" ms={1} me={1}>
        <i className={legacyClassNames("fa", "fa-filter")} />
        <Box as="p" mb={0} p={1} flex={1} fontWeight="bold">
          {title}
        </Box>
      </Box>
      <Box ms={1}>
        <Box w={100} d="flex" flexDirection="column" alignItems="flex-start">
          {items.map((filter) => (
            <FilterListItem
              key={filter.id}
              filter={filter}
              isChecked={active.includes(filter.id)}
              disabled={disabled}
              onClick={onFilterClick}
              onChange={onFilterChange}
            />
          ))}
        </Box>
      </Box>
    </Box>
  );
}

export default React.memo(FilterList);
