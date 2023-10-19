import { isEqual } from "lodash";

import {
  DataSource,
  DeleteOption,
  ExportResult,
  SaveOptions,
  SaveResult,
  SearchOptions,
  SearchPage,
  SearchResult,
} from "./data";
import { DataRecord } from "./data.types";

function ifDiff<T>(current: T, value: T): T {
  return isEqual(current, value) ? current : value;
}

export type DataStoreListener = (ds: DataStore) => void;

export class DataStore extends DataSource {
  #options: SearchOptions;
  #records: DataRecord[] = [];
  #page: SearchPage = {};

  #listeners: DataStoreListener[] = [];

  constructor(model: string, options: SearchOptions = {}) {
    super(model);
    this.#options = options;
    this.#page = {
      offset: options.offset ?? 0,
      limit: options.limit ?? 40,
      totalCount: 0,
    };
  }

  get options() {
    return this.#options;
  }

  get page() {
    return this.#page;
  }

  get records() {
    return this.#records;
  }

  subscribe(listener: DataStoreListener) {
    const listeners = this.#listeners;
    listeners.push(listener);
    return () => {
      const index = listeners.indexOf(listener);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    };
  }

  #notify() {
    this.#listeners.forEach((fn) => fn(this));
  }

  #prepareOption(options: SearchOptions) {
    const opts = this.#options;
    const { offset, limit } = this.#page;
    const { _domain, _domainContext } = opts?.filter ?? {};
    const filter = {
      _domain,
      _domainContext,
      ...options.filter,
    };
    return {
      ...opts,
      offset,
      limit,
      ...options,
      filter,
    };
  }

  #accept(options: SearchOptions, page: SearchPage, records: DataRecord[]) {
    const _options = ifDiff(this.#options, options);
    const _records = ifDiff(this.#records, records);
    const _page = ifDiff(this.#page, page);

    const changed =
      this.#options !== _options ||
      this.#records !== _records ||
      this.#page !== _page;

    this.#options = _options;
    this.#records = _records;
    this.#page = _page;

    if (changed) this.#notify();
  }

  async search(options: SearchOptions): Promise<SearchResult> {
    const opts = this.#prepareOption(options);
    const { page, records = [] } = await super.search(opts);

    this.#accept(opts, page, records);

    return {
      page,
      records,
    };
  }

  async delete(options: DeleteOption | DeleteOption[]): Promise<number> {
    const opts = [options].flat();
    const ids = opts.map((x) => x.id);

    const res = await super.delete(opts);

    const page = this.#page;
    const { totalCount = this.records.length } = page;

    const _records = this.#records.filter((x) => !ids.includes(x.id!));
    const _page = {
      ...page,
      totalCount: totalCount - res,
    };

    this.#accept(this.#options, _page, _records);

    return res;
  }

  async save<T extends DataRecord | DataRecord[]>(
    data: T,
    options?: SaveOptions<T>,
  ): Promise<SaveResult<T>> {
    const res = await super.save(data, options);

    if (Array.isArray(data) || Array.isArray(res) || !res) {
      return res;
    }

    let records = this.#records.slice();
    let page = this.#page;

    if (data.id) {
      records = records.map((record) =>
        record.id === (data as DataRecord).id
          ? { ...record, ...data, ...res }
          : record,
      );
    } else if (res.id && res.id !== data.id) {
      const totalCount = page.totalCount ?? this.records.length;

      records.push(res);
      page = {
        ...page,
        totalCount: totalCount + 1,
      };
    }

    this.#accept(this.#options, page, records);

    return res;
  }

  async export(options: SearchOptions): Promise<ExportResult> {
    const { limit, offset, ...opts } = this.#prepareOption({
      ...this.#options,
      ...options,
    });
    return super.export(opts);
  }
}
