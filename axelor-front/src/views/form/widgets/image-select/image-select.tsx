import { useCallback, useMemo } from "react";
import { useAtom } from "jotai/index";

import { FieldProps } from "../../builder";
import { Selection } from "../selection";
import { ImageSelectIcon, ImageSelectValue } from "./image-select-value";

import { SelectOptionProps } from "@/components/select";
import { Selection as SelectionType } from "@/services/client/meta.types";
import { Icon } from "@/components/icon";

export function ImageSelect(props: FieldProps<string | number | null>) {
  const { schema, valueAtom } = props;
  const { labels } = schema;
  const selectionList = schema?.selectionList as SelectionType[];
  const [value] = useAtom(valueAtom);

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

  const adornment = useMemo(
    () =>
      selectedOption && (
        <ImageSelectIcon option={selectedOption} showLabel={labels} />
      ),
    [selectedOption, labels],
  );
  
  return (
    <Selection
      {...props}
      inputStartAdornment={adornment}
      renderValue={renderValue}
      renderOption={({ option }) => (
        <ImageSelectValue option={option} showLabel={labels} />
      )}
    />
  );
}
