import {
  Criteria,
  DataContext,
  DataRecord,
} from "@/services/client/data.types";

export interface IActionData {
  type: string;
  target: string;
  value: any;
}

export interface ActionAttrData extends IActionData {
  type: "attr";
  name: string;
}

export interface ActionValueData extends IActionData {
  type: "value";
  op: "set" | "add" | "del";
}

export interface ActionFocusData extends IActionData {
  type: "focus";
}

export type ActionData = ActionAttrData | ActionValueData | ActionFocusData;

export type ActionListener = (data: ActionData) => void;

export interface ActionHandler {
  setAttr(target: string, name: string, value: any): any;
  setFocus(target: string): void;

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

  subscribe(subscriber: ActionListener): () => void;

  notify(data: ActionData): void;
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
