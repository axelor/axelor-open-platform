import { Schema } from "@/services/client/meta.types";
import {
  META_FILE_MODEL,
  makeImageURL,
} from "@/views/form/widgets/image/utils";
import { GridCellProps } from "../../builder/types";

import styles from "./image.module.css";

export function Image(props: GridCellProps) {
  const { view, data, record } = props;
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
  return url && <img src={url} className={styles.image} />;
}
