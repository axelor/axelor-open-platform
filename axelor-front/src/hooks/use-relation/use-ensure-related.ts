import { useAtomCallback } from "jotai/utils";
import deepGet from "lodash/get";
import deepEqual from "lodash/isEqual";
import deepSet from "lodash/set";
import { useCallback, useEffect, useMemo, useRef } from "react";

import { DataSource } from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";

import { FormAtom, ValueAtom } from "../../views/form/builder/types";

async function fetchRelated({
  value,
  field,
  related,
  refetch,
}: {
  value: DataRecord;
  field: Schema;
  related: string[];
  refetch?: boolean;
}) {
  if (value && value.id && value.id > 0 && field.perms?.read !== false) {
    const { targetName, target } = field;
    const dataSource = new DataSource(target);
    const missing = refetch
      ? related
      : related.filter((x) => {
          let current = value;
          for (const key of x.split(".")) {
            current = current[key];
            if (current === undefined) return true;
            if (current === null) return false;
          }
          return false;
        });
    if (missing.length > 0) {
      try {
        const rec = await dataSource.read(value.id, { fields: missing }, true);
        return { ...value, ...rec, version: undefined };
      } catch {
        return { ...value, [targetName]: value[targetName] ?? value.id };
      }
    }
  }
  return value;
}

export function useEnsureRelated({
  field,
  formAtom,
  valueAtom,
  dottedOnly,
}: {
  field: Schema;
  formAtom: FormAtom;
  valueAtom: ValueAtom<DataRecord>;
  dottedOnly?: boolean;
}) {
  const { name = "", dotted, targetName, depends } = field;

  const related = useMemo(() => {
    const names = dottedOnly
      ? [...(dotted ?? [])].filter(Boolean)
      : [...(dotted ?? []), targetName, depends?.split(",")].flat().filter(Boolean);

    return [...new Set(names)] as string[];
  }, [depends, dotted, dottedOnly, targetName]);


  const valueRef = useRef<DataRecord>();
  const mountRef = useRef(false);

  useEffect(() => {
    mountRef.current = true;
    return () => {
      mountRef.current = false;
    };
  }, []);

  const ensureRelated = useCallback(
    async (value: DataRecord, refetch?: boolean) => {
      return await fetchRelated({ value, field, related, refetch });
    },
    [field, related],
  );

  const updateRelated = useAtomCallback(
    useCallback(
      async (get, set, value: DataRecord, refetch?: boolean) => {
        if (valueRef.current === value && !refetch) return;
        if (value) {
          const newValue = await ensureRelated(value, refetch);
          if (!deepEqual(newValue, value)) {
            valueRef.current = newValue;
            if (!mountRef.current) return;
            // related field of ref-select?
            if (field.related) {
              set(valueAtom, newValue, false, false);
              return;
            }
            const state = get(formAtom);
            const record = { ...state.record };
            deepSet(record, name, newValue);
            // updated reference dotted fields in record
            Object.keys(record).forEach((fieldName) => {
              if (fieldName.includes(".") && fieldName.startsWith(name)) {
                record[fieldName] = deepGet(record, fieldName.split("."));
              }
            });
            set(formAtom, {
              ...state,
              record,
            });
          } else {
            valueRef.current = value;
          }
        }
      },
      [ensureRelated, field.related, formAtom, name, valueAtom],
    ),
  );

  return {
    valueRef,
    ensureRelated,
    updateRelated,
  } as const;
}
