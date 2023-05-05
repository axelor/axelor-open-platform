import { GridState } from "@axelor/ui/grid";
import { useAtom } from "jotai";
import { atomWithImmer } from "jotai-immer";
import { useMemo } from "react";

import { GridActionHandler } from "./scope";
import { GridView, View } from "@/services/client/meta.types";
import { DataContext } from "@/services/client/data.types";
import { DefaultActionExecutor } from "@/view-containers/action";

export function useGridState(
  initialState?: Partial<GridState> & { view?: GridView },
  deps = []
) {
  const { view, ...gridState } = initialState || {};
  return useAtom(
    useMemo(
      () =>
        atomWithImmer<GridState>({
          rows: [],
          columns: [],
          ...(view?.groupBy && {
            groupBy: [{ name: view.groupBy }],
          }),
          ...(view?.orderBy && {
            orderBy: view.orderBy
              .split(",")
              .map((name) =>
                name.startsWith("-")
                  ? { name: name.slice(1), order: "desc" }
                  : { name, order: "asc" }
              ),
          }),
          ...gridState,
        }),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      deps
    )
  );
}

/**
 * Hook to create grid action executor
 *
 * @param view the grid meta view
 * @param context predefined context if any
 * @returns action executor to execute action
 */
export function useGridActionExecutor(
  view: View,
  getContext?: () => DataContext
) {
  return useMemo(() => {
    const actionHandler = new GridActionHandler(() => ({
      ...getContext?.(),
      _viewName: view.name,
      _model: view.model,
    }));
    return new DefaultActionExecutor(actionHandler);
  }, [getContext, view]);
}
