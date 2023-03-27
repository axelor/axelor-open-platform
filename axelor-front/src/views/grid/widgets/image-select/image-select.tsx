import { Box } from "@axelor/ui";
import { GridColumnProps } from "@axelor/ui/src/grid/grid-column";
import { Field } from "@/services/client/meta.types";
import { legacyClassNames } from "@/styles/legacy";

export function ImageSelect(props: GridColumnProps) {
  const { data, value } = props;
  const { selectionList, labels } = data as Field;

  const option = (selectionList || []).find(
    (item) => String(item.value) === String(value)
  );

  const image = option?.icon || option?.value;
  const text = option?.title;

  return (
    <>
      {image && image.includes("fa-") ? (
        <Box as={"i"} className={legacyClassNames("fa", image)} />
      ) : (
        <img
          style={labels === false ? { maxHeight: 18 } : { maxWidth: 18 }}
          src={image}
          alt={text}
        />
      )}
      {labels !== false && text}
    </>
  );
}
