import { DataRecord } from "@/services/client/data.types";
import { ActionData, ActionHandler, ActionListener } from "./types";

export class DefaultActionHandler implements ActionHandler {
  #listeners = new Set<ActionListener>();

  setAttr(target: string, name: string, value: any) {}
  setFocus(target: string) {}

  setValue(name: string, value: any) {}
  addValue(name: string, value: any) {}
  delValue(name: string, value: any) {}

  async setValues(values: DataRecord) {}

  getContext() {
    return {};
  }

  async save(record?: DataRecord): Promise<void> {}
  async edit(record?: DataRecord | null) {}

  async validate() {}

  async refresh(target?: string) {}

  async close() {}

  async onSignal(signal: string, data?: any) {}

  subscribe(subscriber: ActionListener) {
    this.#listeners.add(subscriber);
    return () => {
      this.#listeners.delete(subscriber);
    };
  }

  notify(data: ActionData) {
    this.#listeners.forEach((fn) => fn(data));
  }
}
