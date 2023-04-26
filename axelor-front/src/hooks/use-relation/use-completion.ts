import uniq from "lodash/uniq";
import { useCallback, useMemo } from "react";

import { DataSource } from "@/services/client/data";
import { DataContext } from "@/services/client/data.types";

export function useCompletion(options: {
  target: string;
  targetName?: string;
  targetSearch?: string[];
  limit?: number;
}) {
  const { target, targetName, targetSearch, limit = 20 } = options;
  const dataSource = useMemo(() => new DataSource(target), [target]);
  const names = useMemo(
    () => [[targetName], targetSearch].flat().filter(Boolean) as string[],
    [targetName, targetSearch]
  );

  const search = useCallback(
    async (
      term: string,
      options?: {
        _domain?: string;
        _domainContext?: DataContext;
      }
    ) => {
      const { _domain, _domainContext } = options || {};
      return dataSource.search({
        limit,
        fields: uniq([
          "id",
          ...(targetName ? [targetName] : []),
          ...(targetSearch || []),
        ]),
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
    [dataSource, limit, names, targetName, targetSearch]
  );

  return search;
}
