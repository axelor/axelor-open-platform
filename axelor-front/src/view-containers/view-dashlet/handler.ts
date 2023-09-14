import { DataStore } from "@/services/client/data-store";
import { AdvancedSearchAtom, View } from "@/services/client/meta.types";
import { PrimitiveAtom, atom } from "jotai";
import { GridState } from "@axelor/ui/grid";
import { createScope, molecule, useMolecule } from "jotai-molecules";

import { DataContext } from "@/services/client/data.types";
import { SearchOptions } from "@/services/client/data";
import { ActionExecutor } from "../action";

export type DashletHandler = {
  dataStore?: DataStore;
  title?: string;
  view?: View;
  actionExecutor?: ActionExecutor;
  searchAtom?: AdvancedSearchAtom;
  gridStateAtom?: PrimitiveAtom<GridState>;
  onAction?: (action: string, context?: DataContext) => Promise<any>;
  onLegendShowHide?: (show: boolean) => void;
  onExport?: () => Promise<void>;
  onRefresh?: (options?: Partial<SearchOptions>) => Promise<void | any>;
};

export const DashletScope = createScope<DashletHandler>({});

const dashletMolecule = molecule((getMol, getScope) => {
  return atom(getScope(DashletScope));
});

export function useDashletHandlerAtom() {
  return useMolecule(dashletMolecule);
}
