import { PrimitiveAtom, useAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import { useCallback, useEffect, useMemo } from "react";
import { useSearchParams } from "react-router-dom";

import { Box, Button, SelectOptionProps } from "@axelor/ui";

import { Select, SelectProps } from "@/components/select";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { MenuItem, SearchView } from "@/services/client/meta.types";

import { SelectionTag } from "../form/widgets";
import { fetchMenus } from "./utils";

import styles from "./search-objects.module.scss";

function ActionM2O({
  value,
  parent,
  actionMenus,
  ...props
}: {
  actionMenus: SearchView["actionMenus"];
  parent?: DataRecord | null;
} & Pick<SelectProps<MenuItem, false>, "placeholder" | "value" | "onChange">) {
  const parentName = parent?.name;
  const fetchOptions = useCallback(async () => {
    const menus =
      actionMenus?.filter((menu) => menu.parent === parentName) ?? [];
    const fetchedMenus = (await fetchMenus(parentName)) ?? [];
    return [...menus, ...fetchedMenus];
  }, [actionMenus, parentName]);
  return (
    <Select
      {...props}
      options={[]}
      value={value ?? null}
      optionKey={(x) => x.name}
      optionLabel={(x) => x.title}
      optionEqual={(x, y) => x.name === y.name}
      fetchOptions={fetchOptions}
    />
  );
}

export type SearchObjectsState = {
  selectValue: DataRecord[];
  actionCategory: MenuItem | null;
  actionSubCategory: MenuItem | null;
  action: MenuItem | null;
};

export function SearchObjects({
  stateAtom,
  selects = [],
  actionMenus,
  hasActions = true,
  onSearch,
  onClear,
  onGo,
}: {
  stateAtom: PrimitiveAtom<SearchObjectsState>;
  selects: SearchView["selects"];
  actionMenus: SearchView["actionMenus"];
  hasActions?: boolean;
  onSearch: () => void;
  onClear?: () => void;
  onGo?: () => void;
}) {
  const [selectValue, onSelectChange] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("selectValue")),
      [stateAtom],
    ),
  );
  const [actionCategory, setActionCategory] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("actionCategory")),
      [stateAtom],
    ),
  );
  const [actionSubCategory, setActionSubCategory] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("actionSubCategory")),
      [stateAtom],
    ),
  );
  const [action, setAction] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("action")), [stateAtom]),
  );
  const [searchParams] = useSearchParams();

  const handleActionCategory = useCallback(
    (value: MenuItem | null) => {
      setActionCategory(value);
      setActionSubCategory(null);
      setAction(null);
    },
    [setActionCategory, setActionSubCategory, setAction],
  );

  const handleActionSubCategory = useCallback(
    (value: MenuItem | null) => {
      setActionSubCategory(value);
      setAction(null);
    },
    [setActionSubCategory, setAction],
  );

  const handleAction = useCallback(
    (value: MenuItem | null) => {
      setAction(value);
    },
    [setAction],
  );

  function handleClear() {
    onSelectChange((selects || []).filter((item) => item.selected));
    setActionCategory(null);
    setActionSubCategory(null);
    setAction(null);
    onClear?.();
  }

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<DataRecord>) => {
      return (
        <SelectionTag
          title={<span>{option?.title}</span>}
          color="primary"
          onRemove={() => {
            if (Array.isArray(selectValue)) {
              onSelectChange(
                selectValue.filter((x) => x.model !== option.model),
              );
            }
          }}
        />
      );
    },
    [selectValue, onSelectChange],
  );

  const objects = searchParams.get("objects");
  useEffect(() => {
    if (objects) {
      const list = objects.split(",");
      onSelectChange(
        list
          .map((v) => selects?.find((item) => item.model === v))
          .filter((v) => v) as DataRecord[],
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
              multiple={true}
              placeholder={i18n.get("Search Objects")}
              options={selects}
              optionLabel={(x) => x.title!}
              optionKey={(x) => x.model!}
              optionEqual={(x, y) => x.model === y.model}
              value={selectValue}
              onChange={(value) => {
                onSelectChange(value as DataRecord[]);
              }}
              renderValue={renderValue}
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
                actionMenus={actionMenus}
                placeholder={i18n.get("Action Category")}
                value={actionCategory}
                onChange={(value) => handleActionCategory(value as MenuItem)}
              />
            </Box>
            <Box d="flex" className={styles.select}>
              {actionCategory && (
                <ActionM2O
                  actionMenus={actionMenus}
                  placeholder={i18n.get("Action Sub-Category")}
                  parent={actionCategory}
                  value={actionSubCategory}
                  onChange={(value) =>
                    handleActionSubCategory(value as MenuItem)
                  }
                />
              )}
            </Box>
            <Box d="flex" className={styles.select}>
              {actionSubCategory && (
                <ActionM2O
                  actionMenus={actionMenus}
                  placeholder={i18n.get("Action")}
                  parent={actionSubCategory}
                  value={action}
                  onChange={(value) => handleAction(value as MenuItem)}
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
