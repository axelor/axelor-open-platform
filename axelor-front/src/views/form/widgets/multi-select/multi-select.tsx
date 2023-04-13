import { Schema, Selection as TSelection } from "@/services/client/meta.types";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtomValue } from "jotai";
import { Chip, Selection } from "../selection";
import { Box } from "@axelor/ui";
import { ReactElement } from "react";

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
  const { readonly, schema, widgetAtom, valueAtom } = props;
  const { uid, showTitle = true } = schema;
  const value = useAtomValue(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);

  if (readonly) {
    return (
      <FieldContainer readonly={readonly}>
        {showTitle && <label htmlFor={uid}>{title}</label>}
        <MultiSelectText schema={schema} value={value} />
      </FieldContainer>
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
