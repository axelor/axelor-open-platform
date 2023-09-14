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
} from "@axelor/ui/core/select2";

import { i18n } from "@/services/client/i18n";
import styles from "./select.module.scss";

export type {
  SelectIcon,
  SelectOptionProps,
  SelectValue,
} from "@axelor/ui/core/select2";

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
    ...selectProps
  } = props;

  const [items, setItems] = useState<Type[]>([]);
  const [inputValue, setInputValue] = useState("");

  const loadTimerRef = useRef<ReturnType<typeof setTimeout>>();
  const loadOptions = useCallback(
    (inputValue: string) => {
      if (loadTimerRef.current) {
        clearTimeout(loadTimerRef.current);
      }
      loadTimerRef.current = setTimeout(async () => {
        if (fetchOptions) {
          const items = await fetchOptions(inputValue);
          setItems(items);
        }
      }, 300);
    },
    [fetchOptions],
  );

  const handleOpen = useCallback(() => {
    if (onOpen) onOpen();
    if (fetchOptions && items.length === 0 && !inputValue) {
      loadOptions("");
    }
  }, [fetchOptions, inputValue, items.length, loadOptions, onOpen]);

  const handleInputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const text = event.target.value.trim();
      setInputValue(text);
      if (onInputChange) onInputChange(event);
      if (event.isDefaultPrevented()) return;
      if (fetchOptions) {
        loadOptions(text);
      }
    },
    [fetchOptions, loadOptions, onInputChange],
  );

  useEffect(() => {
    clearTimeout(loadTimerRef.current);
  }, []);

  const focusProps = useMemo(() => {
    if (autoFocus) {
      return {
        key: "focused",
        autoFocus,
      };
    }
  }, [autoFocus]);

  const customOptions = useMemo(() => {
    const options: SelectCustomOption[] = [];
    if (onShowCreate) {
      options.push({
        key: "create",
        title: (
          <span>
            {i18n.get("Create")}
            {inputValue && <em> {inputValue}</em>}...
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
            {i18n.get("Select")}
            {inputValue && <em> {inputValue}</em>}...
          </span>
        ),
        onClick: () => onShowSelect(inputValue),
      });
    }
    return options;
  }, [inputValue, onShowCreate, onShowSelect]);

  return (
    <AxSelect
      {...selectProps}
      {...focusProps}
      ref={ref}
      readOnly={readOnly}
      options={fetchOptions ? items : options}
      customOptions={customOptions}
      onInputChange={handleInputChange}
      onOpen={handleOpen}
      className={clsx(className, { [styles.readonly]: readOnly })}
    />
  );
}) as unknown as <Type, Multiple extends boolean>(
  props: SelectProps<Type, Multiple>,
  ref: ForwardedRef<HTMLDivElement>,
) => React.ReactNode;
