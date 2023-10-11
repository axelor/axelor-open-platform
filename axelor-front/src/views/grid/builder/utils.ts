import { useAtom } from "jotai";
import { atomWithImmer } from "jotai-immer";
import { useMemo } from "react";

import { GridSortColumn, GridState } from "@axelor/ui/grid";

import { GridView } from "@/services/client/meta.types";

export function useGridState(
  initialState?: Partial<GridState> & {
    view?: GridView;
    params?: Record<string, any>;
  },
  deps = [],
) {
  const { view, params, ...gridState } = initialState || {};
  const groupBy: string = params?.groupBy || view?.groupBy;
  const orderBy: string = params?.orderBy || view?.orderBy;

  const gridAtom = useMemo(
    () =>
      atomWithImmer<GridState>({
        rows: [],
        columns: [],
        ...(groupBy && {
          groupBy: groupBy.split(",").map((name) => ({ name })),
        }),
        ...(orderBy && {
          orderBy: parseOrderBy(orderBy),
        }),
        ...gridState,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    deps,
  );
  const [state, setState] = useAtom(gridAtom);
  return [state, setState, gridAtom] as const;
}

export function parseOrderBy(orderBy?: string): GridSortColumn[] | undefined {
  return orderBy
    ?.split(",")
    .map((name) =>
      name.startsWith("-")
        ? { name: name.slice(1), order: "desc" }
        : { name, order: "asc" },
    );
}

export function getSortBy(orderBy?: GridSortColumn[] | null) {
  return orderBy?.map(
    (column) => `${column.order === "desc" ? "-" : ""}${column.name}`,
  );
}
