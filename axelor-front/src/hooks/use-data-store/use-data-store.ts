import {
  DataSource,
  DeleteOption,
  ReadOptions,
  SearchOptions,
  SearchPage,
} from "@/services/client/data";
import { DataRecord } from "@/services/client/data.types";
import { useCallback, useMemo, useState } from "react";

export interface DataStore extends DataSource {
  readonly page: SearchPage;
  readonly records: DataRecord[];
  readonly options: SearchOptions;
}

export function useDataStore(model: string): DataStore;
export function useDataStore(model: string, options: SearchOptions): DataStore;
export function useDataStore(
  model: string,
  options: SearchOptions = {}
): DataStore {
  const [records, setRecords] = useState<DataRecord[]>([]);
  const [page, setPage] = useState<SearchPage>({
    limit: 40,
  });
  const [opts, setOpts] = useState<SearchOptions>(options);

  const ds = useMemo(() => new DataSource(model), [model]);

  const prepareOptions = useCallback(
    (options: SearchOptions) => {
      // have to re-use previous domain and context
      const { _domain, _domainContext } = opts?.filter ?? {};
      const filter = {
        _domain,
        _domainContext,
        ...options.filter,
      };
      return {
        ...opts,
        ...page,
        ...options,
        filter,
      };
    },
    [opts, page]
  );

  const doSearch = useCallback(
    async (options: SearchOptions) => {
      const args = prepareOptions(options);
      const res = await ds.search(args);

      // preserve updated options
      setOpts(args);

      // preserve result
      setRecords(res.records);
      setPage(res.page);

      return res;
    },
    [prepareOptions, ds, setOpts, setRecords, setPage]
  );

  const doRead = useCallback(
    async (id: number, options?: ReadOptions) => {
      const { fields } = opts;
      const args = {
        fields,
        ...options,
      };
      return await ds.read(id, args);
    },
    [ds, opts]
  );

  const doSave = useCallback(
    async (record: DataRecord) => {
      return ds.save(record);
    },
    [ds]
  );

  const doCopy = useCallback(
    async (id: number) => {
      return ds.copy(id);
    },
    [ds]
  );

  const doDelete = useCallback(
    async (options: DeleteOption | DeleteOption[]) => {
      const ids = Array.isArray(options)
        ? options.map((x) => x.id)
        : [options.id];
      const res = await ds.delete(options);
      const data = records.filter((x) => !ids.includes(x.id!));

      setRecords(data);
      setPage((prev) => {
        const { totalCount = records.length } = prev;
        return {
          ...prev,
          totalCount: totalCount - res,
        };
      });

      return res;
    },
    [ds, records, setRecords, setPage]
  );

  const doExport = useCallback(
    async (options: SearchOptions) => {
      const args = prepareOptions(options);
      return ds.export(args);
    },
    [ds, prepareOptions]
  );

  return {
    model,
    page,
    records,
    options: opts,
    search: doSearch,
    read: doRead,
    save: doSave,
    copy: doCopy,
    delete: doDelete,
    export: doExport,
  } as DataStore;
}
