import { useAtomValue, useSetAtom } from "jotai";
import React, { useCallback, useRef, useState } from "react";

import { useAsyncEffect } from "@/hooks/use-async-effect";
import { Schema } from "@/services/client/meta.types";
import { Cache } from "@/utils/cache";
import { toCamelCase, toKebabCase } from "@/utils/names";
import { ValueAtom } from "./types";

const cache = new Cache<React.ElementType>();

const loadWidget = async (type: string) => {
  const module = toKebabCase(type);
  const name = toCamelCase(type);
  try {
    const { [name]: Comp } = await import(`../widgets/${module}/index.ts`);
    return Comp as React.ElementType;
  } catch (error) {
    console.error(`Unable to load widget ${type}`);
    return Promise.reject(error);
  }
};

export function useLazyWidget(schema: Schema) {
  const name = schema.widget || schema.serverType;
  const componentRef = useRef<React.ElementType | undefined>(cache.get(name));
  const [loading, setLoading] = useState(false);
  useAsyncEffect(async () => {
    if (componentRef.current || loading) return;
    setLoading(true);
    try {
      componentRef.current = await loadWidget(name).catch(() =>
        loadWidget(schema.serverType)
      );
      cache.put(name, componentRef.current);
    } finally {
      setLoading(false);
    }
  }, [schema]);
  return {
    loading,
    Comp: componentRef.current,
  };
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
  const value = useAtomValue(valueAtom) || defaultValue;
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
