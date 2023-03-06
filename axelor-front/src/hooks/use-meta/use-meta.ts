import * as meta from "@/services/client/meta";
import { ActionView } from "@/services/client/meta.types";
import { atom, useAtom } from "jotai";
import { useCallback } from "react";
import { processView, processWidgets } from "./utils";

const actionViewsAtom = atom<Record<string, ActionView>>({});
const viewsAtom = atom<Record<string, meta.ViewData<any>>>({});
const fieldsAtom = atom<Record<string, meta.MetaData>>({});

export function useMeta() {
  const [actionViews, setActionViews] = useAtom(actionViewsAtom);
  const [views, setViews] = useAtom(viewsAtom);
  const [fields, setFields] = useAtom(fieldsAtom);

  const findActionView = useCallback(
    async (name: string) => {
      let view = actionViews[name];
      if (view) {
        return view;
      }
      view = await meta.actionView(name);
      view = { ...view, name };
      setActionViews((prev) => ({ ...prev, [name]: view }));
      return view;
    },
    [actionViews, setActionViews]
  );

  const findView = useCallback(
    async ({
      type,
      name,
      model,
    }: {
      type: string;
      name?: string;
      model?: string;
    }) => {
      let key = `${type}:${name}:${model}`;
      let data = views[key];
      if (data) {
        return data;
      }

      data = await meta.view({ type: type as any, name, model });

      // process the meta data
      processView(data, data.view);
      processWidgets(data.view);

      setViews((state) => ({ ...state, [key]: data }));

      return data;
    },
    [setViews, views]
  );

  const findFields = useCallback(
    async (model: string) => {
      let res = fields[model];
      if (res) {
        return res;
      }
      res = await meta.fields(model);
      setFields((state) => ({ ...state, [model]: res }));
      return res;
    },
    [fields, setFields]
  );

  return {
    findActionView,
    findFields,
    findView,
  };
}
