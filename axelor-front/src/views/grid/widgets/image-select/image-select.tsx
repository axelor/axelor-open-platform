import { useMemo } from "react";

import { GridColumnProps } from "@axelor/ui/grid/grid-column";

import { Schema, Selection } from "@/services/client/meta.types";
import { ImageSelectValue } from "@/views/form/widgets";

export function ImageSelect(props: GridColumnProps) {
  const { data, record } = props;
  const schema = data as Schema;
  const selectionList = schema?.selectionList as Selection[];
  const value = record?.[data?.name];
  const option = useMemo(
    () => selectionList?.find((x) => String(x.value) === String(value)),
    [selectionList, value],
  );
  if (option) {
    return <ImageSelectValue option={option} showLabel={schema.labels} />;
  }
  return null;
}
