import { useMemo } from "react";
import { GridColumnProps } from "@axelor/ui/grid";

import { TagList } from "@/components/tag";
import { DataRecord } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";

export function Tags(props: GridColumnProps) {
  const { rawValue, data: schema } = props;

  const items = useMemo(
    () =>
      (rawValue && !Array.isArray(rawValue) ? [rawValue] : rawValue || []) as DataRecord[],
    [rawValue],
  );

  return <TagList items={items} schema={schema as Schema} />;
}
