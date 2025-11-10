import { createContext, useContext } from "react";

import { DataRecord } from "@/services/client/data.types";

export const DashboardContext = createContext<DataRecord | null>(null);

export function useDashboardContext() {
  return useContext(DashboardContext);
}
