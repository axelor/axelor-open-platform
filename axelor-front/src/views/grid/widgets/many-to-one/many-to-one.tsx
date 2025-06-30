import { useMemo } from "react";
import set from "lodash/set";

import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/grid";
import { RelationalValue, Tag } from "@/views/form/widgets";
import { Field, Schema } from "@/services/client/meta.types";

export function ManyToOne(props: GridColumnProps) {
  const { rawValue, record, data: schema } = props;
  const isTagSelect = (schema as Field).widget === "tag-select";

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
          return set(_value, fieldName, fieldValue);
        }
        return _value;
      },
      { ...rawValue },
    );
  }, [schema.name, rawValue, record]);

  return (
    <Box d="inline-flex">
      {!isTagSelect && (
        <RelationalValue schema={schema as Schema} value={value} />
      )}
      {isTagSelect && value && <Tag schema={schema as Schema} record={value} />}
    </Box>
  );
}
