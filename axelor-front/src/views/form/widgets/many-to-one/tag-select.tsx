import { useCallback, useRef } from "react";
import { useAtom } from "jotai";

import { SelectProps } from "@axelor/ui";
import { SelectOptionProps } from "@/components/select";
import { RelationalTag } from "@/components/tag";
import { DataRecord } from "@/services/client/data.types";

import { ManyToOne } from "./many-to-one";
import { FieldProps } from "../../builder";

export function TagSelect(props: FieldProps<DataRecord>) {
  const { schema, valueAtom } = props;
  const [value, setValue] = useAtom(valueAtom);
  const selectRef = useRef<HTMLDivElement>();

  const optionMatch = useCallback(() => true, []);

  const handleRemove = useCallback(() => {
    setValue(null, true, true);
    requestAnimationFrame(() => {
      const input = selectRef.current?.querySelector("input");
      input?.focus();
    });
  }, [setValue]);

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<DataRecord>) => (
      <RelationalTag value={option} schema={schema} onRemove={handleRemove} />
    ),
    [schema, handleRemove],
  );

  const renderOption = useCallback(
    ({ option }: SelectOptionProps<DataRecord>) => (
      <RelationalTag value={option} schema={schema} />
    ),
    [schema],
  );

  return (
    <ManyToOne
      {...props}
      selectProps={
        {
          autoComplete: !value,
          ref: selectRef,
          optionMatch,
          renderValue,
          renderOption,
          inputStartAdornment: undefined,
        } as unknown as SelectProps<DataRecord, false>
      }
    />
  );
}
