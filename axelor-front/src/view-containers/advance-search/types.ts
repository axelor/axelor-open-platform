import { SearchOptions } from "@/services/client/data";
import { Criteria } from "@/services/client/data.types";
import { MetaData } from "@/services/client/meta";
import {
  Field,
  Property,
  SavedFilter,
  SearchFilter,
} from "@/services/client/meta.types";

type ContextField = {
  field?: Field;
  value?: any;
} | null;

export type AdvancedSearchState = {
  query?: SearchOptions["filter"];
  search?: Record<string, string>; // grid search column state
  editor?: Criteria & {
    id?: number;
    name?: string;
    title?: string;
    version?: number;
    shared?: boolean;
    selected?: boolean;
  };
  contextField?: ContextField;
  appliedContextField?: ContextField;
  archived?: boolean;
  searchText?: string;
  searchTextLabel?: string;
  filterType?: "all" | "single";
  domains?: SearchFilter[];
  filters?: SavedFilter[];
  items?: Property[];
  fields?: MetaData["fields"];
  jsonFields?: MetaData["jsonFields"];
  focusTabId?: string | null;
};
