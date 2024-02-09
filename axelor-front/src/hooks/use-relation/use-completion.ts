import uniq from "lodash/uniq";
import { useCallback, useMemo } from "react";

import { DataSource } from "@/services/client/data";
import { DataContext } from "@/services/client/data.types";
import { DEFAULT_COMPLETION_PAGE_SIZE } from "@/utils/app-settings.ts";

export function useCompletion(options: {
  target: string;
  targetName?: string;
  targetSearch?: string[];
  limit?: number;
  sortBy?: string;
}) {
  const { target, targetName, targetSearch, sortBy, limit = DEFAULT_COMPLETION_PAGE_SIZE } = options;
  const dataSource = useMemo(() => new DataSource(target), [target]);
  const names = useMemo(
    () => uniq([[targetName], targetSearch].flat().filter(Boolean)) as string[],
    [targetName, targetSearch],
  );

  const search = useCallback(
    async (
      term: string,
      options?: {
        _domain?: string;
        _domainContext?: DataContext;
      },
    ) => {
      const { _domain, _domainContext } = options || {};
      return dataSource.search({
        translate: true,
        sortBy: sortBy?.split?.(","),
        limit,
        fields: uniq(["id", ...names]),
        filter: {
          _domain,
          _domainContext,
          ...(term
            ? {
                operator: "or",
                criteria: names.map((name) => ({
                  fieldName: name,
                  operator: "like",
                  value: term,
                })),
              }
            : {}),
        },
      });
    },
    [dataSource, sortBy, limit, names],
  );

  return search;
}
