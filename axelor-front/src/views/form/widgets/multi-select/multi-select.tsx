import { useAtomValue } from "jotai";
import { ReactElement } from "react";

import { Box } from "@axelor/ui";

import { Schema, Selection as TSelection } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { Chip, Selection } from "../selection";

export function MultiSelectText({
  schema,
  value,
}: {
  schema: Schema;
  value?: string | number | null;
}) {
  const { colorField = "color" } = schema;
  const selectionList = (schema.selectionList ?? []) as TSelection[];
  const values: string[] = `${value || ""}`.split(/\s*,\s*/);
  const selected = selectionList.filter((item) =>
    values.includes(String(item.value))
  );
  return (selected.length > 0 && (
    <Box d="flex">
      {selected.map((item, ind) => (
        <Box me={1} key={ind}>
          <Chip title={item.title} color={(item as any)[colorField]} />
        </Box>
      ))}
    </Box>
  )) as ReactElement;
}

export function MultiSelect(props: FieldProps<string | number | null>) {
  const { readonly, schema, valueAtom } = props;
  const value = useAtomValue(valueAtom);

  if (readonly) {
    return (
      <FieldControl {...props}>
        <MultiSelectText schema={schema} value={value} />
      </FieldControl>
    );
  }

  return (
    <Selection
      {...props}
      selectComponents={["SingleValue", "MultiValue"]}
      selectProps={{ isMulti: true }}
    />
  );
}
