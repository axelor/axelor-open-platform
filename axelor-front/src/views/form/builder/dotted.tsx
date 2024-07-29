import { atom, useAtomValue } from "jotai";
import { useCallback, useMemo } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useEnsureRelated } from "@/hooks/use-relation";
import { Schema } from "@/services/client/meta.types";

import { useViewMeta } from "@/view-containers/views/scope";
import { FormAtom } from "./types";
import { isReferenceField } from "./utils";

export function DottedValues({ formAtom }: { formAtom: FormAtom }) {
  const { meta, findItems } = useViewMeta();

  const relations = useMemo(() => {
    const items = findItems();
    const mapping: Record<string, { schema: Schema; related: string[] }> = {};
    for (const item of items) {
      const { name, targetName } = item;

      if (name?.includes(".")) continue;
      if (name && isReferenceField(item)) {
        const field = meta.fields?.[name];
        const prefix = `${name}.`;

        // TODO: `useFieldRelated` code is quite related. can be merged.
        const dotted = items
          .filter((x) => x.name?.startsWith(prefix))
          .map((x) => x.name as string)
          .map((x) => x.substring(prefix.length));

        const relatedFields = items
          .map((x) =>
            [x.depends?.split(","), x.viewer?.depends?.split(",")].flat(),
          )
          .flat()
          .filter(Boolean)
          .filter((x) => x.startsWith(prefix))
          .map((x) => x.substring(prefix.length));

        const names = [...dotted, ...relatedFields].flat().filter(Boolean);

        const shouldFetchTargetName =
          item.targetName &&
          (field?.jsonField || field?.targetName !== item.targetName);

        if (names.length || shouldFetchTargetName) {
          mapping[name] ??= {
            schema: item,
            related: [...new Set([...names, targetName])] as string[],
          };
        }
      }
    }
    return Object.entries(mapping);
  }, [meta, findItems]);

  return relations.map(([name, { schema, related }]) => (
    <EnsureRelated
      key={name}
      schema={schema}
      formAtom={formAtom}
      related={related}
    />
  ));
}

function EnsureRelated({
  schema,
  related,
  formAtom,
}: {
  schema: Schema;
  related: string[];
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
    related,
  });

  const value = useAtomValue(valueAtom);

  const ensureRelatedValues = useCallback(async () => {
    if (value) updateRelated(value);
  }, [updateRelated, value]);

  useAsyncEffect(ensureRelatedValues, [ensureRelatedValues]);

  return null;
}
