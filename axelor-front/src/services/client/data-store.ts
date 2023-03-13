import {
  DataSource,
  DeleteOption,
  ExportResult,
  SearchOptions,
  SearchPage,
  SearchResult,
} from "./data";
import { DataRecord } from "./data.types";

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
    const page = this.#page;
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
  }

  async search(options: SearchOptions): Promise<SearchResult> {
    const opts = this.#prepareOption(options);
    const { page, records } = await super.search(opts);

    this.#options = opts;
    this.#records = records;
    this.#page = page;

    this.#notify();

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

    this.#records = this.#records.filter((x) => !ids.includes(x.id!));
    this.#page = {
      ...page,
      totalCount: totalCount - res,
    };

    this.#notify();

    return res;
  }

  async export(options: SearchOptions): Promise<ExportResult> {
    const opts = this.#prepareOption(options);
    return super.export(opts);
  }
}
