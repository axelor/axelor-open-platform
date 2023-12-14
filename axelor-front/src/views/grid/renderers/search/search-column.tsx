import { PrimitiveAtom, useAtom } from "jotai";
import { ChangeEvent, KeyboardEvent, useMemo } from "react";

import { Box, Input } from "@axelor/ui";
import { GridColumn } from "@axelor/ui/grid";

import { Select } from "@/components/select";
import { i18n } from "@/services/client/i18n";
import { Field } from "@/services/client/meta.types";
import { focusAtom } from "@/utils/atoms";

import { SearchState } from "./types";

import styles from "./search-column.module.scss";

export interface SearchColumnProps {
  column: GridColumn;
  dataAtom: PrimitiveAtom<SearchState>;
  onSearch?: () => void;
}

function SearchInput({ column, dataAtom, onSearch }: SearchColumnProps) {
  const field = column as Field;
  const [value, setValue] = useAtom(
    useMemo(
      () =>
        focusAtom(
          dataAtom,
          (state) => state[column.name] ?? "",
          (state, value) => ({ ...state, [column.name]: value }),
        ),
      [column.name, dataAtom],
    ),
  );

  function applySearch() {
    onSearch?.();
  }

  function handleChange(e: ChangeEvent<HTMLInputElement>) {
    setValue(e.target.value);
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (
      [
        "Enter",
        "Tab",
        "ArrowRight",
        "ArrowLeft",
        "ArrowUp",
        "ArrowDown",
      ].includes(e.key)
    ) {
      e.stopPropagation();
    }
    if (e.key === "Enter") {
      e.preventDefault();
      applySearch();
    }
  }

  if (field.selectionList) {
    const selected = field.selectionList.find((opt) => opt.value === value);
    return (
      <Box w={100} className={styles.select} d="flex">
        <Select
          multiple={false}
          value={selected ?? null}
          placeholder={i18n.get("Search...")}
          onChange={(value) => {
            setValue(value?.value ?? "");
            applySearch();
          }}
          options={field.selectionList}
          optionKey={(x) => x.value!}
          optionLabel={(x) => x.title!}
          optionEqual={(x, y) => x.value === y.value}
        />
      </Box>
    );
  }

  return (
    <Box className={styles.container} d="flex">
      <Input
        type="text"
        value={value || ""}
        onChange={handleChange}
        placeholder={i18n.get("Search...")}
        onKeyDown={handleKeyDown}
      />
    </Box>
  );
}

export function SearchColumn(props: SearchColumnProps) {
  const { column } = props;
  if (column.searchable === false) {
    return <Box h={100} w={100} />;
  }

  return <SearchInput {...props} />;
}
