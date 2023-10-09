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

import styles from "./select.module.scss";

export type {
  SelectCustomOption,
  SelectIcon,
  SelectOptionProps,
  SelectValue,
} from "@axelor/ui";

export interface SelectProps<Type, Multiple extends boolean>
  extends AxSelectProps<Type, Multiple> {
  canSelect?: boolean;
  fetchOptions?: (inputValue: string) => Promise<Type[]>;
  onShowCreateAndSelect?: (inputValue: string) => void;
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
    onShowCreateAndSelect,
    onInputChange,
    onOpen,
    canSelect = true,
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

    if (onShowCreateAndSelect && inputValue) {
      options.push({
        key: "create-and-select",
        title: (
          <span>
            <em> {i18n.get(`Create "{0}" and select...`, inputValue)}</em>
          </span>
        ),
        onClick: () => onShowCreateAndSelect(inputValue),
      });
    }

    if (onShowCreate) {
      options.push({
        key: "create",
        title: (
          <span>
            <em>
              {inputValue && selectProps.multiple
                ? i18n.get(`Create "{0}"...`, inputValue)
                : i18n.get("Create...")}
            </em>
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
            <em>{i18n.get("Search more...")}</em>
          </span>
        ),
        onClick: () => onShowSelect(inputValue),
      });
    }
    return options;
  }, [
    inputValue,
    selectProps.multiple,
    onShowCreate,
    onShowCreateAndSelect,
    onShowSelect,
  ]);

  const currOptions = fetchOptions ? items : options;
  const moreOptions = currOptions.length || inputValue ? customOptions : [];

  return (
    <AxSelect
      clearOnBlur
      clearOnEscape
      {...selectProps}
      ref={selectRef}
      value={value}
      autoFocus={autoFocus}
      readOnly={readOnly || !canSelect}
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
