import pick from "lodash/pick";

import { readCookie, request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { TreeRecord } from "./types";
import { DataRecord } from "@/services/client/data.types";
import { DEFAULT_UPLOAD_CLEANUP_DELAY } from "@/utils/app-settings";

export type UploaderListener = () => void;

export type UploadFile = {
  file: File;
  uuid: string;
  _start?: number;
  _end?: number;
  _size?: number;
  status: UploadStatus;
  progress?: number;
  transfer?: string;
  abort?: () => void;
  retry?: () => void;
};

function toJson(str: string) {
  try {
    return JSON.parse(str);
  } catch {
    return null;
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

export enum UploadStatus {
  Pending = "pending",
  Uploading = "uploading",
  Uploaded = "uploaded",
  Failed = "failed",
  Cancelled = "cancelled",
  Completed = "completed",
}

function createUuid() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `u_${Math.random().toString(36).slice(2, 10)}${Date.now().toString(
    36,
  )}`;
}

export class Uploader {
  #items: UploadFile[] = [];
  #pending: UploadFile[] = [];
  #running = false;
  #saveHandler: ((data: DataRecord) => Promise<DataRecord>) | null = null;
  #cleanupTimer: ReturnType<typeof setTimeout> | null = null;
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

  queue(file: File) {
    this.#queue({
      file,
      uuid: createUuid(),
      status: UploadStatus.Pending,
    });
  }

  #queue(info: UploadFile) {
    this.#clearCleanupTimer();
    info.status = UploadStatus.Pending;
    info.progress = 0;
    info.transfer = i18n.get("Pending");
    info.abort = () => {
      info.transfer = i18n.get("Cancelled");
      info.status = UploadStatus.Cancelled;
      this.notify();
    };
    info.retry = () => {
      this.#queue(info);
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
      this.#scheduleCleanup();
      return this.notify();
    }

    this.#clearCleanupTimer();
    this.#running = true;
    this.notify();

    let info = this.#pending.shift();

    while (info && info.status !== UploadStatus.Pending) {
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
        info.progress = 0;
        info.transfer = reason.message;
        info.status = UploadStatus.Failed;
        this.notify();
      }
      return this.process();
    };

    const success = (): any => {
      this.#running = false;
      if (info) {
        info.status = UploadStatus.Completed;
        info.progress = 100;
        this.notify();
      }
      return this.process();
    };

    return promise.then(success, error);
  }

  finish() {
    this.#clearCleanupTimer();
    this.#running = false;
    this.#items.length = 0;
    this.#pending.length = 0;
    this.notify();
  }

  #scheduleCleanup(delay = DEFAULT_UPLOAD_CLEANUP_DELAY) {
    if (
      this.#cleanupTimer ||
      this.#running ||
      this.#pending.length > 0 ||
      this.#items.length === 0
    ) {
      return;
    }
    const hasInProgress = this.#items.some(
      (item) =>
        item.status === UploadStatus.Pending ||
        item.status === UploadStatus.Uploading ||
        item.status === UploadStatus.Uploaded,
    );
    const hasCompleted = this.#items.some(
      (item) => item.status === UploadStatus.Completed,
    );
    if (hasInProgress || !hasCompleted) {
      return;
    }
    this.#cleanupTimer = setTimeout(() => {
      this.#cleanupTimer = null;
      this.#cleanupCompletedItems();
    }, delay);
  }

  #clearCleanupTimer() {
    if (this.#cleanupTimer) {
      clearTimeout(this.#cleanupTimer);
      this.#cleanupTimer = null;
    }
  }

  #cleanupCompletedItems() {
    if (this.#running || this.#pending.length > 0) {
      return;
    }
    const incompleteItems = this.#items.filter(
      (item) => item.status !== UploadStatus.Completed,
    );
    if (incompleteItems.length === 0) {
      return this.finish();
    }

    const removed = this.#items.length !== incompleteItems.length;
    this.#items = incompleteItems;

    if (removed) {
      this.notify();
    }
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
          const message =
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

        notify();

        if (response && response.id) {
          return onSuccess(response);
        }

        if (info.status === UploadStatus.Uploaded) {
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

      info._start = 0;
      info._size = 1000 * 1000; // 1MB
      info._end = info._size;

      info.status = UploadStatus.Uploading;
      info.transfer = formatSize(0, file.size);
      info.abort = function () {
        xhr.abort();
        info.status = UploadStatus.Cancelled;
        onCancel();
        notify();
      };

      info.retry = () => {
        // put back on queue
        this.#queue(info);
        this.process();
      };

      notify();
      xhr.upload.addEventListener("progress", function (e) {
        const total = (info._start ?? 0) + e.loaded;
        const done = Math.round((total / file.size) * 100);
        info.progress = done > 95 ? 95 : done;
        info.transfer = formatSize(total, file.size);
        info.status =
          total === file.size ? UploadStatus.Uploaded : UploadStatus.Uploading;
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
