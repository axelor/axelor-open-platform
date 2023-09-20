import { uniqueId } from "lodash";
import { useCallback } from "react";
import { Button } from "@axelor/ui";

import { showPopup } from "@/view-containers/view-popup";
import { i18n } from "@/services/client/i18n";
import { initTab } from "@/hooks/use-tabs";
import { DataStore } from "@/services/client/data-store";
import { Property } from "@/services/client/meta.types";
import { TreeRecord } from "./types";
import { DataRecord } from "@/services/client/data.types";
import { usePopupHandlerAtom } from "@/view-containers/view-popup/handler";
import { useAtomValue } from "jotai";

export type DMSPopupOptions = {
  model?: string;
  record?: TreeRecord;
  fields?: Record<string, Property>;
  onSelect?: (dmsFiles: DataRecord[]) => void;
  onCountChanged?: (totalCount: number) => void;
};

const DMSModel = "com.axelor.dms.db.DMSFile";
const dmsDataStore = new DataStore(DMSModel);

async function getDMSFileFromRecord({
  record,
  model: relatedModel,
  fields,
}: DMSPopupOptions) {
  const relatedId = record!.id;
  function findName() {
    for (const name in fields) {
      if (fields[name].nameColumn) {
        return record![name];
      }
    }
    return record!.name || `00000${record!.id}`.slice(-5);
  }

  const {
    records: [dmsRecord],
  } = await dmsDataStore.search({
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

  return (
    dmsRecord || {
      id: -1,
      fileName: findName(),
      relatedId,
      relatedModel,
    }
  );
}

export function useDMSPopup() {
  return useCallback(async (options: DMSPopupOptions) => {
    const { record, model, onSelect, onCountChanged } = options;

    const popupRecord = record && (await getDMSFileFromRecord(options));

    const tab = await initTab({
      name: uniqueId("$dms"),
      title: i18n.get("Attachments"),
      model: DMSModel,
      viewType: "grid",
      views: [{ name: "dms-file-grid", type: "grid" }],
      params: {
        popup: true,
        "_popup-record": popupRecord,
        "ui-template:grid": "dms-file-list",
      },
      context: {
        _showRecord: record?.id,
      },
    });

    if (!tab) return;

    async function onClose() {
      if (onCountChanged && record?.id && model) {
        const { page } = await dmsDataStore.search({
          filter: {
            _domain:
              "self.relatedModel = :model AND self.relatedId = :id AND COALESCE(self.isDirectory, FALSE) = FALSE",
            _domainContext: {
              id: record.id,
              model,
            },
          },
          fields: ["fileName", "relatedModel", "relatedId"],
          limit: 1,
          offset: 0,
        });
        onCountChanged(page.totalCount ?? 0);
      }
    }

    await showPopup({
      tab,
      open: true,
      onClose,
      footer: (close) => <Footer onSelect={onSelect} onClose={close} />,
      buttons: [],
    });
  }, []);
}

function Footer({
  onSelect,
  onClose,
}: Pick<DMSPopupOptions, "onSelect"> & { onClose: (result: boolean) => void }) {
  return (
    <>
      {onSelect && (
        <FooterSelectButton
          onSelect={(list) => {
            onSelect(list);
            onClose(true);
          }}
        />
      )}
      <Button
        data-popup-close="true"
        variant="secondary"
        onClick={() => onClose(false)}
      >
        {i18n.get("Close")}
      </Button>
    </>
  );
}

function FooterSelectButton({ onSelect }: Pick<DMSPopupOptions, "onSelect">) {
  const handler = useAtomValue(usePopupHandlerAtom());
  return (
    <Button
      variant="primary"
      onClick={() => {
        onSelect?.(handler?.data?.selected ?? []);
      }}
    >
      {i18n.get("Select")}
    </Button>
  );
}
