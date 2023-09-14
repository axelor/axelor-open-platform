import { WidgetProps } from "../../types";

export function ImageSelect({ field, record }: WidgetProps) {
  const { name, selectionList, labels } = field;
  const value = record[name];
  const selectValue = selectionList?.find(
    (item) => String(item.value) === String(value)
  );

  if (selectValue?.icon) {
    return (
      <>
        <img
          style={{ maxHeight: 24 }}
          src={selectValue.icon || selectValue.value}
          alt="select icon"
        />
        {labels !== false && <span>{selectValue.title}</span>}
      </>
    );
  }
  return value;
}
