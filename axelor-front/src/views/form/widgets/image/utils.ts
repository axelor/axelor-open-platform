import isNumber from "lodash/isNumber";

import { alerts } from "@/components/alerts";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { session } from "@/services/client/session";

const BLANK =
  "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

export const META_FILE_MODEL = "com.axelor.meta.db.MetaFile";

/**
 * Generates an image URL.
 *
 * @param {DataRecord} value - record
 * @param {string} model - target model
 * @param {string} [field] - target field name
 * @param {DataRecord} [parent] - parent with `id` and `_model`
 * @return {string} The generated image URL
 */
export function makeImageURL(
  value?: DataRecord | null,
  model?: string,
  field?: string,
  parent?: DataRecord | null,
): string {
  if (!value) return BLANK;

  const image = model !== META_FILE_MODEL;
  const id = value.id ?? value;
  if (!id || !isNumber(id) || id <= 0) return BLANK;

  const ver = value.version ?? value.$version ?? new Date().getTime();

  const url = `ws/rest/${model || parent?._model || META_FILE_MODEL}/${id}/${
    image ? field : "content"
  }/download?${image ? "image=true&" : ""}v=${ver}`;

  if (parent) {
    const { id: parentId, _model: parentModel } = parent;
    return `${url}${
      (parentId ?? 0) > 0 ? `&parentId=${parentId}` : ""
    }&parentModel=${parentModel}`;
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
        maxSize,
      ),
    });
    return false;
  }
  return true;
}
