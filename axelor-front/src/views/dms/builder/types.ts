import { DataRecord } from "@/services/client/data.types";

export type TreeRecord = DataRecord & {
  _level?: number;
  _selected?: boolean;
  _expand?: boolean;
  _children?: TreeRecord[];
};

export const DMS_NODE_TYPE = "DMS_NODE_TYPE";