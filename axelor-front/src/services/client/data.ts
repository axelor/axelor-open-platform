import { readCookie, request } from "./client";
import { Criteria, DataContext, DataRecord } from "./data.types";
import { SearchFilter } from "./meta.types";
import { ErrorReport, reject } from "./reject";

export type SearchOptions = {
  limit?: number;
  offset?: number;
  sortBy?: string[];
  translate?: boolean;
  fields?: string[];
  filter?: Criteria & {
    _domains?: SearchFilter[];
    _domain?: string;
    _domainContext?: DataContext;
    _domainAction?: string;
    _archived?: boolean;
    _searchText?: string;
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

export type SaveOptions<T extends DataRecord | DataRecord[]> = ReadOptions & {
  onError?: (error: ErrorReport) => Promise<SaveResult<T>>;
};

export type SaveResult<T extends DataRecord | DataRecord[]> =
  T extends DataRecord[] ? DataRecord[] : DataRecord;

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

  async read(
    id: number,
    options?: ReadOptions,
    silent?: boolean,
  ): Promise<DataRecord> {
    const url = `ws/rest/${this.model}/${id}/fetch`;
    const resp = await request({
      url,
      method: "POST",
      body: options,
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data[0] : reject(silent ? null : data);
    }

    return Promise.reject(resp.status);
  }

  async save<T extends DataRecord | DataRecord[]>(
    data: T,
    options?: SaveOptions<T>,
  ): Promise<SaveResult<T>> {
    const { onError } = options ?? {};

    if (!Array.isArray(data) && data?.$upload) {
      const upload = data.$upload;
      return this.upload(
        data,
        upload.field,
        upload.file,
      ) as Promise<SaveResult<T>>;
    }

    const isRecords = Array.isArray(data);
    const url = `ws/rest/${this.model}`;
    const resp = await request({
      url,
      method: "POST",
      body: isRecords ? { records: data, ...options } : { data, ...options },
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      if (status === 0) {
        return isRecords ? data : data[0];
      }
      return onError ? onError(data).catch(reject) : reject(data);
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
      return status === 0 ? data.length : reject(data);
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
    const { filter: data, ...rest } = options;
    const resp = await request({
      url,
      method: "POST",
      body: {
        ...rest,
        data,
      },
    });

    if (resp.ok) {
      const { status, data } = await resp.json();
      return status === 0 ? data : Promise.reject(500);
    }

    return Promise.reject(resp.status);
  }

  async verify(values: DataRecord) {
    const url = `ws/rest/${this.model}/verify`;
    const resp = await request({
      url,
      method: "POST",
      body: {
        data: values,
      },
    });
    if (resp.ok) {
      const { status } = await resp.json();
      return status === 0;
    }
    return false;
  }

  async upload(
    data: DataRecord,
    field: string | Blob,
    file: File,
    onProgress?: (complete?: number) => void,
  ): Promise<DataRecord> {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    const url = `ws/rest/${this.model}/upload`;

    formData.append("file", file);
    formData.append("field", field);
    formData.append("request", JSON.stringify({ data }));

    return new Promise<DataRecord>(function (resolve, reject) {
      if (onProgress) {
        xhr.upload.addEventListener(
          "progress",
          function (e) {
            const complete = Math.round((e.loaded * 100) / e.total);
            onProgress(complete);
          },
          false,
        );
      }

      xhr.onerror = reject;
      xhr.onabort = reject;

      xhr.onload = function () {
        let data: any = {};
        try {
          data = JSON.parse(xhr.response || xhr.responseText);
        } catch {
          // ignore
        }

        const response = {
          data,
          status: xhr.status,
        };

        if (xhr.status === 200) {
          if (data?.status === 0) {
            resolve(data?.data[0]);
          } else {
            reject(500);
          }
        } else {
          reject(response.status);
        }
      };

      xhr.open("POST", url, true);
      xhr.setRequestHeader("X-CSRF-Token", readCookie("CSRF-TOKEN")!);
      xhr.send(formData);
    });
  }
}
