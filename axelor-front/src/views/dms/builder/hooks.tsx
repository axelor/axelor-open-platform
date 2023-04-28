import { uniqueId } from "lodash";
import { useCallback } from "react";
import { Button } from "@axelor/ui";

import { showPopup } from "@/view-containers/view-popup";
import { i18n } from "@/services/client/i18n";
import { initTab } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { Property } from "@/services/client/meta.types";
import { TreeRecord } from "./types";

export type DMSPopupOptions = {
  model: string;
  record: TreeRecord;
  fields?: Record<string, Property>;
  onCountChanged?: (totalCount: number) => void;
};

export function useDMSPopup() {
  const model = "com.axelor.dms.db.DMSFile";

  return useCallback(async (options: DMSPopupOptions) => {
    const {
      record,
      model: relatedModel,
      fields,
      onCountChanged,
    } = options;
    const relatedId = record.id;
    const ds = new DataStore(model);

    function findName() {
      for (const name in fields) {
        if (fields[name].nameColumn) {
          return record[name];
        }
      }
      return record.name || `00000${record.id}`.slice(-5);
    }

    const {
      records: [dmsRecord],
    } = await ds.search({
      filter: {
        _domain:
          "self.isDirectory = true AND self.relatedId = :id AND self.relatedModel = :model AND self.parent.relatedModel = :model AND (self.parent.relatedId is null OR self.parent.relatedId = 0)",
        _domainContext: {
          id: relatedId,
          model: relatedModel,
        },
      },
      fields: ["fileName", "relatedModel", "relatedId"],
      limit: 1,
      offset: 0,
    });

    const tab = await initTab({
      name: uniqueId("$dms"),
      title: i18n.get("Attachments"),
      model,
      viewType: "grid",
      views: [{ name: "dms-file-grid", type: "grid" }],
      params: {
        popup: true,
        "_popup-record": dmsRecord || {
          id: -1,
          fileName: findName(),
          relatedId,
          relatedModel,
        },
        "ui-template:grid": "dms-file-list",
      },
      context: {
        _showRecord: record?.id,
      },
    });

    if (!tab) return;

    async function onClose() {
      const { page } = await ds.search({
        filter: {
          _domain:
            "self.relatedModel = :model AND self.relatedId = :id AND COALESCE(self.isDirectory, FALSE) = FALSE",
          _domainContext: {
            id: relatedId,
            model: relatedModel,
          },
        },
        fields: ["fileName", "relatedModel", "relatedId"],
        limit: 1,
        offset: 0,
      });
      onCountChanged?.(page.totalCount ?? 0);
    }
    const close = await showPopup({
      tab,
      open: true,
      onClose,
      footer: () => (
        <Footer
          onClose={() => {
            close();
            onClose();
          }}
        />
      ),
      buttons: [],
    });
  }, []);
}

function Footer({ onClose }: { onClose: () => void }) {
  return (
    <Button variant="secondary" onClick={onClose}>
      {i18n.get("Close")}
    </Button>
  );
}
