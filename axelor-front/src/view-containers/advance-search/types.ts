import { Criteria } from "@/services/client/data.types";

export type AdvancedSearchState = {
  query?: Criteria & {
    archived?: boolean;
    freeSearchText?: string;
  };
  selected?: any[];
  state: {
    search?: Record<string, string>;
    activeFilters?: any[];
    contentField?: {
      name?: string;
      value?: any;
    };
    customFilter?: any;
    isSingleFilter?: boolean;
  };
};
