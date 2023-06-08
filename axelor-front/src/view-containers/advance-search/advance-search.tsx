import {
  Box,
  ClickAwayListener,
  Divider,
  FocusTrap,
  Popper,
  TextField,
  useTheme,
} from "@axelor/ui";
import { GridColumn } from "@axelor/ui/grid";
import {
  MaterialIcon,
  MaterialIconProps,
} from "@axelor/ui/icons/meterial-icon";
import { PrimitiveAtom, useAtom, useAtomValue, useSetAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import { useAtomCallback } from "jotai/utils";
import {
  KeyboardEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { dialogs } from "@/components/dialogs";
import { useSession } from "@/hooks/use-session";
import { useShortcut } from "@/hooks/use-shortcut";
import { useTabs } from "@/hooks/use-tabs";
import { SearchOptions, SearchResult } from "@/services/client/data";
import { DataStore } from "@/services/client/data-store";
import { i18n } from "@/services/client/i18n";
import { removeFilter, saveFilter } from "@/services/client/meta";
import {
  AdvancedSearchAtom,
  SavedFilter,
  SearchFilter,
  View,
} from "@/services/client/meta.types";
import { download } from "@/utils/download";
import { useViewAction, useViewTab } from "../views/scope";

import { Chip } from "@/views/form/widgets";
import styles from "./advance-search.module.scss";
import { Editor } from "./editor";
import { getEditorDefaultState } from "./editor/editor";
import { FilterList } from "./filter-list";
import {
  focusSearchTabIdAtom,
  getFreeSearchCriteria,
  prepareAdvanceSearchQuery,
} from "./utils";

export interface AdvanceSearchProps {
  dataStore: DataStore;
  stateAtom: AdvancedSearchAtom;
  customSearch?: boolean;
  freeSearch?: string;
  items?: View["items"];
  onSearch: (options?: SearchOptions) => Promise<SearchResult | undefined>;
}

export function AdvanceSearch({
  dataStore,
  stateAtom,
  items,
  freeSearch = "all",
  customSearch = true,
  onSearch,
}: AdvanceSearchProps) {
  const [open, setOpen] = useState(false);
  const { data: sessionInfo } = useSession();
  const setEditor = useSetAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("editor")), [stateAtom])
  );
  const [filters, setFilters] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("filters")), [stateAtom])
  );
  const [domains, setDomains] = useAtom(
    useMemo(() => focusAtom(stateAtom, (o) => o.prop("domains")), [stateAtom])
  );
  const freeSearchTextAtom = useMemo(
    () => focusAtom(stateAtom, (o) => o.prop("searchText")),
    [stateAtom]
  );
  const searchTextLabel = useAtomValue(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("searchTextLabel")),
      [stateAtom]
    )
  );
  const [filterType, setFilterType] = useAtom(
    useMemo(
      () => focusAtom(stateAtom, (o) => o.prop("filterType")),
      [stateAtom]
    )
  );

  const containerRef = useRef<HTMLDivElement>(null);

  const { name, params } = useViewAction();
  const filterView = (params || {})["search-filters"] || `act:${name}`;
  const rtl = useTheme().dir === "rtl";
  const advanceSearchConfig = sessionInfo?.view?.advanceSearch;

  const handleOpen = useCallback(() => setOpen(true), []);
  const handleClose = useCallback(() => setOpen(false), []);

  const handleApply = useAtomCallback(
    useCallback(
      (get, set, hasEditorApply?: boolean) => {
        const state = get(stateAtom);
        set(stateAtom, prepareAdvanceSearchQuery(state, hasEditorApply));
        onSearch?.();
      },
      [stateAtom, onSearch]
    )
  );

  const handleClear = useAtomCallback(
    useCallback(
      (get, set, shouldApply: boolean = true) => {
        const state = get(stateAtom);
        const { domains, filters } = state;
        set(stateAtom, {
          ...state,
          search: {},
          domains: domains?.map((d) =>
            d.checked ? { ...d, checked: false } : d
          ),
          filters: filters?.map((f) =>
            f.checked ? { ...f, checked: false } : f
          ),
          searchText: "",
          searchTextLabel: "",
          filterType: "all",
          editor: getEditorDefaultState(),
        });
        shouldApply && handleApply();
        handleClose();
      },
      [stateAtom, handleApply, handleClose]
    )
  );

  const handleFreeSearch = useAtomCallback(
    useCallback(
      (get, set) => {
        const state = get(stateAtom);
        const { fields, searchText } = state;

        if (!searchText) return handleApply();

        const freeSearchList = freeSearch.split(",");
        const viewItems = items?.filter((item) => {
          if ((item as GridColumn).searchable === false) return false;
          if (freeSearch === "all") {
            return true;
          }
          if (freeSearch === "none") {
            return false;
          }
          return freeSearch ? freeSearchList.includes(item.name!) : true;
        });

        set(stateAtom, {
          ...state,
          query: {
            _archived: false,
            _searchText: searchText,
            operator: "or",
            criteria: getFreeSearchCriteria(searchText, viewItems, fields),
          },
        });

        onSearch?.();
      },
      [stateAtom, handleApply, onSearch, freeSearch, items]
    )
  );

  const handleEditorApply = useCallback(() => {
    handleApply();
    handleClose();
  }, [handleApply, handleClose]);

  const handleDomainCheck = useCallback(
    (filter: SearchFilter | SavedFilter, type?: "click" | "change") => {
      setDomains((domains) =>
        domains?.map((d) =>
          d === (filter as SearchFilter)
            ? { ...d, checked: !d.checked }
            : type === "click"
            ? { ...d, checked: false }
            : d
        )
      );
      type === "click" && handleApply();
    },
    [setDomains, handleApply]
  );

  const handleFilterCheck = useCallback(
    (filter: SearchFilter | SavedFilter, type?: "click" | "change") => {
      const isSingle = type === "click" && !filter.checked;
      setFilterType(isSingle ? "single" : "all");
      setFilters((filters) =>
        filters?.map((f) =>
          f === (filter as SavedFilter)
            ? { ...f, checked: !f.checked }
            : type === "click"
            ? { ...f, checked: false }
            : f
        )
      );
      if (type === "click") {
        setDomains((domains) =>
          domains?.map((d) => (d.checked ? { ...d, checked: false } : d))
        );
        try {
          setEditor(() =>
            filter.checked
              ? getEditorDefaultState()
              : {
                  ...filter,
                  ...JSON.parse((filter as SavedFilter).filterCustom || "{}"),
                }
          );
          handleApply(true);
        } catch {}
      }
    },
    [setFilters, setFilterType, setDomains, setEditor, handleApply]
  );

  const handleFilterSave = useCallback(
    async (filter: SavedFilter) => {
      const isNew = !filter.id;
      const savedFilter = await saveFilter({ ...filter, filterView });
      setFilters((_filters) => {
        const filters = [...(_filters || [])];
        if (isNew) {
          filters.push(savedFilter);
        }
        return filters.map((f) =>
          f.id === savedFilter.id
            ? { ...f, ...savedFilter, checked: true }
            : {
                ...f,
                checked: false,
              }
        );
      });
      if (isNew) {
        setFilterType("single");
        setEditor((editor) => ({ ...editor, id: savedFilter.id }));
      }
      handleApply(true);
    },
    [filterView, setFilters, setFilterType, setEditor, handleApply]
  );

  const handleFilterRemove = useCallback(
    async (filter: SavedFilter) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: i18n.get(`Would you like to remove the filter?`),
      });
      if (!confirmed) return;

      const res = await removeFilter(filter);
      res &&
        setFilters((filters) => filters!.filter((f) => f.id !== filter.id));

      setFilterType("all");
      setEditor(getEditorDefaultState());
      handleApply();
    },
    [setFilters, setEditor, setFilterType, handleApply]
  );

  const handleExport = useCallback(
    (exportFull?: boolean) => {
      dataStore
        .export(
          exportFull
            ? { fields: [] }
            : items
            ? {
                fields: items
                  ?.filter(
                    (item) =>
                      item.name &&
                      (item as GridColumn).visible !== false &&
                      (item as GridColumn).searchable !== false
                  )
                  .map((item) => item.name) as string[],
              }
            : {}
        )
        .then(({ fileName }) => {
          download(
            `ws/rest/${dataStore.model}/export/${fileName}?fileName=${fileName}`,
            fileName
          );
        });
    },
    [dataStore, items]
  );

  const inputIcons = useMemo(
    () =>
      [
        {
          key: "open",
          icon: "arrow_drop_down",
          className: styles.icon,
          onClick: () => handleOpen(),
        },
        {
          key: "clear",
          icon: "clear",
          className: styles.icon,
          onClick: () => handleClear(false),
        },
        {
          key: "search",
          icon: "search",
          className: styles.icon,
          onClick: () => handleFreeSearch(),
        },
      ] as SearchInputIconProps[],
    [handleOpen, handleClear, handleFreeSearch]
  );

  return (
    <Box className={styles.root} ref={containerRef}>
      <SearchInput
        readonly={freeSearch === "none"}
        label={searchTextLabel}
        icons={inputIcons}
        valueAtom={freeSearchTextAtom}
        onClear={handleClear}
        onSearch={handleFreeSearch}
      />
      <Popper
        bg="body"
        open={open}
        target={containerRef.current}
        placement={`bottom-${rtl ? "end" : "start"}`}
      >
        <ClickAwayListener onClickAway={handleClose}>
          <Box {...(rtl ? { dir: "rtl" } : {})} className={styles.popper} p={2}>
            <FocusTrap initialFocus={false} enabled={open}>
              <Box d="flex" flexDirection="column">
                <Box d="flex" alignItems="center">
                  <Box as="p" mb={0} p={1} flex={1} fontWeight="bold">
                    {i18n.get("Advanced Search")}
                  </Box>
                  <Box as="span" className={styles.icon} onClick={handleClose}>
                    <MaterialIcon icon="close" />
                  </Box>
                </Box>
                <Divider />
                <Box d="flex" alignItems="flex-start" mb={customSearch ? 0 : 1}>
                  {(domains || []).length > 0 && (
                    <FilterList
                      title={i18n.get("Filters")}
                      items={domains}
                      disabled={filterType === "single"}
                      onFilterCheck={handleDomainCheck}
                    />
                  )}
                  {(filters || []).length > 0 && (
                    <FilterList
                      title={i18n.get("My Filters")}
                      items={filters}
                      disabled={filterType === "single"}
                      onFilterCheck={handleFilterCheck}
                    />
                  )}
                  {!customSearch && !domains?.length && !filters?.length && (
                    <Box as="p" mb={0} p={1} flex={1}>
                      {i18n.get("No filters available")}
                    </Box>
                  )}
                </Box>

                {customSearch &&
                  ((filters?.length ?? 0) > 0 ||
                    (domains?.length ?? 0) > 0) && <Divider mt={1} />}

                {customSearch && (
                  <Editor
                    stateAtom={stateAtom}
                    canShare={advanceSearchConfig?.share}
                    canExportFull={advanceSearchConfig?.exportFull}
                    items={items}
                    onApply={handleEditorApply}
                    onClear={handleClear}
                    onExport={handleExport}
                    onSave={handleFilterSave}
                    onDelete={handleFilterRemove}
                  />
                )}
              </Box>
            </FocusTrap>
          </Box>
        </ClickAwayListener>
      </Popper>
    </Box>
  );
}

