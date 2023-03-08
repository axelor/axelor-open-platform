import { Tab } from "@/hooks/use-tabs";
import { ViewData } from "@/services/client/meta";
import { View } from "@/services/client/meta.types";

export interface ViewProps<T extends View> {
  tab: Tab;
  meta: ViewData<T>;
  component: React.ElementType;
}
