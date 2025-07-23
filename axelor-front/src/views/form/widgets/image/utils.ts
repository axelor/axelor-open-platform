import isNumber from "lodash/isNumber";

import { DataRecord } from "@/services/client/data.types";

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
  binary?: boolean,
): string {
  if (!value) return BLANK;

  const image = model !== META_FILE_MODEL;
  const id = value.id ?? value;
  if (!id || !isNumber(id) || id <= 0) return BLANK;

  const ver = value.version ?? value.$version ?? new Date().getTime();

  const url = `ws/rest/${model || parent?._model || META_FILE_MODEL}/${id}/${
    image ? field : "content"
  }/download?${image && !binary ? "image=true&" : ""}v=${ver}`;

  if (parent) {
    const { id: parentId, _model: parentModel } = parent;
    return `${url}${
      (parentId ?? 0) > 0 ? `&parentId=${parentId}` : ""
    }&parentModel=${parentModel}`;
  }

  return url;
}
