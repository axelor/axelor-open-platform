import {
  META_FILE_MODEL,
  makeImageURL,
} from "@/views/form/widgets/image/utils";
import { Field } from "@/services/client/meta.types";
import { GridCellProps } from "../../builder/types";

import styles from "./image.module.css";

export function Image(props: GridCellProps) {
  const { view, data, record } = props;
  const value = record?.[data?.name];
  const isMetaFile = (data as Field).target === META_FILE_MODEL;
  const parent =
    isMetaFile && !record
      ? null
      : {
          ...record,
          _model: view?.model,
        };
  const url = makeImageURL(isMetaFile ? value : record, parent, data);
  return url && <img src={url} className={styles.image} />;
}
