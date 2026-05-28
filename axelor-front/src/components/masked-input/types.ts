export const PLACEHOLDER = "_";

export type MaskArray = (string | RegExp)[];

export type MaskFunction = (
  rawValue: string,
  config?: MaskFunctionConfig,
) => MaskArray | false;

export type Mask = MaskArray | MaskFunction | false;

export interface MaskFunctionConfig {
  previousConformedValue?: string;
  currentCaretPosition?: number | null;
  placeholderChar?: string;
}

export interface ConformConfig {
  previousConformedValue?: string;
  guide?: boolean;
  placeholderChar?: string;
  placeholder?: string;
  currentCaretPosition?: number | null;
  keepCharPositions?: boolean;
}

export interface CaretPositionConfig {
  previousConformedValue: string;
  previousPlaceholder: string;
  currentCaretPosition: number;
  conformedValue: string;
  rawValue: string;
  placeholderChar: string;
  placeholder: string;
}
