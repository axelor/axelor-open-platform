import clsx from "clsx";
import { useAtom, useAtomValue } from "jotai";
import { useCallback, useMemo } from "react";

import { Select, SelectProps, SelectValue } from "@/components/select";
import { Selection as SelectionType } from "@/services/client/meta.types";
import convert from "@/utils/convert";

import { FieldControl, FieldProps } from "../../builder";
import { useSelectionDefault, useSelectionList } from "./hooks";

import styles from "./selection.module.scss";

const optionKey = (item: SelectionType) => item.value!;
const optionLabel = (item: SelectionType) => item.title!;
const optionEqual = (a: SelectionType, b: SelectionType) => a.value === b.value;
const optionMatch = (option: SelectionType, text: string) =>
  optionLabel(option).toLowerCase().includes(text.toLowerCase());

export type SelectionProps<Multiple extends boolean> = FieldProps<
  string | number | null
> &
  Pick<
    SelectProps<SelectionType, Multiple>,
    | "multiple"
    | "autoComplete"
    | "renderOption"
    | "renderValue"
    | "closeOnSelect"
  >;

export function Selection<Multiple extends boolean>(
  props: SelectionProps<Multiple>,
) {
  const {
    schema,
    readonly,
    invalid,
    multiple,
    autoComplete = true,
    closeOnSelect,
    renderOption,
    renderValue,
    widgetAtom,
    valueAtom,
  } = props;

  const { placeholder } = schema;
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { required, focus },
  } = useAtomValue(widgetAtom);

  const selectionList = useSelectionList({ schema, widgetAtom });
  const selectionDefault = useSelectionDefault({ schema, value });

  const selectionValue = useMemo(() => {
    const selectionAll: SelectionType[] = schema.selectionList ?? [];
    if (multiple) {
      const values = value == null ? [] : String(value ?? "").split(",");
      return values.map(
        (x) =>
          selectionAll.find((item) => String(item.value) === String(x)) ??
          selectionDefault,
      );
    }
    return (
      selectionAll.find((item) => String(item.value) === String(value)) ??
      selectionDefault
    );
  }, [multiple, schema.selectionList, selectionDefault, value]) as SelectValue<
    SelectionType,
    Multiple
  >;

  const handleChange = useCallback(
    (value: SelectValue<SelectionType, Multiple>) => {
      let next: string | null = null;
      if (value) {
        next = Array.isArray(value)
          ? value.map((x) => String(x.value)).join(",")
          : convert(value.value, { props: schema }) ?? null;
      }
      setValue(next, true);
    },
    [schema, setValue],
  );

  return (
    <FieldControl {...props}>
      <Select
        className={clsx({
          [styles.readonly]: readonly,
          [styles.editable]: schema.editable,
        })}
        autoFocus={focus}
        autoComplete={autoComplete}
        multiple={multiple}
        readOnly={readonly}
        required={required}
        invalid={invalid}
        options={selectionList}
        optionKey={optionKey}
        optionLabel={optionLabel}
        optionEqual={optionEqual}
        optionMatch={optionMatch}
        value={selectionValue}
        onChange={handleChange}
        renderOption={renderOption}
        renderValue={renderValue}
        placeholder={placeholder}
        closeOnSelect={closeOnSelect}
      />
    </FieldControl>
  );
}
