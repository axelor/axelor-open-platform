import { DataRecord } from "@/services/client/data.types";

export type TreeRecord = DataRecord & {
  _level?: number;
  _selected?: boolean;
  _expand?: boolean;
  _children?: TreeRecord[];
};
