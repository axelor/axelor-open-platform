import { useMemo } from "react";

import { Schema, Selection } from "@/services/client/meta.types";
import { SelectionTag } from "@/views/form/widgets";

export type SingleSelectChipProps = {
  schema: Schema;
  value: string | number | null;
};

export function SingleSelectValue(props: SingleSelectChipProps) {
  const { schema, value } = props;
  const selectionList = schema.selectionList as Selection[];
  const colorField = useMemo(() => schema.colorField ?? "color", [schema]);
  const selection = useMemo(
    () => selectionList?.find((x) => String(x.value) === String(value)),
    [selectionList, value],
  );

  if (selection) {
    return (
      <SelectionTag
        title={selection.title}
        color={selection?.[colorField as keyof typeof value]}
      />
    );
  }

  return value;
}
