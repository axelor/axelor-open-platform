import { useAtomValue, useSetAtom } from "jotai";
import React, { useCallback, useRef, useState } from "react";

import { Schema } from "@/services/client/meta.types";
import { toCamelCase } from "@/utils/names";
import { ValueAtom } from "./types";

import * as WIDGETS from "../widgets";

export function useWidget(schema: Schema) {
  const compRef = useRef<React.ElementType>();
  if (compRef.current) {
    return compRef.current;
  }

  const name = toCamelCase(schema.widget) as keyof typeof WIDGETS;
  const editName = `${name}Edit` as keyof typeof WIDGETS;
  const type = toCamelCase(schema.serverType) as keyof typeof WIDGETS;

  const Comp =
    (schema.editable && WIDGETS[editName]) || WIDGETS[name] || WIDGETS[type];

  compRef.current = Comp as React.ElementType;

  if (!(name in WIDGETS)) {
    console.log("Unknown widget:", schema.widget);
  }

  return compRef.current;
}

/**
 * This hook can be used with input fields to handle value updates.
 *
 * @param valueAtom the field value atom
 * @param options additional options
 * @returns
 */
export function useInput<T>(
  valueAtom: ValueAtom<T>,
  options: {
    /**
     * The default value to use when the atom value is null or undefined.
     */
    defaultValue: T;

    /**
     * When to fire the onChange event.
     */
    onChangeTrigger?: "blur" | "change";
  }
) {
  const { onChangeTrigger = "blur", defaultValue } = options;
  const value = useAtomValue(valueAtom) ?? defaultValue;
  const setValue = useSetAtom(valueAtom);
  const [changed, setChanged] = useState(false);

  const update = useCallback(
    (text: string, fireOnChange = false) => {
      const value = text as T;
      setValue(value, fireOnChange);
    },
    [setValue]
  );

  const onChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (e) => {
      update(e.target.value, onChangeTrigger === "change");
      setChanged(true);
    },
    [onChangeTrigger, update]
  );

  const onBlur = useCallback<React.FocusEventHandler<HTMLInputElement>>(
    (e) => {
      if (changed && onChangeTrigger === "blur") {
        setChanged(false);
        update(e.target.value, true);
      }
    },
    [changed, onChangeTrigger, update]
  );

  return {
    value,
    setValue,
    onChange,
    onBlur,
  } as const;
}
