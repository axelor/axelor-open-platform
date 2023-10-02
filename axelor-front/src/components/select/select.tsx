import clsx from "clsx";
import {
  ForwardedRef,
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Select as AxSelect,
  SelectProps as AxSelectProps,
  SelectCustomOption,
  SelectValue,
  useRefs,
} from "@axelor/ui";

import { i18n } from "@/services/client/i18n";
import { SanitizedContent } from "@/utils/sanitize";

import styles from "./select.module.scss";

export type {
  SelectCustomOption,
  SelectIcon,
  SelectOptionProps,
  SelectValue,
} from "@axelor/ui";

export interface SelectProps<Type, Multiple extends boolean>
  extends AxSelectProps<Type, Multiple> {
  fetchOptions?: (inputValue: string) => Promise<Type[]>;
  onShowCreate?: (inputValue: string) => void;
  onShowSelect?: (inputValue: string) => void;
}

export const Select = forwardRef(function Select<
  Type,
  Multiple extends boolean,
>(props: SelectProps<Type, Multiple>, ref: ForwardedRef<HTMLDivElement>) {
  const {
    autoFocus,
    readOnly,
    className,
    options,
    fetchOptions,
    onShowCreate,
    onShowSelect,
    onInputChange,
    onOpen,
    openOnFocus = true,
    value = null,
    onChange,
    ...selectProps
  } = props;

  const [items, setItems] = useState<Type[]>([]);
  const [inputValue, setInputValue] = useState("");

  const selectRef = useRefs(ref);
  const loadTimerRef = useRef<ReturnType<typeof setTimeout>>();

  const loadOptions = useCallback(
    (inputValue: string) => {
      if (loadTimerRef.current) {
        clearTimeout(loadTimerRef.current);
      }
      loadTimerRef.current = setTimeout(async () => {
        if (fetchOptions) {
          const items = await fetchOptions(inputValue);
          loadTimerRef.current = undefined;
          setItems(items);
        }
      }, 300);
    },
    [fetchOptions],
  );

  const handleOpen = useCallback(() => {
    if (onOpen) onOpen();
    if (
      fetchOptions &&
      items.length === 0 &&
      !inputValue &&
      !loadTimerRef.current
    ) {
      loadOptions("");
    }
  }, [fetchOptions, inputValue, items.length, loadOptions, onOpen]);

  const handleInputChange = useCallback(
    (text: string) => {
      setInputValue(text);
      if (onInputChange) onInputChange(text);
      if (fetchOptions && text) {
        loadOptions(text);
      }
    },
    [fetchOptions, loadOptions, onInputChange],
  );

  const handleChange = useCallback(
    (value: SelectValue<Type, Multiple>) => {
      onChange?.(value);
    },
    [onChange],
  );

  useEffect(() => {
    clearTimeout(loadTimerRef.current);
  }, []);

  useEffect(() => {
    if (autoFocus && selectRef.current) {
      selectRef.current.focus();
    }
  }, [autoFocus, selectRef]);

  const customOptions = useMemo(() => {
    const options: SelectCustomOption[] = [];
    if (onShowCreate) {
      options.push({
        key: "create",
        title: (
          <span>
            {inputValue ? (
              <SanitizedContent
                content={i18n.get("Create {0}...", `<em>${inputValue}</em>`)}
              />
            ) : (
              i18n.get("Create...")
            )}
          </span>
        ),
        onClick: () => onShowCreate(inputValue),
      });
    }
    if (onShowSelect) {
      options.push({
        key: "select",
        title: (
          <span>
            {inputValue ? (
              <SanitizedContent
                content={i18n.get("Select {0}...", `<em>${inputValue}</em>`)}
              />
            ) : (
              i18n.get("Select...")
            )}
          </span>
        ),
        onClick: () => onShowSelect(inputValue),
      });
    }
    return options;
  }, [inputValue, onShowCreate, onShowSelect]);

  const currOptions = fetchOptions ? items : options;
  const moreOptions = currOptions.length || inputValue ? customOptions : [];

  return (
    <AxSelect
      {...selectProps}
      ref={selectRef}
      value={value}
      autoFocus={autoFocus}
      readOnly={readOnly}
      openOnFocus={openOnFocus}
      options={currOptions}
      customOptions={moreOptions}
      onInputChange={handleInputChange}
      onOpen={handleOpen}
      onChange={handleChange}
      className={clsx(className, { [styles.readonly]: readOnly })}
    />
  );
}) as unknown as <Type, Multiple extends boolean>(
  props: SelectProps<Type, Multiple>,
  ref: ForwardedRef<HTMLDivElement>,
) => React.ReactNode;
