import { View } from "./meta.types";

export type DataRecord = {
  id?: number | null;
  version?: number | null;
  $version?: number | null;
  selected?: boolean;
  [k: string]: any;
};

export type DataContext = DataRecord & {
  _id?: number;
  _ids?: number[];
  _model?: string;
  _parent?: DataContext;
  _signal?: string;
  _source?: string;
  _viewType?: string;
  _viewName?: string;
  _views?: View[];
};

export type FilterOp =
  | "="
  | "!="
  | ">"
  | ">="
  | "<"
  | "<="
  | "in"
  | "like"
  | "notLike"
  | "notIn"
  | "isNull"
  | "notNull"
  | "isEmpty"
  | "notEmpty"
  | "between"
  | "notBetween";

export interface Filter {
  operator?: FilterOp;
  fieldName?: string;
  value?: any;
  value2?: any;
}

export interface Criteria {
  operator?: "and" | "or";
  criteria?: (Filter | Criteria)[];
}