interface SearchInputIconProps extends MaterialIconProps {
  key: string;
}

function SearchInput({
  readonly,
  label,
  icons,
  valueAtom,
  onClear,
  onSearch,
}: {
  readonly?: boolean;
  label?: string;
  icons?: SearchInputIconProps[];
  valueAtom: PrimitiveAtom<string | undefined>;
  onClear?: () => void;
  onSearch?: () => void;
}) {
  const [value = "", setValue] = useAtom(valueAtom);
  const searchRef = useRef<HTMLInputElement>(null);
  const { active } = useTabs();
  const tab = useViewTab();
  const canHandle = useCallback(() => active === tab, [active, tab]);

  useShortcut({
    key: "f",
    altKey: true,
    canHandle,
    action: useCallback(() => searchRef.current?.focus(), []),
  });

  const [focusSearchTabId, setFocusSearchTabId] = useAtom(focusSearchTabIdAtom);

  useEffect(() => {
    if (focusSearchTabId === tab.id) {
      setFocusSearchTabId(null);
      searchRef.current?.focus();
    }
  }, [tab.id, focusSearchTabId, setFocusSearchTabId]);

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      onSearch?.();
    }
  }

  if (label) {
    return (
      <Box rounded border d="flex" p={1} pe={2}>
        <Box className={styles.chipList} d="flex" flex={1}>
          <Chip title={label} color="indigo" onRemove={() => onClear?.()} />
        </Box>
        <Box
          d="flex"
          justifyContent="flex-end"
          alignItems="center"
          position="relative"
        >
          {icons?.map((icon, ind) => (
            <MaterialIcon
              key={ind}
              icon={icon.icon}
              onClick={icon.onClick}
              className={icon.className}
            />
          ))}
        </Box>
      </Box>
    );
  }
  return (
    <TextField
      ref={searchRef}
      name="search"
      icons={icons}
      readOnly={readonly}
      value={value}
      onChange={(e) => setValue(e.target.value)}
      {...(readonly
        ? {
            onClick: (e) =>
              icons?.find(({ key }) => key === "open")?.onClick?.(e),
          }
        : {
            onKeyDown: handleKeyDown,
          })}
    />
  );
}
