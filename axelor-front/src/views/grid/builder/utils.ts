import { GridState } from "@axelor/ui/grid";
import { atomWithImmer } from "jotai-immer";
import { useMemo } from "react";
import { useAtom } from "jotai";

import { DataContext } from "@/services/client/data.types";
import { GridView, View } from "@/services/client/meta.types";
import { DefaultActionExecutor } from "@/view-containers/action";
import { GridActionHandler } from "./scope";

export function useGridState(
  initialState?: Partial<GridState> & { view?: GridView },
  deps = []
) {
  const { view, ...gridState } = initialState || {};
  const gridAtom = useMemo(
    () =>
      atomWithImmer<GridState>({
        rows: [],
        columns: [],
        ...(view?.groupBy && {
          groupBy: view.groupBy.split(",").map((name) => ({ name })),
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
  );
  const [state, setState] = useAtom(gridAtom);
  return [state, setState, gridAtom] as const;
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
  options?: {
    onRefresh?: () => Promise<any>;
    getContext?: () => DataContext;
  }
) {
  const { onRefresh, getContext } = options || {};
  return useMemo(() => {
    const actionHandler = new GridActionHandler(() => ({
      ...getContext?.(),
      _viewName: view.name,
      _model: view.model,
    }));

    actionHandler.refresh = async () => {
      await onRefresh?.();
    };

    return new DefaultActionExecutor(actionHandler);
  }, [getContext, onRefresh, view.model, view.name]);
}
