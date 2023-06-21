import { DataStore } from "@/services/client/data-store";
import { View } from "@/services/client/meta.types";
import { PrimitiveAtom, atom } from "jotai";
import { GridState } from "@axelor/ui/grid";
import { createScope, molecule, useMolecule } from "jotai-molecules";

import { DataContext } from "@/services/client/data.types";
import { ActionExecutor } from "../action";

export type DashletHandler = {
  dataStore?: DataStore;
  view?: View;
  actionExecutor?: ActionExecutor;
  gridStateAtom?: PrimitiveAtom<GridState>;
  onAction?: (action: string, context?: DataContext) => Promise<any>;
  onLegendShowHide?: (show: boolean) => void;
  onExport?: () => Promise<void>;
  onRefresh?: () => Promise<void | any>;
};

export const DashletScope = createScope<DashletHandler>({});

const dashletMolecule = molecule((getMol, getScope) => {
  return atom(getScope(DashletScope));
});

export function useDashletHandlerAtom() {
  return useMolecule(dashletMolecule);
}
