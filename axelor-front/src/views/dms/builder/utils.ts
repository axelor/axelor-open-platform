import { request } from "@/services/client/client";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { ActionView, GridView } from "@/services/client/meta.types";
import { download } from "@/utils/download";

export const CONTENT_TYPE = {
  HTML: "html",
  SPREADSHEET: "spreadsheet",
} as const;

export const toStrongText = (text: string, quote?: boolean) =>
  `<strong>${quote ? `<em>${text}</em>` : text}</strong>`;

export function prepareCustomView({ model }: GridView, record: DataRecord) {
  const isSpreadsheet = record.contentType === CONTENT_TYPE.SPREADSHEET;
  const view = isSpreadsheet
    ? {
        name: `spreadsheet?id=${record.id}`,
        type: "html",
      }
    : {
        name: `dms-html-form`,
        type: "form",
        width: "large",
        items: [
          {
            type: "panel",
            items: [
              {
                type: "panel",
                items: [
                  {
                    type: "button",
                    title: i18n.get("Save"),
                    icon: "fa-save",
                    onClick: "save",
                    colSpan: "3",
                  },
                  {
                    type: "button",
                    title: i18n.get("Download"),
                    icon: "fa-download",
                    onClick: "save,action-dms-file-download",
                    colSpan: "3",
                  },
                ],
              },
              {
                type: "field",
                name: "content",
                showTitle: false,
                widget: record.contentType || "html",
                colSpan: 12,
                height: 520,
              },
            ],
          } as any,
        ],
      };
  return {
    name: `$act:dms${record.id}`,
    model,
    title: isSpreadsheet ? i18n.get("Spreadsheet") : i18n.get("Document"),
    viewType: view.type,
    views: [view],
    params: {
      "show-toolbar": false,
      forceEdit: true,
    },
    context: {
      _showRecord: record?.id,
    },
  } as ActionView;
}

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
