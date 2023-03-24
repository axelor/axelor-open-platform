import { DataRecord } from "@/services/client/data.types";
import { ActionView } from "@/services/client/meta.types";

import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";

import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { ActionHandler } from "./types";

export class DefaultActionHandler implements ActionHandler {
  getAttr(target: string, name: string) {}
  setAttr(target: string, name: string, value: any) {}

  setFocus(target: string) {}

  getValue(name: string) {}
  setValue(name: string, value: any) {}
  addValue(name: string, value: any) {}
  delValue(name: string, value: any) {}

  async setValues(values: DataRecord) {}

  async showError(message: string) {
    return dialogs.error({
      content: message,
    });
  }

  async showAlert(message: string) {
    return dialogs.confirm({
      content: message,
    });
  }

  async showNotice(message: string) {
    return alerts.info({
      message,
    });
  }

  getContext() {
    return {};
  }

  async save(): Promise<void>;
  async save(record: DataRecord): Promise<void>;
  async save(record?: DataRecord): Promise<void> {}
  async edit(record: DataRecord | null) {}

  async validate() {}

  async refresh(target?: string) {}

  async open(action: ActionView) {
    openTab(action);
  }

  async close() {}

  async download(url: string, fileName?: string) {}

  async onSignal(signal: string, data?: any) {}
}
