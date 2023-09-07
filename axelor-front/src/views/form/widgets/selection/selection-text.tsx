import { Box } from "@axelor/ui";

import {
  Schema,
  Selection as SelectionType,
} from "@/services/client/meta.types";

import { ViewerInput } from "../string/viewer";

export function SelectionText({
  schema,
  value,
}: {
  schema: Schema;
  value?: string | number | null;
}) {
  const selectionList = (schema.selectionList ?? []) as SelectionType[];
  const selected = selectionList.find(
    (item) => String(item.value) === String(value),
  );
  return (
    <Box d="flex">
      <ViewerInput value={selected?.title ?? ""} />
    </Box>
  );
}
