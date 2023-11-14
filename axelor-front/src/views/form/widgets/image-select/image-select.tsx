import { useCallback, useMemo } from "react";
import { useAtom } from "jotai/index";

import { FieldProps } from "../../builder";
import { Selection } from "../selection";
import { ImageSelectValue } from "./image-select-value";

import { SelectOptionProps } from "@/components/select";
import { Selection as SelectionType } from "@/services/client/meta.types";

export function ImageSelect(props: FieldProps<string | number | null>) {
  const { schema, valueAtom } = props;
  const { labels } = schema;
  const selectionList = schema?.selectionList as SelectionType[];
  const [value, setValue] = useAtom(valueAtom);

  const selectedOption = useMemo(
    () => selectionList?.find((x) => String(x.value) === String(value)),
    [selectionList, value],
  );

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<SelectionType>) => {
      if (selectedOption) {
        return <ImageSelectValue option={option} showLabel={labels} />;
      }
      return null;
    },
    [labels, selectedOption],
  );

  return (
    <Selection
      {...props}
      autoComplete={false}
      renderValue={renderValue}
      renderOption={({ option }) => (
        <ImageSelectValue option={option} showLabel={labels} />
      )}
    />
  );
}
