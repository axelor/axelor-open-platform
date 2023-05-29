import {
  useEffect,
  useCallback,
  useMemo,
  useState,
  useRef,
  CSSProperties,
} from "react";
import { Box, Popper, ReactSelect } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import { i18n } from "@/services/client/i18n";
import { unaccent } from "@/utils/sanitize";
import styles from "./search.module.scss";

const customStyles = {
  menu: (base: CSSProperties) => ({
    ...base,
    margin: 0,
    backgroundColor: "var(--bs-body-bg)",
    boxShadow: "none",
  }),
  menuList: (base: CSSProperties) => ({
    ...base,
    padding: "0.25rem",
    backgroundColor: "var(--bs-body-bg)",
    overflowX: "hidden",
    minHeight: "100%",
  }),
  menuPortal: (base: CSSProperties) => ({
    ...base,
    top: 0,
    width: "100%",
    position: "relative",
  }),
  option: (
    base: CSSProperties,
    state: { isFocused: boolean; isActive: boolean }
  ) => ({
    ...base,
    ...((state.isFocused || state.isActive) && {
      backgroundColor: "var(--bs-gray-200)",
      color: "var(--bs-gray-900)",
    }),
    wordBreak: "break-word",
    padding: `0.25rem 0.375rem`,
    paddingLeft: "0.5rem",
  }),
  group: (base: CSSProperties) => ({ ...base, padding: 0 }),
  groupHeading: (base: CSSProperties) => ({
    ...base,
    textTransform: "none",
    fontSize: "inherit",
    color: "var(--bs-gray-900)",
    fontWeight: "bold",
    paddingLeft: "0.125rem",
    paddingBottom: "0.125rem",
  }),
  control: (base: CSSProperties) => ({
    ...base,
    minHeight: "unset",
    margin: "0.25rem 0.25rem 0.5rem",
  }),
  noOptionsMessage: (base: CSSProperties) => ({ ...base, display: "none" }),
  dropdownIndicator: (base: CSSProperties) => ({ ...base, display: "none" }),
  indicatorSeparator: (base: CSSProperties) => ({ ...base, display: "none" }),
};

export type SearchItem = {
  id?: string;
  title?: string;
  label?: string;
  action?: string;
  category?: string;
  categoryTitle?: string;
  options?: SearchItem[];
};

function filterSearchItem(item: SearchItem, searchText?: string) {
  let text = item.label ?? "";
  let search = searchText ?? "";
  if (search[0] === "/") {
    search = search.substring(1);
    text = item.title ?? text;
  }

  function normalize(text: string) {
    return unaccent(text.toLocaleLowerCase())
      .replace(/\s+/, " ")
      .replace(/[^\w\s]/g, "");
  }

  text = normalize(text.replace("/", ""));

  if (search[0] === '"' || search[0] === "=") {
    search = search.substring(1);
    if (search.indexOf('"') === search.length - 1) {
      search = search.substring(0, search.length - 1);
    }
    return text.indexOf(search) > -1;
  }

  const parts = normalize(search).split(/\s+/);

  for (var i = 0; i < parts.length; i++) {
    if (text.indexOf(parts[i]) === -1) {
      return false;
    }
  }
  return parts.length > 0;
}

function getSearchOptions(items: SearchItem[], searchText?: string) {
  if (!searchText) return [];

  const filterItems = items.filter((item) =>
    filterSearchItem(item, searchText)
  );
  return filterItems
    .filter((x) => x.categoryTitle)
    .reduce((options, item) => {
      const { categoryTitle } = item;
      const optionIndex = options.findIndex(
        (opt) => opt.label === categoryTitle
      );
      if (optionIndex > -1) {
        return options.map((opt, ind) => {
          if (ind === optionIndex) {
            return { ...opt, options: [...(opt.options || []), item] };
          }
          return opt;
        });
      } else {
        return [
          ...options,
          {
            label: categoryTitle,
            options: [item],
          } as SearchItem,
        ];
      }
    }, [] as SearchItem[]);
}

