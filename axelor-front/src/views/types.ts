import { DataStore } from "@/hooks/use-data-store";
import { ViewData } from "@/services/client/meta";
import { View } from "@/services/client/meta.types";

export interface ViewProps<T extends View> {
  meta: ViewData<T>;
  dataStore?: DataStore;
}
