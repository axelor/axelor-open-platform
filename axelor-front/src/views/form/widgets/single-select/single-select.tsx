import { FieldProps } from "../../builder";
import { useCallback } from "react";

import { Tag } from "@/components/tag";
import { SelectOptionProps } from "@/components/select";
import { Selection as SelectionType } from "@/services/client/meta.types";

import { Selection } from "../selection";

export function SingleSelect(props: FieldProps<string | number | null>) {

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<SelectionType>) => {
      return <Tag title={option.title} color={option.color} />;
    },
    [],
  );

  return (
    <Selection
      {...props}
      autoComplete={false}
      multiple={false}
      renderValue={renderValue}
      renderOption={renderValue}
    />
  );
}
