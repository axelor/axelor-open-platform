import {
  Criteria,
  DataContext,
  DataRecord,
} from "@/services/client/data.types";
import { ActionView } from "@/services/client/meta.types";

export interface ActionHandler {
  getAttr(target: string, name: string): any;
  setAttr(target: string, name: string, value: any): any;

  setFocus(target: string): void;

  getValue(name: string): any;
  setValue(name: string, value: any): void;
  addValue(name: string, value: any): void;
  delValue(name: string, value: any): void;

  setValues(values: DataRecord): Promise<void>;

  showError(message: string): Promise<boolean>;
  showAlert(message: string): Promise<boolean>;
  showNotice(message: string): Promise<void>;

  save(): Promise<void>;
  save(record: DataRecord): Promise<void>;
  edit(record: DataRecord | null): Promise<void>;
  validate(): Promise<void>;

  refresh(target?: string): Promise<void>;

  getContext(): DataContext;

  open(action: ActionView): Promise<void>;
  close(): Promise<void>;

  download(url: string, fileName?: string): Promise<void>;

  onSignal(signal: string, data?: any): Promise<void>;
}

export type ActionOptions = {
  data?: Criteria & {
    _domain?: string;
    _domainContext?: DataContext;
    _archived?: boolean;
  };
  context?: DataContext;
};

export interface ActionExecutor {
  execute(action: string, options?: ActionOptions): Promise<void>;
}
