import { escape } from "lodash";

import { TextLink } from "@/components/text-link";
import { GridColumnProps } from "@axelor/ui/grid";

export function Url(props: GridColumnProps) {
  const { value } = props;

  return <TextLink href={escape(value)}>{escape(value)}</TextLink>;
}
