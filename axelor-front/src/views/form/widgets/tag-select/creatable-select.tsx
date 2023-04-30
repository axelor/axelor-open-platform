import { useCallback, useMemo, useState } from "react";
import { Select, SelectProps } from "@axelor/ui";

import { EditorOptions } from "@/hooks/use-relation";
import { DataRecord } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";
import { i18n } from "@/services/client/i18n";
import { DataStore } from "@/services/client/data-store";

const ADD_ON_OPTIONS: Record<string, number> = {
  Create: -1,
  CreateInput: -2,
  CreateAndSelectInput: -3,
};

export type CreatableSelectProps = SelectProps & {
  schema: Schema;
  canCreate?: boolean;
  onCreate?: (
    value: DataRecord,
    readonly?: boolean,
    onSelect?: EditorOptions["onSelect"]
  ) => Promise<any>;
};

export function CreatableSelect({
  schema,
  canCreate,
  onCreate,
  onChange,
  ...selectProps
}: CreatableSelectProps) {
  const [inputText, setInputText] = useState("");

  const createNames = useMemo<string[]>(
    () => schema.create?.split(/\s*,\s*/) || [],
    [schema.create]
  );
  const { target, targetName: optionLabel } = schema;

  const handleChange = useCallback(
    async (value: null | DataRecord | DataRecord[]) => {
      const isMulti = Array.isArray(value);
      const addOnOption =
        value &&
        (isMulti ? value : [value])?.find?.((v: DataRecord) =>
          Object.values(ADD_ON_OPTIONS).includes(v?.id!)
        );
      if (value && addOnOption) {
        const record: DataRecord = {};
        const updateValue = (record: DataRecord) =>
          onChange(
            isMulti
              ? value.map((v: DataRecord) => (v === addOnOption ? record : v))
              : record
          );
        if (
          [
            ADD_ON_OPTIONS.CreateInput,
            ADD_ON_OPTIONS.CreateAndSelectInput,
          ].includes(addOnOption.id!)
        ) {
          createNames?.forEach((name) => {
            record[name] = addOnOption._text;
          });
        }

        if (addOnOption.id === ADD_ON_OPTIONS.CreateAndSelectInput) {
          const ds = new DataStore(target);
          const newRecord = await ds.save(record);
          newRecord && updateValue(newRecord);
        } else if (onCreate) {
          await onCreate(record, false, updateValue);
        }
      } else {
        onChange(value);
      }
    },
    [onCreate, onChange, createNames, target]
  );

  const addOnOptions = useMemo(() => {
    if (!canCreate) return [];
    if (inputText && createNames.length) {
      return [
        {
          id: ADD_ON_OPTIONS.CreateInput,
          _text: inputText,
          [optionLabel]: i18n.get('Create "{0}"...', inputText),
        },
        {
          id: ADD_ON_OPTIONS.CreateAndSelectInput,
          _text: inputText,
          [optionLabel]: i18n.get('Create "{0}" and select...', inputText),
        },
      ];
    }
    return [
      { id: ADD_ON_OPTIONS.Create, [optionLabel]: i18n.get("Create...") },
    ];
  }, [canCreate, optionLabel, createNames, inputText]);

  return (
    <Select
      {...selectProps}
      onChange={handleChange}
      {...(canCreate && {
        addOnOptions,
      })}
      {...(createNames.length > 0 && {
        onInputChange: setInputText,
      })}
    />
  );
}
