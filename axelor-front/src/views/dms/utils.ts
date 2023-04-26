import { request } from "@/services/client/client";
import { DataRecord } from "@/services/client/data.types";
import { download } from "@/utils/download";

export const toStrongText = (text: string, quote?: boolean) =>
  `<strong>${quote ? `<em>${text}</em>` : text}</strong>`;

export async function downloadAsBatch(
  record: DataRecord,
  model = "com.axelor.dms.db.DMSFile"
) {
  const resp = await request({
    url: `ws/dms/download/batch`,
    method: "POST",
    body: {
      model,
      records: [record.id],
    },
  });
  if (resp.ok) {
    const { batchId, batchName } = await resp.json();
    if (batchId || batchName) {
      return download(
        `ws/dms/download/${batchId}?fileName=${batchName}`,
        batchName
      );
    }
  }
}
