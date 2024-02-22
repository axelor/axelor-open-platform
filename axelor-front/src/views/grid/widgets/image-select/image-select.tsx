import { useMemo } from "react";

import { GridColumnProps } from "@axelor/ui/grid";

import { Schema, Selection } from "@/services/client/meta.types";
import { ImageSelectValue } from "@/views/form/widgets";

export function ImageSelect(props: GridColumnProps) {
  const { data, rawValue } = props;
  const schema = data as Schema;
  const selectionList = schema?.selectionList as Selection[];
  const option = useMemo(
    () => selectionList?.find((x) => String(x.value) === String(rawValue)),
    [selectionList, rawValue],
  );
  if (option) {
    return <ImageSelectValue option={option} showLabel={schema.labels} />;
  }
  return null;
}
