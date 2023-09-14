import { useAtomValue } from "jotai";
import { ReactElement } from "react";

import { Box } from "@axelor/ui";

import { Schema, Selection as TSelection } from "@/services/client/meta.types";

import { FieldControl, FieldProps } from "../../builder";
import { Chip, Selection } from "../selection";

export function SingleSelectText({
  schema,
  value,
}: {
  schema: Schema;
  value?: string | number | null;
}) {
  const { colorField = "color" } = schema;
  const selectionList = (schema.selectionList ?? []) as TSelection[];
  const selected = selectionList.find((item) => String(item.value) === String(value));
  return (selected && (
    <Box d="flex">
      <Chip
        title={selected.title}
        color={(selected as any)[colorField] || "blue"}
      />
    </Box>
  )) as ReactElement;
}

export function SingleSelect(props: FieldProps<string | number | null>) {
  const { readonly, schema, valueAtom } = props;
  const value = useAtomValue(valueAtom);

  if (readonly) {
    return (
      <FieldControl {...props}>
        <SingleSelectText schema={schema} value={value} />
      </FieldControl>
    );
  }

  return <Selection {...props} selectComponents={["SingleValue"]} />;
}
