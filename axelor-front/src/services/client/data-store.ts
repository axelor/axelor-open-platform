import getObjValue from "lodash/get";
import isEqual from "lodash/isEqual";
import isPlainObject from "lodash/isPlainObject";

import {
  DataSource,
  DeleteOption,
  ExportResult,
  ReadOptions,
  SaveOptions,
  SaveResult,
  SearchOptions,
  SearchPage,
  SearchResult,
} from "./data";
import { DataContext, DataRecord } from "./data.types";
import { session } from "./session";

function ifDiff<T>(current: T, value: T): T {
  return isEqual(current, value) ? current : value;
}

function updateRecord(
  initialRecord: DataRecord,
  record: DataRecord,
): DataRecord {
  const rec: DataRecord = {};

  Object.keys(initialRecord).forEach((fieldName) => {
    const fieldValue = record[fieldName];
    if (fieldName.includes(".")) {
      const nestValue = getObjValue(record, fieldName.split("."));
      rec[fieldName] =
        nestValue !== undefined ? nestValue : initialRecord[fieldName];
    } else if (
      fieldValue &&
      initialRecord[fieldName] &&
      typeof fieldValue === "object" &&
      !Array.isArray(fieldValue)
    ) {
      rec[fieldName] = updateRecord(
        initialRecord[fieldName],
        fieldValue as DataRecord,
      );
    } else {
      rec[fieldName] = Object.hasOwn(record, fieldName)
        ? fieldValue
        : initialRecord[fieldName];
    }
  });

  return rec;
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
      limit:
        options.limit ?? session.info?.api?.pagination?.defaultPerPage ?? 40,
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

  #setVersion(context: DataContext, nested = false) {
    if (!isPlainObject(context)) {
      return context;
    }
    return Object.entries(context).reduce((acc, [key, value]) => {
      if (isPlainObject(value) && value.version !== undefined) {
        const { version: $version, ...rest } = value;
        value = { $version, ...rest };
        value = this.#setVersion(value, true);
      }
      if (Array.isArray(value)) {
        value = value.map((item) => this.#setVersion(item, true));
      }
      if (nested && key === "version") {
        return acc;
      }
      return { ...acc, [key]: value };
    }, {} as DataContext);
  }

  #prepareOption(options: SearchOptions) {
    const opts = this.#options;
    const { offset, limit } = this.#page;
    const { _domain, _domainContext } = opts?.filter ?? {};
    const filter = {
      _domain,
      _domainContext: _domainContext
        ? this.#setVersion(_domainContext)
        : undefined,
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

  async read(
    id: number,
    options?: ReadOptions,
    silent?: boolean,
  ): Promise<DataRecord> {
    const res = await super.read(id, options, silent);
    if (res) {
      const records = this.records.slice();
      const recInd = records.findIndex((r) => r.id === res.id);
      if (recInd > -1) {
        records[recInd] = updateRecord(records[recInd], res);
        this.#accept(this.#options, this.#page, records);
      }
    }
    return res;
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
      const recInd = records.findIndex((r) => r.id === res.id);
      if (recInd > -1) {
        records[recInd] = updateRecord(records[recInd], { ...data, ...res });
      }
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
