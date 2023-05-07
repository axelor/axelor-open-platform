import { atom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { useMemo, useRef } from "react";

import { Box } from "@axelor/ui";

import { useAsync } from "@/hooks/use-async";
import { DataRecord } from "@/services/client/data.types";
import { findFields } from "@/services/client/meta-cache";
import { Selection as SelectionType } from "@/services/client/meta.types";

import { FieldControl, FieldProps, ValueAtom } from "../../builder";
import { ManyToOne } from "../many-to-one";
import { Selection } from "../selection";

export function RefSelect(props: FieldProps<any>) {
  const { schema, valueAtom } = props;

  const selection = useMemo(() => {
    const items: SelectionType[] = schema.selectionList || [];
    return items.reduce((acc, item) => {
      acc[item.value!] = item;
      return acc;
    }, {} as Record<string, SelectionType>);
  }, [schema.selectionList]);

  const selectSchema = useMemo(
    () => ({ ...schema, showTitle: false }),
    [schema]
  );

  const target = useAtomValue(valueAtom);
  const options = selection[target] ?? {};

  const domain = options?.data?.domain;
  const gridView = options?.data?.grid;
  const formView = options?.data?.form;

  const refSchema = useMemo(
    () => ({
      ...schema,
      target,
      domain,
      formView,
      gridView,
      showTitle: false,
    }),
    [domain, formView, gridView, schema, target]
  );

  const selectAtom: ValueAtom<string | number> = useMemo(() => {
    return atom(
      (get) => get(valueAtom),
      (get, set, value, fireOnChange) => {
        set(valueAtom, value, fireOnChange);
      }
    );
  }, [valueAtom]);

  return (
    <FieldControl {...props}>
      <Box d="flex" g={3}>
        <Box style={{ width: 200 }}>
          <Selection {...props} schema={selectSchema} valueAtom={selectAtom} />
        </Box>
        <Box flex="1">
          {target && <RefItem {...props} schema={refSchema} />}
        </Box>
      </Box>
    </FieldControl>
  );
}

function RefItem(props: FieldProps<any>) {
  const { schema } = props;
  const { target } = schema;

  const { data: targetName } = useAsync(async () => {
    const { fields } = await findFields(target);
    const nameField =
      Object.values(fields).find((f) => f.nameColumn) ??
      fields.name ??
      fields.code ??
      fields.id;
    return nameField.name;
  }, [target]);

  if (targetName) {
    return <RefItemInner {...props} targetName={targetName} />;
  }

  return null;
}

function RefItemInner(props: FieldProps<any> & { targetName: string }) {
  const { formAtom, valueAtom, targetName } = props;
  const schema = useMemo(
    () => ({ ...props.schema, targetName, related: props.schema.related }),
    [props.schema, targetName]
  );

  const related = schema.related ?? schema.name + "Id";
  const relatedAtom = useMemo(
    () => focusAtom(formAtom, (o) => o.prop("record").path(related)),
    [formAtom, related]
  );

  const valRef = useRef<DataRecord | null>();

  const refAtom: ValueAtom<DataRecord> = useMemo(() => {
    return atom(
      (get) => {
        const id = get(relatedAtom);
        if (valRef.current && valRef.current.id === +(id || 0)) {
          return valRef.current;
        }
        return { id };
      },
      (get, set, value, fireOnChange) => {
        const id = value && value.id && value.id > 0 ? value.id : null;
        valRef.current = value;
        set(relatedAtom, id);
        // trigger onchange
        const prev = get(valueAtom);
        set(valueAtom, null);
        set(valueAtom, prev, fireOnChange);
      }
    );
  }, [relatedAtom, valueAtom]);

  return <ManyToOne {...props} schema={schema} valueAtom={refAtom} />;
}
