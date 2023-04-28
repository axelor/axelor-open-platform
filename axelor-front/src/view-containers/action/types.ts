import {
  Criteria,
  DataContext,
  DataRecord,
} from "@/services/client/data.types";
import { ActionResult } from "@/services/client/meta";

export interface IActionData {
  type: string;
}

export interface ActionAttrData extends IActionData {
  type: "attr";
  target: string;
  name: string;
  value: any;
}

export interface ActionValueData extends IActionData {
  type: "value";
  target: string;
  value: any;
  op: "set" | "add" | "del";
}

export interface ActionFocusData extends IActionData {
  type: "focus";
  target: string;
}

export interface ActionCloseData extends IActionData {
  type: "close";
}

export type ActionData =
  | ActionAttrData
  | ActionValueData
  | ActionFocusData
  | ActionCloseData;

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
  execute(
    action: string,
    options?: ActionOptions
  ): Promise<ActionResult[] | void>;
}
