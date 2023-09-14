export interface Filter {
  color?: string;
  label?: string;
  value?: number | string;
  match?(obj: object): boolean;
  checked?: boolean;
}
