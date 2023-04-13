import { DataContext } from "@/services/client/data.types";
import { RecordHandler, RecordListener } from "./types";

export class FormRecordHandler implements RecordHandler {
  #listeners = new Set<RecordListener>();
  #record: DataContext = {};

  subscribe(subscriber: RecordListener) {
    this.#listeners.add(subscriber);
    subscriber(this.#record)
    return () => {
      this.#listeners.delete(subscriber);
    };
  }

  notify(data: DataContext) {
    this.#record = data;
    this.#listeners.forEach((fn) => fn(data));
  }
}