export function NavBarSearch({
  items,
  onClick,
}: {
  items: SearchItem[];
  onClick: (e: any, value: any) => void;
}) {
  const [input, setInput] = useState("");
  const [showSearchInput, setShowSearchInput] = useState(false);
  const [tooltip, setTooltip] = useState(false);
  const [options, setOptions] = useState<SearchItem[]>([]);
  const selectRef = useRef<any>();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  const handleExpandMoreClick = useCallback(() => {
    setShowSearchInput(true);
  }, []);

  const handleInputChange = useCallback(
    (value: string, { action }: { action?: string } = {}) => {
      if (action === "input-change") {
        setTooltip(false);
        setInput(value);
      }
    },
    []
  );

  const handleReset = useCallback(() => {
    setInput("");
    setShowSearchInput(false);
    setTooltip(false);
  }, []);

  const handleChange = useCallback(
    (value: any) => {
      onClick(null, value);
      handleReset();
    },
    [onClick, handleReset]
  );

  const handleKeyDown = useCallback(
    (e: any) => {
      switch (e.key) {
        case "Escape":
          e.preventDefault();
          handleReset();
          break;
        case "Home":
          e.preventDefault();
          if (e.shiftKey) e.target.selectionStart = 0;
          else e.target.setSelectionRange(0, 0);
          break;
        case "End":
          e.preventDefault();
          const len = e.target.value.length;
          if (e.shiftKey) e.target.selectionEnd = len;
          else e.target.setSelectionRange(len, len);
          break;
      }
    },
    [handleReset]
  );

  const handleFilterOption = useCallback(
    (option: { data: SearchItem }, searchText?: string) => {
      const { data: item } = option;
      return filterSearchItem(item, searchText);
    },
    []
  );

  const maxMenuHeight = useMemo(() => {
    if (showSearchInput) {
      const parent = containerRef.current?.parentElement?.parentElement;
      const searchInputHeight = 55;
      const parentHeight = parent?.clientHeight || 0;
      return parentHeight - searchInputHeight;
    }
    return;
  }, [showSearchInput]);

  useEffect(() => {
    if (tooltip) {
      let timer = setTimeout(() => {
        hideTooltip();
      }, 3000);

      function hideTooltip() {
        clearTimeout(timer);
        setTooltip(false);
      }

      return () => hideTooltip();
    }
  }, [tooltip]);

  useEffect(() => {
    timerRef.current && clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      setTooltip(true);
      setOptions(getSearchOptions(items, input));
    }, 400);
  }, [items, input]);

  useEffect(() => {
    return () => {
      timerRef.current && clearTimeout(timerRef.current);
    };
  }, []);

  const hasSearchResult = showSearchInput && input && options.length > 0;

  useEffect(() => {
    const navbarElement = containerRef.current?.nextSibling as HTMLElement;
    navbarElement &&
      navbarElement.classList?.[hasSearchResult ? "add" : "remove"]?.(
        styles.hide
      );
  }, [hasSearchResult]);

  return (
    <>
      <Box
        ref={containerRef}
        d="flex"
        alignItems="center"
        justifyContent="center"
        bg="body"
      >
        {showSearchInput ? (
          <ReactSelect
            ref={selectRef}
            autoFocus
            menuIsOpen={options.length > 0}
            placeholder={i18n.get("Search...")}
            className={styles.select}
            inputValue={input}
            options={options}
            styles={
              {
                ...customStyles,
                menu: (base: CSSProperties) => ({
                  ...customStyles.menu(base),
                  ...(maxMenuHeight && { height: maxMenuHeight }),
                }),
              } as any
            }
            onBlur={handleReset}
            onInputChange={handleInputChange}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            filterOption={handleFilterOption}
          />
        ) : (
          <MaterialIcon
            fontSize="1.25rem"
            icon="expand_more"
            className={styles.icon}
            onClick={handleExpandMoreClick}
          />
        )}
      </Box>
      {options.length === 0 && showSearchInput && input && tooltip && (
        <Popper
          open
          arrow
          target={selectRef.current?.controlRef}
          placement="bottom"
          offset={[0, 4]}
          bg="dark"
          color="light"
        >
          <Box p={2}>{i18n.get("No results")}</Box>
        </Popper>
      )}
    </>
  );
}
