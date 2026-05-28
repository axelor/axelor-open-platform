import { useMemo } from "react";
import setImmutableValue from "lodash/fp/set";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";
import { RelationalTag } from "@/components/tag";
import { RelationalValue } from "@/views/form/widgets";
import { Field, Schema } from "@/services/client/meta.types";

export function ManyToOne(props: GridColumnProps) {
  const { rawValue, record, data: schema } = props;
  const isTag = (schema as Field).widget === "tag";

  const value = useMemo(() => {
    if (!rawValue) {
      return rawValue;
    }
    return Object.keys(record).reduce(
      (_value, key) => {
        const prefix = `${schema.name}.`;
        // To merge dotted field value from record
        if (key.startsWith(prefix)) {
          const fieldName = key.slice(prefix.length);
          const fieldValue = record[key];
          return setImmutableValue(fieldName, fieldValue, _value);
        }
        return _value;
      },
      { ...rawValue },
    );
  }, [schema.name, rawValue, record]);

  return (
    <Box d="inline-flex">
      {!isTag && <RelationalValue schema={schema as Schema} value={value} />}
      {isTag && value && (
        <RelationalTag schema={schema as Schema} value={value} />
      )}
    </Box>
  );
}
