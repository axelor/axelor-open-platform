import { Schema, Selection as TSelection } from "@/services/client/meta.types";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtomValue } from "jotai";
import { Chip, Selection } from "../selection";
import { Box } from "@axelor/ui";
import { ReactElement } from "react";

export function SingleSelectText({
  schema,
  value,
}: {
  schema: Schema;
  value?: string | number | null;
}) {
  const { colorField = "color" } = schema;
  const selectionList = (schema.selectionList ?? []) as TSelection[];
  const selected = selectionList.find((item) => String(item.value) === value);
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
  const { uid, title } = schema;
  const value = useAtomValue(valueAtom);

  if (readonly) {
    return (
      <FieldContainer readonly={readonly}>
        <label htmlFor={uid}>{title}</label>
        <SingleSelectText schema={schema} value={value} />
      </FieldContainer>
    );
  }

  return <Selection {...props} selectComponents={["SingleValue"]} />;
}
