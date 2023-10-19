import { useAtom } from "jotai";
import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { Schema } from "@/services/client/meta.types";
import { toCamelCase } from "@/utils/names";
import { ValueAtom } from "./types";

import { useViewDirtyAtom } from "@/view-containers/views/scope";
import { useAtomCallback } from "jotai/utils";
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

const defaultFormatter = <T>(value?: T | null) =>
  value === null || value === undefined ? "" : String(value);

const defaultConverter = <T>(text: string) => (text ? (text as T) : null);

const defaultValidator = () => true;

/**
 * This hook can be used with input fields to handle value updates.
 *
 * @param valueAtom the field value atom
 * @param options additional options
 */
export function useInput<T>(
  valueAtom: ValueAtom<T>,
  options?: {
    /**
     * The default value to use when the atom value is null or undefined.
     */
    defaultValue?: T | null;

    /**
     * When to fire the onChange event.
     */
    onChangeTrigger?: "blur" | "change";

    /**
     * Validate the input text.
     *
     * Only validated input text will be propagated to valueAtom.
     *
     * @param text the input text to validate
     * @returns true if valid false otherwise
     */
    validate?: (text: string) => boolean;

    /**
     * Format the value to string.
     */
    format?: (value?: T | null) => string;

    /**
     * Convert the input text to target value.
     */
    parse?: (text: string) => T | null;
  },
) {
  const {
    defaultValue,
    onChangeTrigger = "blur",
    validate = defaultValidator,
    format = defaultFormatter,
    parse = defaultConverter,
  } = options ?? {};
  const [value = defaultValue, setValue] = useAtom(valueAtom);
  const valueText = useMemo(() => format(value) ?? "", [format, value]);
  const [changed, setChanged] = useState(false);
  const [text, setText] = useState(valueText);

  const dirtyAtom = useViewDirtyAtom();
  const dirtyRef = useRef<boolean>();

  const setDirty = useAtomCallback(
    useCallback(
      (get, set, dirty: boolean) => {
        if (dirtyRef.current === undefined) {
          dirtyRef.current = get(dirtyAtom);
        }
        set(dirtyAtom, dirty ? true : dirtyRef.current);
      },
      [dirtyAtom],
    ),
  );

  const update = useCallback(
    (text: string, fireOnChange: boolean) => {
      if (validate(text)) {
        setValue(parse(text), fireOnChange);
      }
    },
    [parse, setValue, validate],
  );

  const onChange = useCallback<React.ChangeEventHandler<HTMLInputElement>>(
    (event) => {
      setChanged(event.target.value !== valueText);
      setText(event.target.value);
      setDirty(event.target.value !== valueText);
      if (onChangeTrigger === "change") {
        update(event.target.value, true);
      }
    },
    [onChangeTrigger, setDirty, update, valueText],
  );

  const onBlur = useCallback<React.FocusEventHandler<HTMLInputElement>>(
    (event) => {
      dirtyRef.current = undefined;
      if (changed) {
        setChanged(false);
        if (onChangeTrigger === "blur") {
          update(event.target.value, true);
        }
      }
    },
    [changed, onChangeTrigger, update],
  );

  useEffect(() => {
    setText(() => format(value) ?? "");
  }, [format, value]);

  return {
    text,
    setText,
    value,
    setValue,
    onChange,
    onBlur,
  } as const;
}
