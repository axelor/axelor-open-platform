import { atom, useAtomValue } from "jotai";
import { useCallback, useMemo } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useEnsureRelated } from "@/hooks/use-relation";
import { Schema } from "@/services/client/meta.types";

import { useViewMeta } from "@/view-containers/views/scope";
import { FormAtom } from "./types";
import { isReferenceField } from "./utils";

export function DottedValues({ formAtom }: { formAtom: FormAtom }) {
  const { findItems } = useViewMeta();

  const relations = useMemo(() => {
    const items = findItems();
    const mapping: Record<string, Schema> = {};
    for (const item of items) {
      const { name } = item;
      if (name?.includes(".")) continue;
      if (name && isReferenceField(item)) {
        const prefix = `${name}.`;
        const related = items.find((x) => x.name?.startsWith(prefix));
        if (related) {
          mapping[name] ??= item;
        }
      }
    }
    return Object.entries(mapping);
  }, [findItems]);

  return relations.map(([name, schema]) => (
    <EnsureRelated key={name} schema={schema} formAtom={formAtom} />
  ));
}

function EnsureRelated({
  schema,
  formAtom,
}: {
  schema: Schema;
  formAtom: FormAtom;
}) {
  const { name = "" } = schema;
  const valueAtom = useMemo(
    () =>
      atom(
        (get) => get(formAtom).record[name],
        (get, set, value: unknown) => {
          const { record, ...rest } = get(formAtom);
          set(formAtom, { ...rest, record: { ...record, [name]: value } });
        },
      ),
    [formAtom, name],
  );

  const { updateRelated } = useEnsureRelated({
    field: schema,
    formAtom,
    valueAtom,
  });

  const value = useAtomValue(valueAtom);

  const ensureRelatedValues = useCallback(async () => {
    if (value) updateRelated(value);
  }, [updateRelated, value]);

  useAsyncEffect(ensureRelatedValues, [ensureRelatedValues]);

  return null;
}
