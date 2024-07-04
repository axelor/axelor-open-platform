import { createContext, useContext } from "react";

import { DataRecord } from "@/services/client/data.types";

export const DashboardContext = createContext({} as DataRecord);

export function useDashboardContext() {
  return useContext(DashboardContext);
}
