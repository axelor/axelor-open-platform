import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { findView } from "@/services/client/meta-cache";
import { Schema } from "@/services/client/meta.types";
import { useCallback, useMemo } from "react";

export function useCreateOnTheFly(schema: Schema) {
  const { target: model, formView } = schema;

  const createNames = useMemo<string[]>(
    () => schema.create?.split(/\s*,\s*/) || [],
    [schema.create],
  );

  return useCallback(
    async ({
      input,
      popup,
      onEdit,
      onSelect,
    }: {
      input: string;
      popup?: boolean;
      onEdit?: (record: DataRecord) => void;
      onSelect?: (record: DataRecord) => void;
    }) => {
      if (!input) return onEdit?.({});
      const meta = await findView({
        type: "form",
        model,
        name: formView,
      });

      const record: DataRecord = {};
      let missing = false;

      if (meta?.fields) {
        Object.keys(meta.fields).forEach((k) => {
          const field = meta.fields![k];
          if (createNames.includes(field.name)) {
            record[field.name] = input;
          } else if (field.required) {
            missing = true;
          }
        });
      }

      if (missing || popup || Object.keys(record).length === 0) {
        return onEdit?.(record);
      }

      const ds = new DataStore(model);
      const saved = await ds.save(record);
      saved && onSelect?.(saved);
    },
    [createNames, model, formView],
  );
}
