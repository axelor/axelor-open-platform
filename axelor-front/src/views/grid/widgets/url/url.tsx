import { useMemo } from "react";

import { GridColumnProps } from "@axelor/ui/grid";

import { TextLink } from "@/components/text-link";
import { isValidUrl } from "@/views/form/widgets/url/utils";

export function Url(props: GridColumnProps) {
  const { value } = props;
  const validUrl = useMemo(() => isValidUrl(value), [value]);

  return value ? (
    <TextLink href={validUrl ? value : undefined}>{value}</TextLink>
  ) : null;
}
