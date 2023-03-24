import { atom, useAtomValue, useSetAtom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";

import { DataContext } from "@/services/client/data.types";
import { ActionExecutor, DefaultActionHandler } from "@/view-containers/action";

import { Schema } from "@/services/client/meta.types";
import { useEffect, useRef } from "react";
import { FormAtom, WidgetAtom } from "./types";

type ContextCreator = () => DataContext;
type ActionListener = (data: any) => void | boolean;

export class FormActionHandler extends DefaultActionHandler {
  #prepareContext: ContextCreator;
  #listeners: Map<string, Set<ActionListener>> = new Map();

  constructor(prepareContext: ContextCreator) {
    super();
    this.#prepareContext = prepareContext;
  }

  subscribe(event: string, subscriber: ActionListener) {
    const listeners =
      this.#listeners.get(event) ??
      this.#listeners.set(event, new Set()).get(event)!;
    listeners.add(subscriber);
    return () => {
      listeners.delete(subscriber);
    };
  }

  #notify(event: string, data: any) {
    const listeners = this.#listeners.get(event) ?? [];
    for (const fn of listeners) {
      if (fn(data) === false) {
        break;
      }
    }
  }

  getContext() {
    return this.#prepareContext();
  }

  setAttr(target: string, name: string, value: any) {
    const event = `attr:change:${target}`;
    const data = {
      [name]: value,
    };
    this.#notify(event, data);
  }

  setFocus(target: string) {
    this.#notify(`focus:${target}`, true);
  }
}

type FormScopeState = {
  actionHandler: FormActionHandler;
  actionExecutor: ActionExecutor;
  formAtom: FormAtom;
};

export const FormScope = createScope<FormScopeState>({} as any); // we'll always provide initialValue

const formMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(FormScope);
  return atom(initialView);
});

export function useFormScope() {
  const scopeAtom = useMolecule(formMolecule);
  return useAtomValue(scopeAtom);
}

export function useHandleAttrs(schema: Schema, widgetAtom: WidgetAtom) {
  const canceledRef = useRef<boolean>(false);

  useEffect(() => {
    canceledRef.current = false;
    return () => {
      canceledRef.current = true;
    };
  });

  const { actionHandler } = useFormScope();
  const setAttrs = useSetAtom(widgetAtom);

  useEffect(() => {
    if (canceledRef.current) return;
    if (schema.name) {
      return actionHandler.subscribe(`attr:change:${schema.name}`, (data) => {
        if (canceledRef.current) return;
        setAttrs((prev) => {
          const { attrs } = prev;
          return { ...prev, attrs: { ...attrs, ...data } };
        });
      });
    }
  }, [actionHandler, schema.name, setAttrs]);
}
