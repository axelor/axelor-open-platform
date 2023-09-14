import { Column, Record } from "@axelor/ui/kanban";

export interface KanbanRecord extends Record {
  name?: string;
  text?: string;
}

export interface KanbanColumn extends Column {
  collapsed?: boolean;
  loading?: boolean;
  name?: string;
  value?: any;
  records?: KanbanRecord[];
  canEdit?: boolean;
  canDelete?: boolean;
  canCreate?: boolean;
  hasMore?: boolean;
}
