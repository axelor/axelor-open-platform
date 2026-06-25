import { atom, useAtomValue } from "jotai";
import { useCallback, useMemo } from "react";
import deepGet from "lodash/get";
import deepSet from "lodash/set";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useEnsureRelated } from "@/hooks/use-relation";
import { Schema } from "@/services/client/meta.types";

import { getExtraRelated } from "@/hooks/use-relation/use-editor";
import { useViewAction, useViewMeta } from "@/view-containers/views/scope";
import { FormAtom } from "./types";
import { isReferenceField } from "./utils";

export function DottedValues({ formAtom }: { formAtom: FormAtom }) {
  const { meta, findItems } = useViewMeta();
  const { context } = useViewAction();
  const extraRelated = getExtraRelated(context);

  const relations = useMemo(() => {
    const items = findItems();

    const mapping: Record<string, { schema: Schema; related: string[] }> = {};

    function getRelatedFieldList(prefix: string) {
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

      return [...dotted, ...relatedFields].flat();
    }

    // pre-process editor items
    // in order to reduce overload of finding editor item repeatedly
    const editorItems = items.reduce(
      (itemSet, item) =>
        item.name && item.editor ? { ...itemSet, [item.name]: item } : itemSet,
      {},
    );

    for (const item of items) {
      const { name, targetName } = item;

      if (name?.includes(".")) continue;
      if (name && isReferenceField(item)) {
        const field = meta.fields?.[name];
        const prefix = `${name}.`;

        const editorItem = editorItems[name];
        const relatedList = getRelatedFieldList(prefix);
        const relatedFieldsInEditor = editorItem
          ? relatedList.filter((fieldName) =>
              Object.keys(editorItem?.editor?.fields ?? {}).some(
                (editorFieldName) => fieldName.startsWith(editorFieldName),
              ),
            )
          : [];

        if (relatedFieldsInEditor.length) {
          relatedFieldsInEditor.forEach((editFieldName) => {
            const subFieldName = editFieldName.split(".")[0];
            const fieldName = `${name}.${subFieldName}`;
            const subFieldPrefix = `${fieldName}.`;
            const schema = item?.editor?.fields?.[subFieldName];
            if (schema) {
              mapping[fieldName] ??= {
                schema: {
                  ...schema,
                  name: fieldName,
                },
                related: getRelatedFieldList(subFieldPrefix),
              };
            }
          });
        }

        const names = relatedList.filter(
          (x) => x && !relatedFieldsInEditor.includes(x),
        );

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
    // Merge extra related fields injected by the editor popup (e.g. O2M dotted grid columns)
    if (extraRelated) {
      for (const [fieldName, subFields] of Object.entries(extraRelated)) {
        if (!subFields.length) continue;
        const fieldSchema = meta.fields?.[fieldName];
        if (!fieldSchema) continue;
        if (mapping[fieldName]) {
          mapping[fieldName] = {
            ...mapping[fieldName],
            related: [
              ...new Set([...mapping[fieldName].related, ...subFields]),
            ],
          };
        } else {
          mapping[fieldName] = {
            schema: { ...fieldSchema, name: fieldName } as Schema,
            related: subFields,
          };
        }
      }
    }

    return Object.entries(mapping);
  }, [meta, findItems, extraRelated]);

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
        (get) => deepGet(get(formAtom).record, name),
        (get, set, value: unknown) => {
          const { record, ...rest } = get(formAtom);
          set(formAtom, {
            ...rest,
            record: deepSet({ ...record }, name, value),
          });
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
    if (value) return updateRelated(value);
  }, [updateRelated, value]);

  useAsyncEffect(ensureRelatedValues, [ensureRelatedValues]);

  return null;
}
