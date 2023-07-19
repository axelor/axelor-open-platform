import { GridState } from "@axelor/ui/grid";
import { atomWithImmer } from "jotai-immer";
import { useMemo } from "react";
import { useAtom } from "jotai";

import { GridView } from "@/services/client/meta.types";

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
