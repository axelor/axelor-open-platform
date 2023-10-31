import { SetStateAction, atom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { useMemo, useRef } from "react";
import isObject from "lodash/isObject";

import { Box } from "@axelor/ui";

import { useAsync } from "@/hooks/use-async";
import { DataRecord } from "@/services/client/data.types";
import { findFields } from "@/services/client/meta-cache";
import {
  Schema,
  Selection as SelectionType,
} from "@/services/client/meta.types";

import { createWidgetAtom } from "../../builder/atoms";
import { FieldControl, FieldProps, ValueAtom } from "../../builder";
import { ManyToOne } from "../many-to-one";
import { Selection } from "../selection";

export function RefSelect(props: FieldProps<any>) {
  const { schema, formAtom, valueAtom: _valueAtom, readonly } = props;

  const relatedAtom = useMemo(() => {
    const related = schema.related ?? schema.name + "Id";
    return focusAtom(formAtom, (o) => o.prop("record").path(related));
  }, [formAtom, schema]);

  const valueAtom = useMemo(() => {
    return atom(
      (get) => get(_valueAtom),
      (
        get,
        set,
        update: SetStateAction<any>,
        markDirty?: boolean,
        fireOnChange?: boolean,
      ) => {
        const value =
          typeof update === "function" ? update(get(_valueAtom)) : update;
        set(_valueAtom, value, markDirty, fireOnChange);
        set(relatedAtom, null);
      },
    );
  }, [_valueAtom, relatedAtom]);

  const selection = useMemo(() => {
    const items: SelectionType[] = schema.selectionList || [];
    return items.reduce(
      (acc, item) => {
        acc[item.value!] = item;
        return acc;
      },
      {} as Record<string, SelectionType>,
    );
  }, [schema.selectionList]);

  const selectSchema = useMemo(
    () => ({ ...schema, showTitle: false }),
    [schema],
  );

  const isRefLink = schema.widget === "ref-link";
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
    [domain, formView, gridView, schema, target],
  );

  return (
    <FieldControl {...props}>
      <Box
        d="flex"
        g={3}
        flexDirection={{
          base: "column",
          md: "row",
        }}
        overflow="hidden"
      >
        {(!isRefLink || !readonly) && (
          <Box flex={1} overflow="hidden" style={{ maxWidth: 200 }}>
            <Selection {...props} schema={selectSchema} valueAtom={valueAtom} />
          </Box>
        )}
        <Box flex={1} overflow="hidden">
          {target && (
            <RefItem
              {...props}
              schema={refSchema}
              relatedValueAtom={relatedAtom}
            />
          )}
        </Box>
      </Box>
    </FieldControl>
  );
}

function RefItem(
  props: FieldProps<any> & {
    relatedValueAtom: ValueAtom<any>;
  },
) {
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

function RefItemInner(
  props: FieldProps<any> & {
    targetName: string;
    relatedValueAtom: ValueAtom<any>;
  },
) {
  const {
    formAtom,
    relatedValueAtom,
    valueAtom,
    schema: _schema,
    targetName,
  } = props;
  const related = _schema.related ?? _schema.name + "Id";
  const schema = useMemo(
    () => ({ ..._schema, name: related, targetName }),
    [_schema, related, targetName],
  );

  const valRef = useRef<DataRecord | null>();

  const refAtom: ValueAtom<DataRecord> = useMemo(() => {
    return atom(
      (get) => {
        const id = get(relatedValueAtom);
        if (isObject(id)) return id;
        if (valRef.current && valRef.current.id === +(id || 0)) {
          return valRef.current;
        }
        return id ? { id } : null;
      },
      (get, set, value, fireOnChange, markDirty) => {
        const id = value && value.id && value.id > 0 ? value.id : null;
        valRef.current = value;
        set(relatedValueAtom, null);
        set(relatedValueAtom, id);
        // // trigger onchange
        const prev = get(valueAtom);
        set(valueAtom, null, false, false);
        set(valueAtom, prev, fireOnChange, markDirty);
      },
    );
  }, [relatedValueAtom, valueAtom]);

  const widgetAtom = useMemo(
    () => createWidgetAtom({ schema, formAtom }),
    [formAtom, schema],
  );

  return (
    <ManyToOne
      {...props}
      key={(schema as Schema).target}
      schema={schema}
      valueAtom={refAtom}
      widgetAtom={widgetAtom}
    />
  );
}
