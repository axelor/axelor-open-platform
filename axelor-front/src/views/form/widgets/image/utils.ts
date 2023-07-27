import isNumber from "lodash/isNumber";

import { session } from "@/services/client/session";
import { Schema } from "@/services/client/meta.types";
import { alerts } from "@/components/alerts";
import { i18n } from "@/services/client/i18n";

const BLANK =
  "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

export const META_FILE_MODEL = "com.axelor.meta.db.MetaFile";

export function makeImageURL(
  value: any,
  parent?: any,
  { name, target }: Schema = {}
): string {
  if (!value) return BLANK;

  const image = target !== META_FILE_MODEL;
  const id = value.id ?? value;
  if (!id || id <= 0 || !isNumber(id)) return BLANK;

  const ver = value.version ?? value.$version ?? new Date().getTime();

  const url = `ws/rest/${target || parent?._model || META_FILE_MODEL}/${id}/${
    image ? name : "content"
  }/download?${image ? "image=true&" : ""}v=${ver}`;

  if (parent) {
    return `${url}${
      +parent?.id > 0 ? `&parentId=${parent.id}` : ""
    }&parentModel=${parent?._model}`;
  }

  return url;
}

export function validateFileSize(file: File): boolean {
  if (!file) return false;
  const maxSize = session.info?.data?.upload?.maxSize ?? 0;
  const uploadMaxSize = 1048576 * maxSize;

  if (maxSize > 0 && file.size > uploadMaxSize) {
    alerts.info({
      message: i18n.get(
        "You are not allowed to upload a file bigger than {0} MB.",
        maxSize
      ),
    });
    return false;
  }
  return true;
}
