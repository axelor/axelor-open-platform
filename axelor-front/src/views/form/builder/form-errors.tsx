import { useStore } from "jotai";
import uniq from "lodash/uniq";
import { useCallback } from "react";

import { alerts } from "@/components/alerts";
import { i18n } from "@/services/client/i18n";

import { FormState, WidgetErrors, WidgetState } from "./types";

// Error priority keys in descending priority order.
// Only the highest-priority error per field is shown.
const ERROR_PRIORITY: (keyof WidgetErrors)[] = [
  "required",
  "min",
  "max",
  "pattern",
  "errors",
  "invalid",
  "error",
];

export function getTopError(
  fieldErrors: WidgetErrors,
): string | string[] | undefined {
  for (const key of ERROR_PRIORITY) {
    const val = fieldErrors[key];
    if (val) return val;
  }
}

export const showErrors = (errors: WidgetErrors[]) => {
  const titles = uniq(
    errors.flatMap((e) => {
      const top = getTopError(e);
      if (!top) return [];
      return Array.isArray(top) ? top : [top];
    }),
  );
  if (titles.length) {
    alerts.error({
      message: (
        <ul>
          {titles.map((title, i) => (
            <li key={i}>{title}</li>
          ))}
        </ul>
      ),
    });
  }
};

export const useGetErrors = () => {
  const store = useStore();
  return useCallback(
    (formState: FormState) => {
      const { states, statesByName = {} } = formState;
      const isHidden = function isHidden(s: WidgetState): boolean {
        return Boolean(
          s.attrs.hidden ||
          (s.name && statesByName[s.name]?.attrs?.hidden) ||
          (s.parent && isHidden(store.get(s.parent))),
        );
      };

      const serverErrorNames = new Set(
        Object.keys(statesByName)
          .filter((k) => statesByName[k].errors?.error)
          .filter((v) => Object.values(states).some((w) => w.name === v)),
      );

      const errors: WidgetErrors[] = [];

      Object.values(states)
        .filter((s) => !isHidden(s))
        .forEach((s) => {
          const widgetErrors = { ...(s.errors ?? {}) };
          const hasClientErrors =
            Object.keys(widgetErrors).length > 0 && s.valid !== true;
          let hasMergedServerError = false;

          // Merge server error into the same field's error object
          if (s.name && serverErrorNames.has(s.name)) {
            const title =
              s.attrs?.title || formState.fields[s.name]?.title || s.name;
            widgetErrors.error = i18n.get(`{0} is invalid`, title);
            serverErrorNames.delete(s.name);
            hasMergedServerError = true;
          }

          if (hasClientErrors || hasMergedServerError) {
            errors.push(widgetErrors);
          }
        });

      return errors.length ? errors : null;
    },
    [store],
  );
};
