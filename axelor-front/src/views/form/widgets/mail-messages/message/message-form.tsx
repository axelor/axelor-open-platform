import { useCallback, useEffect, useRef } from "react";
import { PrimitiveAtom, atom, useAtom, useSetAtom } from "jotai";
import { Box, Input, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";

import HtmlEditor from "../../html/editor";
import { i18n } from "@/services/client/i18n";
import { dialogs } from "@/components/dialogs";
import { TagSelectComponent } from "../../tag-select";
import { request } from "@/services/client/client";
import { useDMSPopup } from "@/views/dms/builder/hooks";
import { MessageFiles } from "./message-files";
import { Message, MessageFile } from "../message/types";

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
      const isOk = await new Promise<boolean>(async (resolve) => {
        function doClose(flag: boolean) {
          return dialogs.confirmDirty(
            async () => !flag && (formData as any)._dirty,
            async () => close(flag)
          );
        }
        const close = await dialogs.modal({
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
          footer: (
            <FormFooter
              formAtom={formAtom}
              yesTitle={yesTitle}
              noTitle={noTitle}
              onClose={doClose}
            />
          ),
          size: "lg",
          onClose: async (result) => {
            await close(result);
            resolve(result);
          },
        });
      });
      if (isOk) {
        onSave?.({ ...formData, _dirty: undefined } as Message);
      }
    },
    []
  );
}

async function searchEmails(term?: string) {
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
        } as Message)
    );
  }

  function handleFileRemove(file: MessageFile) {
    onChange(
      "files",
      files?.filter((f) => f !== file)
    );
  }

  useEffect(() => {
    onFormChanged?.(formData);
  }, [formData, onFormChanged]);

  return (
    <Box flex={1} d="flex" flexDirection="column" p={2}>
      <Box flex={1} mb={2}>
        <TagSelectComponent
          value={recipients}
          onChange={(vals) => onChange("recipients", vals)}
          schema={recipientField}
          optionValue="address"
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
          onChange={(e: string) => onChange("body", e)}
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
                      f.isDirectory !== true
                  ),
                ],
              } as Message)
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
