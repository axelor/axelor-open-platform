import { DataStore } from "@/services/client/data-store";
import { ViewData } from "@/services/client/meta";
import {
  CalendarView,
  CardsView,
  FormView,
  GanttView,
  GridView,
  KanbanView,
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
    }
  : {
      meta: ViewData<T>;
    };
