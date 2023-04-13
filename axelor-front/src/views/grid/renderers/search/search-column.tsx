import { Field } from "@/services/client/meta.types";
import { toKebabCase } from "@/utils/names";
import { Box, Input, Select } from "@axelor/ui";
import { GridColumn } from "@axelor/ui/grid";
import { ChangeEvent, KeyboardEvent, useState } from "react";
import { i18n } from "@/services/client/i18n";
import { SearchState, useGridSearchFieldScope } from "./scope";
import styles from "./search-column.module.scss";

function SearchInput({ column }: { column: GridColumn }) {
  const { search, setSearch, onSearch } = useGridSearchFieldScope();
  const field = column as Field;
  const [value, setValue] = useState<string>(search[field.name] || "");
  const [focus, setFocus] = useState(false);

  function applySearch(value: any) {
    onSearch &&
      onSearch({
        ...search,
        [field.name]: value,
      });
  }

  function handleChange(e: ChangeEvent<HTMLInputElement>) {
    setValue(e.target.value);
  }

  function handleFocus() {
    setFocus(true);
  }

  function handleBlur(e: ChangeEvent<HTMLInputElement>) {
    setFocus(false);
    setSearch &&
      setSearch((state: SearchState) => {
        const value = e.target.value;
        return {
          ...state,
          [field.name]: value,
        };
      });
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Tab") {
      e.stopPropagation();
    }
    if (e.key === "Enter") {
      e.preventDefault();
      applySearch(value);
    }
  }

  if (field.selectionList) {
    const selected = field.selectionList.find((opt) => opt.value === value);
    return (
      <Box w={100} className={styles.select} d="flex">
        <Select
          value={selected ?? null}
          placeholder={focus ? i18n.get("Search...") : ""}
          onChange={({ value }) => {
            setValue(value);
            applySearch(value);
          }}
          options={field.selectionList}
          optionLabel="title"
          optionValue="value"
          onFocus={handleFocus}
          onBlur={() => setFocus(false)}
          icons={
            selected
              ? [
                  {
                    icon: "close",
                    onClick: () => {
                      setValue('');
                      applySearch(null);
                    },
                  },
                ]
              : []
          }
        />
      </Box>
    );
  }

  return (
    <Box className={styles.container} d="flex">
      <Input
        type="text"
        border={false}
        value={value || ""}
        onChange={handleChange}
        placeholder={focus ? i18n.get("Search...") : ""}
        onKeyDown={handleKeyDown}
        onFocus={handleFocus}
        onBlur={handleBlur}
      />
    </Box>
  );
}

export function SearchColumn({ column }: { column: GridColumn }) {
  const field = column as Field;
  if (
    column.searchable === false ||
    field.transient ||
    field.json ||
    field.encrypted ||
    ["one-to-many", "many-to-many"].includes(toKebabCase(field.type))
  ) {
    return <Box h={100} w={100} bg="light" />;
  }

  return <SearchInput column={column} />;
}
