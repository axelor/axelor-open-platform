import { GridState } from "@axelor/ui/grid";
import { useAtom } from "jotai";
import { atomWithImmer } from "jotai-immer";
import { useMemo } from "react";

export function useGridState(initialState?: Partial<GridState>, deps = []) {
  return useAtom(
    useMemo(
      () =>
        atomWithImmer<GridState>({
          rows: [],
          columns: [],
          ...initialState,
        }),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      deps
    )
  );
}
