import {
  Criteria,
  DataContext,
  DataRecord,
} from "@/services/client/data.types";

export interface ActionHandler {
  getAttr(target: string, name: string): any;
  setAttr(target: string, name: string, value: any): any;

  setFocus(target: string): void;

  getValue(name: string): any;
  setValue(name: string, value: any): void;
  addValue(name: string, value: any): void;
  delValue(name: string, value: any): void;

  setValues(values: DataRecord): Promise<void>;

  save(record?: DataRecord): Promise<void>;
  edit(record?: DataRecord | null): Promise<void>;

  validate(): Promise<void>;

  refresh(target?: string): Promise<void>;

  close(): Promise<void>;

  getContext(): DataContext;

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
