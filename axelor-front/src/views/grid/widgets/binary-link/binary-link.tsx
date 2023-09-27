import { Link } from "@axelor/ui";

import { Schema } from "@/services/client/meta.types";
import {
  META_FILE_MODEL,
  makeImageURL,
} from "@/views/form/widgets/image/utils";
import { GridCellProps } from "../../builder/types";

export function BinaryLink(props: GridCellProps) {
  const { value: cellValue, view, data, record } = props;
  const { target, name } = data as Schema;
  const isMetaFile = target === META_FILE_MODEL;
  const value = isMetaFile ? record?.[data?.name] : record;
  const parent =
    isMetaFile && !record
      ? null
      : {
          ...record,
          _model: view?.model,
        };
  const url = makeImageURL(value, target, name, parent);
  return url && <Link href={url}>{cellValue}</Link>;
}
