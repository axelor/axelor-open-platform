import { atom, useAtomValue, useSetAtom } from "jotai";
import { createScope, molecule, useMolecule } from "jotai-molecules";
import { useCallback, useEffect, useRef, useSyncExternalStore } from "react";

import { DataContext } from "@/services/client/data.types";
import { Schema } from "@/services/client/meta.types";
import {
  ActionAttrData,
  ActionData,
  ActionExecutor,
  ActionHandler,
  DefaultActionExecutor,
  DefaultActionHandler,
} from "@/view-containers/action";

import { fallbackFormAtom } from "./atoms";
import { FormAtom, WidgetAtom } from "./types";

type ContextCreator = () => DataContext;

export class FormActionHandler extends DefaultActionHandler {
  #prepareContext: ContextCreator;

  constructor(prepareContext: ContextCreator) {
    super();
    this.#prepareContext = prepareContext;
  }

  getContext() {
    return this.#prepareContext();
  }

  setAttr(target: string, name: string, value: any) {
    this.notify({
      type: "attr",
      target,
      name,
      value,
    });
  }

  setFocus(target: string) {
    this.notify({
      type: "focus",
      target,
      value: true,
    });
  }
}

type FormScopeState = {
  actionHandler: ActionHandler;
  actionExecutor: ActionExecutor;
  formAtom: FormAtom;
};

const fallbackHandler = new FormActionHandler(() => ({}));
const fallbackExecutor = new DefaultActionExecutor(fallbackHandler);

export const FormScope = createScope<FormScopeState>({
  actionHandler: fallbackHandler,
  actionExecutor: fallbackExecutor,
  formAtom: fallbackFormAtom,
});

const formMolecule = molecule((getMol, getScope) => {
  const initialView = getScope(FormScope);
  return atom(initialView);
});

export function useFormScope() {
  const scopeAtom = useMolecule(formMolecule);
  return useAtomValue(scopeAtom);
}

function useActionData<T extends ActionData>(
  check: (data: ActionData) => boolean,
  handler: (data: T) => void
) {
  const { actionHandler } = useFormScope();
  const dataRef = useRef<ActionData | null>(null);
  const doneRef = useRef<boolean>(false);

  const subscribe = useCallback(
    (callback: () => any) => {
      return actionHandler.subscribe((data) => {
        if (check(data)) {
          dataRef.current = data;
          callback();
        }
      });
    },
    [actionHandler, check]
  );

  const data = useSyncExternalStore(subscribe, () => dataRef.current);

  useEffect(() => {
    doneRef.current = false;
    return () => {
      doneRef.current = true;
    };
  });

  useEffect(() => {
    if (doneRef.current) return;
    if (data) {
      handler(data as T);
    }
  }, [handler, data]);
}

export function useActionAttrs(schema: Schema, widgetAtom: WidgetAtom) {
  const setAttrs = useSetAtom(widgetAtom);
  useActionData<ActionAttrData>(
    useCallback(
      (data) => data.target === schema.name && data.type === "attr",
      [schema.name]
    ),
    useCallback(
      (data) => {
        setAttrs((prev) => {
          const { attrs } = prev;
          const { name, value } = data;
          return {
            ...prev,
            attrs: { ...attrs, [name]: value },
          };
        });
      },
      [setAttrs]
    )
  );
}
