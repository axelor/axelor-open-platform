import { Box, Button, Select, SelectProps } from "@axelor/ui";
import { useCallback, useEffect, useMemo } from "react";
import { PrimitiveAtom, useAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import { useSearchParams } from "react-router-dom";

import { SearchView } from "@/services/client/meta.types";
import { i18n } from "@/services/client/i18n";
import { DataRecord } from "@/services/client/data.types";
import { fetchMenus } from "./utils";
import styles from "./search-objects.module.scss";

function ActionM2O({
  value,
  parent,
  ...props
}: SelectProps & { parent?: DataRecord | null }) {
  const fetchOptions = useCallback(() => fetchMenus(parent?.name), [parent]);
  return (
    <Select
      {...props}
      value={value ?? null}
      optionLabel="title"
      optionValue="name"
      fetchOptions={fetchOptions}
      icons={
        value
          ? [{ icon: "close", onClick: () => props.onChange?.(null) }]
          : [{ icon: "arrow_drop_down" }]
      }
    />
  );
}

export type SearchObjectsState = {
  selectValue: DataRecord[];
  actionCategory: DataRecord | null;
  actionSubCategory: DataRecord | null;
  action: DataRecord | null;
};

export function SearchObjects({
  stateAtom,
  selects,
  hasActions = true,
  onSearch,
  onClear,
  onGo,
}: {
  stateAtom: PrimitiveAtom<SearchObjectsState>;
  selects: SearchView["selects"];
  hasActions?: boolean;
  onSearch: () => void;
  onClear?: () => void;
  onGo?: () => void;
}) {
  const [selectValue, onSelectChange] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("selectValue")),
      [stateAtom]
    )
  );
  const [actionCategory, setActionCategory] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("actionCategory")),
      [stateAtom]
    )
  );
  const [actionSubCategory, setActionSubCategory] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("actionSubCategory")),
      [stateAtom]
    )
  );
  const [action, setAction] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("action")), [stateAtom])
  );
  const [searchParams] = useSearchParams();

  const handleActionCategory = useCallback(
    (value: DataRecord | null) => {
      setActionCategory(value);
      setActionSubCategory(null);
      setAction(null);
    },
    [setActionCategory, setActionSubCategory, setAction]
  );

  const handleActionSubCategory = useCallback(
    (value: DataRecord | null) => {
      setActionSubCategory(value);
      setAction(null);
    },
    [setActionSubCategory, setAction]
  );

  const handleAction = useCallback(
    (value: DataRecord | null) => {
      setAction(value);
    },
    [setAction]
  );

  function handleClear() {
    onSelectChange((selects || []).filter((item) => item.selected));
    setActionCategory(null);
    setActionSubCategory(null);
    setAction(null);
    onClear?.();
  }

  const objects = searchParams.get("objects");
  useEffect(() => {
    if (objects) {
      const list = objects.split(",");
      onSelectChange(
        list
          .map((v) => selects?.find((item) => item.model === v))
          .filter((v) => v) as DataRecord[]
      );
    } else {
      onSelectChange((selects || []).filter((item) => item.selected));
    }
  }, [objects, selects, onSelectChange]);

  return (
    <Box
      className={styles["search-objects"]}
      d="flex"
      p={3}
      my={2}
      border
      rounded
    >
      <Box d="flex" flex={1}>
        <Box className={styles["search-section"]}>
          <Box d="flex" className={styles.select} w={100}>
            <Select
              value={selectValue}
              onChange={onSelectChange}
              isMulti
              options={selects}
              optionLabel="title"
              optionValue="model"
              placeholder={i18n.get("Search Objects")}
              icons={[
                {
                  icon: "arrow_drop_down",
                },
              ]}
            />
          </Box>
          <Box d="flex">
            <Button variant="primary" w={100} onClick={() => onSearch()}>
              {i18n.get("Search")}
            </Button>
          </Box>
          <Box d="flex">
            <Button variant="primary" w={100} onClick={() => handleClear()}>
              {i18n.get("Clear")}
            </Button>
          </Box>
        </Box>
      </Box>
      {hasActions && (
        <Box d="flex" flex={1}>
          <Box className={styles["actions-section"]}>
            <Box d="flex" className={styles.select}>
              <ActionM2O
                placeholder={i18n.get("Action Category")}
                value={actionCategory}
                onChange={handleActionCategory}
              />
            </Box>
            <Box d="flex" className={styles.select}>
              {actionCategory && (
                <ActionM2O
                  placeholder={i18n.get("Action Sub-Category")}
                  parent={actionCategory}
                  value={actionSubCategory}
                  onChange={handleActionSubCategory}
                />
              )}
            </Box>
            <Box d="flex" className={styles.select}>
              {actionSubCategory && (
                <ActionM2O
                  placeholder={i18n.get("Action")}
                  parent={actionSubCategory}
                  value={action}
                  onChange={handleAction}
                />
              )}
            </Box>
            <Box d="flex">
              <Button variant="primary" w={100} onClick={onGo}>
                {i18n.get("Go")}
              </Button>
            </Box>
          </Box>
        </Box>
      )}
    </Box>
  );
}
