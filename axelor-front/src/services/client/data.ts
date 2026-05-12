import { dialogs } from "@/components/dialogs";
import { readCookie, request } from "./client";
import { Criteria, DataContext, DataRecord } from "./data.types";
import { i18n } from "./i18n";
import { Perms, SearchFilter } from "./meta.types";
import { ErrorReport, reject, rejectAsAlert } from "./reject";

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
  };
};

export type SearchInit = {
  signal?: AbortSignal;
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

export type SelectOptions = {
  [K: string]: boolean | SelectOptions;
};

export type ReadOptions = {
  fields?: string[];
  related?: {
    [K: string]: string[];
  };
  select?: SelectOptions;
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

export type AccessType = "read" | "write" | "create" | "remove" | "export";

export type UploadItem = {
  field: string;
  file: File;
};

export type UploadValue = UploadItem | UploadItem[];

type UploadErrorPayload = {
  status?: number;
  data?: ErrorReport | string | null;
  error?: ErrorReport | string | null;
};

function getUploadError(data: unknown): number | string | ErrorReport | null {
  if (data == null) {
    return null;
  }

  if (typeof data === "number" || typeof data === "string") {
    return data;
  }

  const payload = data as UploadErrorPayload;

  if (payload.error) {
    return payload.error;
  }

  if (payload.status !== 0 && payload.data) {
    return payload.data;
  }

  return payload.data ?? null;
}

export class DataSource {
  #model;

  constructor(model: string) {
    this.#model = model;
  }

  get model() {
    return this.#model;
  }

  async search(
    options: SearchOptions,
    init?: SearchInit,
  ): Promise<SearchResult> {
    const url = `ws/rest/${this.model}/search`;
    const { filter: data, limit, ...rest } = options ?? {};
    const resp = await request({
      ...init,
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
        : rejectAsAlert(records);
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
    const { onError, ...saveOptions } = options ?? {};

    if (!Array.isArray(data) && data?.$upload) {
      const uploads = Array.isArray(data.$upload)
        ? data.$upload
        : [data.$upload];
      const result = await this.upload(data as DataRecord, uploads);
      return result as SaveResult<T>;
    }

    const isRecords = Array.isArray(data);
    const url = `ws/rest/${this.model}`;
    const resp = await request({
      url,
      method: "POST",
      body: isRecords
        ? { records: data, ...saveOptions }
        : { data, ...saveOptions },
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
      return status === 0 ? data[0] : reject(data);
    }

    return Promise.reject(resp.status);
  }

  async export(options: SearchOptions): Promise<ExportResult> {
    const url = `ws/rest/${this.model}/export`;
    const { filter: data, translate: _translate, ...rest } = options;
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
      return status === 0 ? data : reject(data);
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
    uploads: UploadItem[],
    onProgress?: (complete?: number) => void,
  ): Promise<DataRecord> {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    const url = `ws/rest/${this.model}/upload`;

    const { $upload: _upload, ...requestData } = data;

    for (const upload of uploads) {
      let fileToUpload = upload.file;
      if (upload.file.type === "message/rfc822") {
        fileToUpload = new File([upload.file], upload.file.name, {
          type: "application/octet-stream",
        });
      }
      formData.append("file", fileToUpload);
      formData.append("field", upload.field);
    }

    formData.append("request", JSON.stringify({ data: requestData }));

    return new Promise<DataRecord>(function (resolve, rejectPromise) {
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

      xhr.onerror = rejectPromise;
      xhr.onabort = rejectPromise;

      xhr.onload = function () {
        let responseData: unknown = {};
        try {
          responseData = JSON.parse(xhr.response || xhr.responseText);
        } catch {
          // ignore
        }

        const response = {
          data: responseData,
          status: xhr.status,
        };

        if (xhr.status === 200) {
          if (
            typeof responseData === "object" &&
            responseData !== null &&
            "status" in responseData &&
            responseData.status === 0 &&
            "data" in responseData &&
            Array.isArray(responseData.data)
          ) {
            resolve(responseData.data[0]);
          } else {
            rejectAsAlert(getUploadError(responseData)).catch(rejectPromise);
          }
        } else {
          rejectAsAlert(getUploadError(responseData) ?? response.status).catch(
            rejectPromise,
          );
        }
      };

      xhr.open("POST", url, true);
      xhr.setRequestHeader("X-CSRF-Token", readCookie("CSRF-TOKEN")!);
      xhr.send(formData);
    });
  }

  async perms(id?: number | null) {
    let url = `ws/rest/${this.model}/perms`;
    const params = new URLSearchParams();

    if ((id ?? 0) > 0) {
      params.append("id", String(id));
    }

    if (params.size > 0) {
      url += `?${params}`;
    }

    const resp = await request({ url, method: "GET" });

    if (resp.ok) {
      const { status, data } = await resp.json();

      if (status === 0) {
        if (data && Array.isArray(data)) {
          return data.reduce(
            (acc, curr) => {
              acc[curr?.toLowerCase?.()] = true;
              return acc;
            },
            {
              read: false,
              write: false,
              create: false,
              remove: false,
              export: false,
            },
          ) as Perms;
        }
      }
    }

    return Promise.reject(resp.status);
  }

  async isPermitted(action: AccessType, id?: number | null, silent?: boolean) {
    let url = `ws/rest/${this.model}/perms`;
    const params = new URLSearchParams({ action });

    if ((id ?? 0) > 0) {
      params.append("id", String(id));
    }

    if (params.size > 0) {
      url += `?${params}`;
    }

    const resp = await request({ url, method: "GET" });

    if (resp.ok) {
      const { status, errors } = await resp.json();

      if (status === 0) {
        return true;
      }

      if (!silent) {
        dialogs.box({
          title: i18n.get("Access Error"),
          content: Object.values(errors ?? {}).join("<br>"),
          yesNo: false,
        });
      }

      return false;
    }

    return Promise.reject(resp.status);
  }
}
