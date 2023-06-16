import { WritableAtom, atom, useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { atomFamily, useAtomCallback } from "jotai/utils";
import { useCallback, useMemo } from "react";

import { Button, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { DataRecord } from "@/services/client/data.types";

import { FieldControl, FieldProps } from "../../builder";

import styles from "./json-raw.module.scss";

type JsonRawItem = {
  id: number;
  name: string;
  value: string;
};

function JsonItem({
  readonly,
  itemAtom,
  removeItem,
}: {
  readonly?: boolean;
  itemAtom: WritableAtom<JsonRawItem, [JsonRawItem], void>;
  removeItem: () => void;
}) {
  const nameAtom = useMemo(
    () => focusAtom(itemAtom, (o) => o.prop("name")),
    [itemAtom]
  );
  const valueAtom = useMemo(
    () => focusAtom(itemAtom, (o) => o.prop("value")),
    [itemAtom]
  );

  const [name, setName] = useAtom(nameAtom);
  const [value, setValue] = useAtom(valueAtom);

  const handleNameChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setName(e.target.value);
    },
    [setName]
  );

  const handleValueChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setValue(e.target.value);
    },
    [setValue]
  );

  if (readonly) {
    return (
      <div className={styles.item}>
        <div className={styles.name}>{name}</div>
        <div className={styles.value}>{value}</div>
      </div>
    );
  }

  return (
    <div className={styles.item}>
      <Input type="text" value={name} onChange={handleNameChange} />
      <Input type="text" value={value} onChange={handleValueChange} />
      <Button variant="link" onClick={removeItem}>
        <MaterialIcon icon="remove" />
      </Button>
    </div>
  );
}

export function JsonRaw(props: FieldProps<string>) {
  const { valueAtom, readonly } = props;
  const recordAtom = useMemo(
    () =>
      atom(
        (get) => {
          const value = get(valueAtom) || "{}";
          const record = JSON.parse(value) as DataRecord;
          return record;
        },
        (get, set, record: DataRecord) => {
          const value = JSON.stringify(record);
          set(valueAtom, value);
        }
      ),
    [valueAtom]
  );

  const itemsAtom = useMemo(() => {
    return atom(
      (get) => {
        const record = get(recordAtom);
        return Object.entries(record).map(
          ([name, value], index) => ({ id: index, name, value } as JsonRawItem)
        );
      },
      (get, set, items: JsonRawItem[]) => {
        const newRecord = items.reduce((acc, item) => {
          acc[item.name] = item.value;
          return acc;
        }, {} as DataRecord);
        set(recordAtom, newRecord);
      }
    );
  }, [recordAtom]);

  const itemsFamily = useMemo(() => {
    return atomFamily(
      (id: number) => {
        return atom(
          (get) => {
            const items = get(itemsAtom);
            const item = items.find((item) => item.id === id) ?? {
              id: items.length,
              name: "",
              value: "",
            };
            return item as JsonRawItem;
          },
          (get, set, value: JsonRawItem) => {
            const items = get(itemsAtom);
            const newItems = items.map((item) => {
              const found = items.find((item) => item.name === value.name);
              if (found && found.id !== id) {
                return item;
              }
              if (item.id === id) {
                return { ...item, ...value };
              }
              return item;
            });
            set(itemsAtom, newItems);
          }
        );
      },
      (a: number, b: number) => a === b
    );
  }, [itemsAtom]);

  const items = useAtomValue(itemsAtom);

  const remove = useAtomCallback(
    useCallback(
      (get, set, id: number) => {
        const newItems = items.filter((item) => item.id !== id);
        set(itemsAtom, newItems);
        itemsFamily.remove(id);
      },
      [items, itemsAtom, itemsFamily]
    )
  );

  const add = useAtomCallback(
    useCallback(
      (get, set) => {
        const items = get(itemsAtom);
        const found = items.find((item) => item.name === "");
        if (found) {
          return;
        }
        const newItems = [...items, { id: items.length, name: "", value: "" }];
        set(itemsAtom, newItems);
      },
      [itemsAtom]
    )
  );

  return (
    <FieldControl {...props} showTitle={false} className={styles.container}>
      {items.map((item) => {
        return (
          <JsonItem
            key={item.id}
            readonly={readonly}
            itemAtom={itemsFamily(item.id)}
            removeItem={() => remove(item.id)}
          />
        );
      })}
      {readonly || (
        <Button variant="link" onClick={add}>
          <MaterialIcon icon="add" />
        </Button>
      )}
    </FieldControl>
  );
}
