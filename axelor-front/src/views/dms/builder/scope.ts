import { pick } from "lodash";

import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { readCookie } from "@/services/client/client";
import { TreeRecord } from "./types";
import { DataRecord } from "@/services/client/data.types";

export type UploaderListener = () => void;

export type UploadFile = {
  file: File;
  uuid?: null | string;
  _start?: number;
  _end?: number;
  _size?: number;
  loaded?: boolean;
  pending?: boolean;
  active?: boolean;
  failed?: boolean;
  complete?: boolean;
  progress?: number;
  transfer?: string;
  abort?: () => void;
  retry?: () => void;
};

function toJson(str: string) {
  let data = null;
  try {
    data = JSON.parse(str);
  } finally {
    return data;
  }
}

function formatSize(done: number, total: number) {
  function format(size: number) {
    if (size > 1000000000)
      return parseFloat(String(size / 1000000000)).toFixed(2) + " GB";
    if (size > 1000000)
      return parseFloat(String(size / 1000000)).toFixed(2) + " MB";
    if (size >= 1000) return parseFloat(String(size / 1000)).toFixed(2) + " KB";
    return size + " B";
  }
  return format(done || 0) + "/" + format(total);
}

export class Uploader {
  #items: UploadFile[] = [];
  #pending: UploadFile[] = [];
  #running = false;
  #saveHandler: ((data: DataRecord) => Promise<DataRecord>) | null = null;
  #listeners = new Set<UploaderListener>();

  get running() {
    return this.#running;
  }

  get items() {
    return this.#items;
  }

  setSaveHandler(handler: (data: DataRecord) => Promise<DataRecord>) {
    this.#saveHandler = handler;
  }

  subscribe(listener: UploaderListener) {
    this.#listeners.add(listener);
    return () => {
      this.#listeners.delete(listener);
    };
  }

  notify() {
    this.#listeners.forEach((fn) => fn());
  }

  queue(info: UploadFile) {
    info.pending = true;
    info.progress = 0;
    info.transfer = i18n.get("Pending");
    info.abort = () => {
      info.transfer = i18n.get("Cancelled");
      info.pending = false;
      this.notify();
    };
    info.retry = () => {
      this.queue(info);
      this.process();
      this.notify();
    };

    if (!this.#items.includes(info)) {
      this.#items.push(info);
    }
    if (!this.#pending.includes(info)) {
      this.#pending.push(info);
    }
    this.notify();
  }

  process() {
    if (this.#running || this.#pending.length === 0) {
      if (this.#items.every((item) => item.complete)) {
        this.#items.length = 0;
      }
      return this.notify();
    }

    this.#running = true;
    this.notify();

    let info = this.#pending.shift();

    while (info && !info.pending) {
      info = this.#pending.shift();
    }

    if (!info) {
      this.#running = false;
      this.notify();
      return;
    }

    const promise = this.upload(info);

    const error = (reason: any): any => {
      this.#running = false;
      if (info) {
        info.active = false;
        info.pending = false;
        info.progress = 0;
        info.transfer = reason.message;
        info.failed = true;
        this.notify();
      }
      return this.process();
    };

    const success = (): any => {
      this.#running = false;
      if (info) {
        info.active = false;
        info.pending = false;
        info.complete = true;
        info.progress = 100;
        this.notify();
      }
      return this.process();
    };

    return promise.then(success, error);
  }

  finish() {
    this.#running = false;
    this.#items.length = 0;
    this.#pending.length = 0;
    this.notify();
  }

  async upload(info: UploadFile) {
    const xhr = new XMLHttpRequest();
    const file = info.file;
    const onSave = this.#saveHandler;
    const notify = () => this.notify();

    return new Promise<any>((resolve, reject) => {
      function doClean() {
        return request({
          url: "ws/files/upload/" + info.uuid,
          method: "DELETE",
        });
      }

      function onError(reason?: any) {
        function done() {
          var message =
            reason && reason.error ? reason.error : i18n.get("Failed");
          reject({ message: message, failed: true });
        }
        doClean().then(done, done);
      }

      function onCancel(clean?: any) {
        function done() {
          reject({ message: i18n.get("Cancelled"), cancelled: true });
        }
        return clean ? doClean().then(done, done) : done();
      }

      function onSuccess(meta: TreeRecord) {
        const record = {
          fileName: meta.fileName,
          metaFile: pick(meta, "id"),
        };
        if (onSave) {
          return onSave(record).then(resolve, onError);
        }
        return resolve(record);
      }

      function onChunk(response?: DataRecord | null) {
        info._start = info._end;
        info._end = Math.min((info._end ?? 0) + (info._size ?? 0), file.size);

        if (response && response.fileId) {
          info.uuid = response.fileId;
        }

        notify();

        if (response && response.id) {
          return onSuccess(response);
        }

        if (info.loaded) {
          return onError();
        }

        // continue with next chunk
        sendChunk();
      }

      function sendChunk() {
        xhr.open("POST", "ws/files/upload", true);
        xhr.overrideMimeType("application/octet-stream");
        xhr.setRequestHeader("Content-Type", "application/octet-stream");
        xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        xhr.setRequestHeader("X-CSRF-Token", readCookie("CSRF-TOKEN")!);

        if (info.uuid) {
          xhr.setRequestHeader("X-File-Id", info.uuid);
        }

        xhr.setRequestHeader("X-File-Name", encodeURIComponent(file.name));
        xhr.setRequestHeader("X-File-Type", file.type);
        xhr.setRequestHeader("X-File-Size", String(file.size));
        xhr.setRequestHeader("X-File-Offset", String(info._start));

        if (info._end! > file.size) {
          info._end = file.size;
          notify();
        }

        const chunk = file.slice(info._start, info._end);

        xhr.send(chunk);
      }

      info.uuid = null;
      info._start = 0;
      info._size = 1000 * 1000; // 1MB
      info._end = info._size;

      info.active = true;
      info.transfer = formatSize(0, file.size);
      info.abort = function () {
        xhr.abort();
        onCancel();
        notify();
      };

      info.retry = () => {
        // put back on queue
        this.queue(info);
        this.process();
      };

      notify();
      xhr.upload.addEventListener("progress", function (e) {
        const total = (info._start ?? 0) + e.loaded;
        const done = Math.round((total / file.size) * 100);
        info.progress = done > 95 ? 95 : done;
        info.transfer = formatSize(total, file.size);
        info.loaded = total === file.size;
        notify();
      });

      xhr.onreadystatechange = function (e) {
        if (xhr.readyState === 4) {
          switch (xhr.status) {
            case 0:
            case 406:
              onCancel(true);
              break;
            case 200:
              onChunk(xhr.responseText ? toJson(xhr.responseText) : null);
              break;
            default:
              onError(xhr.responseText ? toJson(xhr.responseText) : null);
          }
        }
      };

      sendChunk();
    });
  }
}
