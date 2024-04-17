import { DataStore } from "@/services/client/data-store";
import { Column, Record } from "@axelor/ui/kanban";

export interface KanbanRecord extends Record {
  name?: string;
  text?: string;
}

export interface KanbanColumn extends Column {
  name: string;
  dataStore: DataStore;
  collapsed?: boolean;
  loading?: boolean;
  records?: KanbanRecord[];
  canEdit?: boolean;
  canDelete?: boolean;
  canCreate?: boolean;
}
