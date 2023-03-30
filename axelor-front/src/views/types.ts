import { DataStore } from "@/services/client/data-store";
import { ViewData } from "@/services/client/meta";
import {
  CalendarView,
  CardsView,
  FormView,
  GanttView,
  GridView,
  KanbanView,
  SavedFilter,
  SearchFilter,
  View,
} from "@/services/client/meta.types";

export type ViewProps<T extends View> = T extends
  | GridView
  | FormView
  | CardsView
  | KanbanView
  | CalendarView
  | GanttView
  ? {
      meta: ViewData<T>;
      dataStore: DataStore;
      filters?: SavedFilter[];
      domains?: SearchFilter[];
    }
  : {
      meta: ViewData<T>;
    };
