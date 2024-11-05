import { useAtom } from "jotai";
import { useCallback, useMemo } from "react";

import { Rating as AxRating } from "@axelor/ui";

import { Field } from "@/services/client/meta.types";
import convert from "@/utils/convert";
import format from "@/utils/format";
import { FieldControl, FieldProps } from "@/views/form/builder";
import { useFormReady } from "../../builder/scope";

export function Rating(props: FieldProps<number>) {
  const { schema, readonly, valueAtom } = props;
  const { maxSize = 5, widgetAttrs, required } = schema;
  const {
    ratingIcon = "star",
    ratingColor,
    ratingFill = true,
    ratingHighlightSelected = false,
  } = widgetAttrs || {};

  const ready = useFormReady();
  const [value, setValue] = useAtom(valueAtom);

  const handleClick = useCallback(
    (position: number) => {
      if (readonly) return;
      if (position === value) {
        setValue(required ? null : convert(0, { props: schema }), true);
      } else {
        setValue(convert(position, { props: schema }), true);
      }
    },
    [readonly, value, setValue, required, schema],
  );

  const text = useMemo(
    () =>
      value != null ? format(value, { props: { ...schema } as Field }) : "",
    [schema, value],
  );

  return (
    <FieldControl {...props}>
      {ready && (
        <AxRating
          value={Number(value)}
          text={text}
          icon={ratingIcon}
          color={ratingColor}
          fill={ratingFill}
          highlightSelected={ratingHighlightSelected}
          readonly={!!readonly}
          max={maxSize}
          handleClick={handleClick}
        />
      )}
    </FieldControl>
  );
}
