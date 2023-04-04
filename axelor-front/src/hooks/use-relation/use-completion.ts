import { useCallback, useMemo } from "react";

import { DataSource } from "@/services/client/data";
import { DataContext } from "@/services/client/data.types";

export function useCompletion(options: {
  target: string;
  targetName?: string;
  targetSearch?: string[];
  domain?: string;
  context?: DataContext;
  limit?: number;
}) {
  const {
    target,
    targetName,
    targetSearch,
    domain,
    context,
    limit = 20,
  } = options;
  const dataSource = useMemo(() => new DataSource(target), [target]);
  const names = useMemo(
    () => [[targetName], targetSearch].flat().filter(Boolean) as string[],
    [targetName, targetSearch]
  );

  const search = useCallback(
    async (term: string) => {
      return dataSource.search({
        limit,
        filter: {
          _domain: domain,
          _domainContext: context,
          operator: "or",
          criteria: names.map((name) => ({
            fieldName: name,
            operator: "like",
            value: term,
          })),
        },
      });
    },
    [context, dataSource, domain, limit, names]
  );

  return search;
}
