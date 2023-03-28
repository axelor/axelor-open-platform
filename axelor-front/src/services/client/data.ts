import { request } from "./client";
import { Criteria, DataContext, DataRecord } from "./data.types";

export type SearchOptions = {
  limit?: number;
  offset?: number;
  sortBy?: string[];
  translate?: boolean;
  fields?: string[];
  filter?: Criteria & {
    _domain?: string;
    _domainContext?: DataContext;
  };
};

export type SearchPage = {
  offset?: number;
  limit?: number;
  totalCount?: number;
};

export type SearchResult = {
  page: SearchPage;
  records: DataRecord[];
};

export type ReadOptions = {
  fields?: string[];
  related?: {
    [K: string]: string[];
  };
};

export type DeleteOption = {
  id: number;
  version: number;
};

export type ExportResult = {
  exportSize: number;
  fileName: string;
};

export class DataSource {
  #model;

  constructor(model: string) {
    this.#model = model;
  }

  get model() {
    return this.#model;
  }

  async search(options: SearchOptions): Promise<SearchResult> {
    const url = `ws/rest/${this.model}/search`;
    const { filter: data, limit, ...rest } = options ?? {};
    const resp = await request({
      url,
      method: "POST",
      body: {
        ...rest,
        limit,
        data,
      },
    });

    if (resp.ok) {
      const {
        status,
        offset,
        total: totalCount,
        data: records = [],
      } = await resp.json();

      return status === 0
        ? {
            records,
            page: {
              offset,
              limit,
              totalCount,
            },
          }
        : Promise.reject(500);
    }

    return Promise.reject(resp.status);
  }

  async read(id: number, options?: ReadOptions): Promise<DataRecord> {
    const url = `ws/rest/${this.model}/${id}/fetch`;
    const resp = await request({
      url,
      method: "POST",
      body: options,
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data[0] : Promise.reject(500);
    }

    return Promise.reject(resp.status);
  }

  async save(data: DataRecord | DataRecord[]): Promise<DataRecord> {
    const url = `ws/rest/${this.model}`;
    const resp = await request({
      url,
      method: "POST",
      body: Array.isArray(data) ? { records: data } : { data },
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data[0] : Promise.reject(500);
    }

    return Promise.reject(resp.status);
  }

  async delete(options: DeleteOption | DeleteOption[]): Promise<number> {
    const records = Array.isArray(options) ? options : [options];
    const url = `ws/rest/${this.model}/removeAll`;
    const resp = await request({
      url,
      method: "POST",
      body: {
        records,
      },
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data.length : Promise.reject(500);
    }

    return Promise.reject(resp.status);
  }

  async copy(id: number): Promise<DataRecord> {
    const url = `ws/rest/${this.model}/${id}/copy`;
    const resp = await request({
      url,
      method: "GET",
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data[0] : Promise.reject(500);
    }

    return Promise.reject(resp.status);
  }

  async export(options: SearchOptions): Promise<ExportResult> {
    const url = `ws/rest/${this.model}/export`;
    const { filter: data, limit, ...rest } = options;
    const resp = await request({
      url,
      method: "POST",
      body: {
        ...rest,
        limit,
        data,
      },
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data : Promise.reject(500);
    }

    return Promise.reject(resp.status);
  }
}
