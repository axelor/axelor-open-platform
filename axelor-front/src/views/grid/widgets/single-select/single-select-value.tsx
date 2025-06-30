import { useMemo } from "react";

import { Tag } from "@/components/tag";
import { Schema, Selection } from "@/services/client/meta.types";

export type SingleSelectChipProps = {
  schema: Schema;
  value: string | number | null;
};

export function SingleSelectValue(props: SingleSelectChipProps) {
  const { schema, value } = props;
  const selectionList = schema.selectionList as Selection[];
  const selection = useMemo(
    () => selectionList?.find((x) => String(x.value) === String(value)),
    [selectionList, value],
  );

  if (selection) {
    return <Tag title={selection.title} color={selection.color} />;
  }

  return value;
}
