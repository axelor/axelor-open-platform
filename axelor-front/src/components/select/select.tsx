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

import { Select as AxSelect, SelectProps } from "@axelor/ui/core/select2";

import { i18n } from "@/services/client/i18n";

import styles from "./select.module.scss";

export type {
  SelectIcon,
  SelectOptionProps,
  SelectOptionType,
  SelectProps,
} from "@axelor/ui/core/select2";

export const Select = forwardRef(function Select<
  Type,
  Multiple extends boolean,
>(
  props: SelectProps<Type, Multiple> & {
    fetchOptions?: (inputValue: string) => Promise<Type[]>;
  },
  ref: ForwardedRef<HTMLDivElement>,
) {
  const {
    autoFocus,
    readOnly,
    className,
    options,
    fetchOptions,
    onInputChange,
    onOpen,
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

  return (
    <AxSelect
      {...props}
      {...focusProps}
      ref={ref}
      options={fetchOptions ? items : options}
      onInputChange={handleInputChange}
      onOpen={handleOpen}
      className={clsx(className, { [styles.readonly]: readOnly })}
      translations={{
        create: i18n.get("Create"),
        select: i18n.get("Select"),
      }}
    />
  );
}) as unknown as <Type, Multiple extends boolean>(
  props: SelectProps<Type, Multiple> & {
    fetchOptions?: (inputValue: string) => Promise<Type[]>;
  },
  ref: ForwardedRef<HTMLDivElement>,
) => React.ReactNode;
