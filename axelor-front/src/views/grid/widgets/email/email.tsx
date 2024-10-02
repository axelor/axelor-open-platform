import escape from "lodash/escape";

import { TextLink } from "@/components/text-link";
import { GridColumnProps } from "@axelor/ui/grid";

export function Email(props: GridColumnProps) {
  const { value } = props;
  return (
    <TextLink
      href={`mailto:${value}`}
      onClick={(e) => e.stopPropagation()}
    >
      {value}
    </TextLink>
  );
}
