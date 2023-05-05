import {
  ChangeEvent,
  KeyboardEvent,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import { Box, Badge, TextField } from "@axelor/ui";
import { MaterialIconProps } from "@axelor/ui/icons/meterial-icon";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import clsx from "clsx";

import { legacyClassNames } from "@/styles/legacy";
import { i18n } from "@/services/client/i18n";
import { SavedFilter, SearchFilter } from "@/services/client/meta.types";

import styles from "./search-input.module.scss";

export interface SearchInputProps {
  text?: string;
  search?: boolean;
  count?: number;
  domains?: SearchFilter[];
  filters?: SavedFilter[];
  value?: any[];
  onOpen?: () => void;
  onClear?: () => void;
  onSearch?: (term?: string) => void;
}

function Chip({
  label,
  color,
  onDelete,
}: {
  label: string;
  color: string;
  onDelete: () => void;
}) {
  function renderLabel() {
    return (
      <>
        <span>{label}</span>
        {onDelete && (
          <Box
            d="flex"
            alignItems="center"
            justifyContent="center"
            className={styles.chipIcon}
            ms={1}
            onClick={onDelete}
          >
            <MaterialIcon icon="close" />
          </Box>
        )}
      </>
    );
  }

  const chipCss = legacyClassNames(`bg-${(color || "blue").trim()}`);
  if (!chipCss) return renderLabel();
  return (
    <Badge d="flex" className={clsx(styles.chip, chipCss)}>
      {renderLabel()}
    </Badge>
  );
}

function ChipList({
  value,
  icons = [],
  onClear,
}: Pick<SearchInputProps, "onClear"> & {
  value?: string;
  icons?: MaterialIconProps[];
}) {
  function handleClear() {
    onClear && onClear();
  }

  return (
    <Box border d="flex">
      <Box className={styles.chipList} d="flex" flex={1} p={1}>
        <Chip color={"indigo"} label={value!} onDelete={handleClear} />
      </Box>
      <Box
        d="flex"
        justifyContent="flex-end"
        alignItems="center"
        position="relative"
        className={styles.actions}
      >
        {icons.map((icon, ind) => (
          <MaterialIcon
            key={`icon_${ind}`}
            icon={icon.icon}
            onClick={icon.onClick}
            className={styles.icon}
          />
        ))}
      </Box>
    </Box>
  );
}

export function SearchInput({
  text,
  search = true,
  count,
  filters,
  domains,
  value,
  onOpen,
  onClear,
  onSearch,
}: SearchInputProps) {
  const [inputText, setInputText] = useState("");
  const hasValue = Boolean(value?.length);

  const handleSearch = useCallback(() => {
    onSearch?.(inputText);
  }, [onSearch, inputText]);

  function handleClear() {
    setInputText("");
  }

  function handleChange(e: ChangeEvent<HTMLInputElement>) {
    setInputText(e.target.value);
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      handleSearch();
    }
  }

  const $value = useMemo(() => {
    if (value?.length === 0) return "";

    if ((value?.length ?? 0) > 1) {
      return i18n.get("Filters ({0})", value?.length);
    }

    if (value?.length === 1) {
      const filter = filters?.find((f) => f.id === value[0]);

      if (filter) {
        return filter.title;
      } else {
        const $domain = domains?.find((d) => d.id === value[0]);
        return $domain
          ? $domain.title
          : (count ?? 0) > 1
          ? i18n.get("Custom ({0})", count)
          : i18n.get("Custom");
      }
    }
    return "";
  }, [count, value, domains, filters]);

  const icons = useMemo(
    () =>
      [
        { icon: "arrow_drop_down", onClick: onOpen },
        { icon: "clear", onClick: onClear },
        { icon: "search", onClick: handleSearch },
      ] as MaterialIconProps[],
    [onOpen, onClear, handleSearch]
  );

  useEffect(() => {
    hasValue && setInputText("");
  }, [hasValue]);

  useEffect(() => {
    text && setInputText(text);
  }, [text]);

  return hasValue ? (
    <ChipList value={$value} icons={icons} onClear={onClear} />
  ) : (
    <TextField
      className={styles.input}
      name="search"
      {...(search
        ? {
            value: inputText,
            placeholder: i18n.get("Search"),
            onChange: handleChange,
            onKeyDown: handleKeyDown,
          }
        : {
            readOnly: true,
            onClick: onOpen,
          })}
      icons={
        [
          {
            icon: "arrow_drop_down",
            className: styles.icon,
            onClick: onOpen,
          },
          ...(hasValue
            ? [
                {
                  icon: "clear",
                  className: styles.icon,
                  onClick: handleClear,
                },
              ]
            : []),
          {
            icon: "search",
            className: styles.icon,
            onClick: handleSearch,
          },
        ] as unknown as MaterialIconProps[]
      }
    />
  );
}
