import { PrimitiveAtom, atom, useAtom, useSetAtom } from "jotai";
import { useCallback, useEffect, useRef } from "react";

import { Box, Button, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { dialogs } from "@/components/dialogs";
import { Select } from "@/components/select";
import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { useDMSPopup } from "@/views/dms/builder/hooks";

import HtmlEditor from "../../html/editor";
import { Message, MessageFile, MessageRecipient } from "../message/types";
import { MessageFiles } from "./message-files";

export function useMessagePopup() {
  return useCallback(
    async ({
      title,
      record,
      yesTitle,
      noTitle,
      onSave,
    }: {
      title: string;
      record: Message;
      yesTitle?: string;
      noTitle?: string;
      onSave: (record: Message) => void;
    }) => {
      let formData = { ...record };
      const formAtom = atom<Message>(formData);
      const isOk = await new Promise<boolean>((resolve) => {
        dialogs.modal({
          open: true,
          title,
          content: (
            <Form
              formAtom={formAtom}
              onFormChanged={(data) => {
                formData = data;
              }}
            />
          ),
          buttons: [],
          footer: (close) => (
            <FormFooter
              formAtom={formAtom}
              yesTitle={yesTitle}
              noTitle={noTitle}
              onClose={async (result) => {
                if (
                  result ||
                  (await dialogs.confirmDirty(
                    async () => (formData as any)._dirty,
                    async () => close(result),
                  ))
                ) {
                  close(result);
                }
              }}
            />
          ),
          size: "lg",
          onClose: async (result) => {
            resolve(result);
          },
        });
      });
      if (isOk) {
        onSave?.({ ...formData, _dirty: undefined } as Message);
      }
    },
    [],
  );
}

async function searchEmails(term?: string): Promise<MessageRecipient[]> {
  const resp = await request({
    url: "ws/search/emails",
    method: "POST",
    body: {
      data: {
        search: term,
        selected: [],
      },
    },
  });
  if (resp.ok) {
    const { status, data } = await resp.json();
    return status === 0 ? data : [];
  }
  return [];
}

function Form({
  formAtom,
  onFormChanged,
}: {
  formAtom: PrimitiveAtom<Message>;
  onFormChanged: (data: Message) => void;
}) {
  const [formData, setFormValues] = useAtom(formAtom);
  const { subject = "", body = "", recipients = [], files = [] } = formData;

  const recipientField = useRef({
    targetName: "personal",
  }).current;

  function onChange(name: keyof Message, value: any) {
    setFormValues(
      (form) =>
        ({
          ...form,
          [name]: value,
          _dirty: true,
        }) as Message,
    );
  }

  function handleFileRemove(file: MessageFile) {
    onChange("files", files?.filter((f) => f !== file));
  }

  useEffect(() => {
    onFormChanged?.(formData);
  }, [formData, onFormChanged]);

  return (
    <Box flex={1} d="flex" flexDirection="column" p={2}>
      <Box flex={1} mb={2}>
        <Select
          value={recipients}
          multiple={true}
          onChange={(vals) => onChange("recipients", vals)}
          options={[] as MessageRecipient[]}
          optionKey={(x) => x.address}
          optionLabel={(x) => x.address}
          optionEqual={(x, y) => x.address === y.address}
          placeholder={i18n.get("Recipients")}
          fetchOptions={searchEmails}
        />
      </Box>
      <Box flex={1} mb={2}>
        <Input
          type="text"
          placeholder={i18n.get("Subject")}
          value={subject}
          onChange={(e) => onChange("subject", e.target.value)}
        />
      </Box>
      <Box flex={1} border rounded>
        <HtmlEditor
          value={body || ""}
          onChange={(
            e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
          ) => onChange("body", e.target.value)}
          {...({} as any)}
        />
      </Box>
      <Box d="flex" flex={1} my={1}>
        {files && (
          <MessageFiles
            showIcon={false}
            data={files}
            onRemove={handleFileRemove}
          />
        )}
      </Box>
    </Box>
  );
}

function FormFooter({
  yesTitle,
  noTitle,
  formAtom,
  onClose,
}: {
  yesTitle?: string;
  noTitle?: string;
  formAtom: PrimitiveAtom<Message>;
  onClose?: (isOk: boolean) => void;
}) {
  const showDMSPopup = useDMSPopup();
  const setFormValues = useSetAtom(formAtom);

  function handleAttachment() {
    showDMSPopup({
      onSelect: (dmsFiles) => {
        dmsFiles &&
          setFormValues(
            (form) =>
              ({
                _dirty: true,
                ...form,
                files: [
                  ...(form.files || []),
                  ...dmsFiles.filter(
                    (f) =>
                      !form.files?.find?.((_f) => _f.id === f.id) &&
                      f.isDirectory !== true,
                  ),
                ],
              }) as Message,
          );
      },
    });
  }

  return (
    <Box d="flex" flex={1}>
      <Box d="flex" flex={1}>
        <Box flex={1}>
          <Button
            outline
            variant="primary"
            size="sm"
            onClick={handleAttachment}
          >
            <Box d="flex" as="span">
              <MaterialIcon icon="attach_file" />
            </Box>
          </Button>
        </Box>
      </Box>
      <Box d="flex">
        <Button
          type="button"
          me={1}
          variant={"secondary"}
          onClick={() => onClose?.(false)}
        >
          {noTitle || i18n.get("Close")}
        </Button>
        <Button
          type="button"
          variant={"primary"}
          onClick={() => onClose?.(true)}
        >
          {yesTitle || i18n.get("Save")}
        </Button>
      </Box>
    </Box>
  );
}
