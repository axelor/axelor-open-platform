import { SearchOptions } from "@/services/client/data";
import { Criteria } from "@/services/client/data.types";
import { MetaData } from "@/services/client/meta";
import { Field, SavedFilter, SearchFilter } from "@/services/client/meta.types";

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
  contextField?: {
    name?: string;
    value?: any;
  };
  archived?: boolean;
  searchText?: string;
  searchTextLabel?: string;
  filterType?: "all" | "single";
  domains?: SearchFilter[];
  filters?: SavedFilter[];
  items?: Field[];
  fields?: MetaData["fields"];
  jsonFields?: MetaData["jsonFields"];
  focusTabId?: string | null;
};
