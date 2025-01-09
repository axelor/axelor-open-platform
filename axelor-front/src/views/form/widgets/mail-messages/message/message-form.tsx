import { PrimitiveAtom, atom, useAtom, useSetAtom } from "jotai";
import { useCallback, useEffect } from "react";

import { Box, Button, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";
import { Select, SelectOptionProps } from "@/components/select";
import { request } from "@/services/client/client";
import { i18n } from "@/services/client/i18n";
import { useDMSPopup } from "@/views/dms/builder/hooks";
import { SelectionTag } from "@/views/form/widgets";

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
      showRecipients = true,
    }: {
      title: string;
      record: Message;
      yesTitle?: string;
      noTitle?: string;
      onSave: (record: Message) => void;
      showRecipients?: boolean;
    }) => {
      let formData = { ...record };
      const formAtom = atom<Message>(formData);
      const isOk = await new Promise<boolean>((resolve) => {
        dialogs.modal({
          open: true,
          title,
          content: (
            <Form
              showRecipients={showRecipients}
              formAtom={formAtom}
              onFormChanged={(data) => {
                formData = data;
              }}
            />
          ),
          buttons: [],
          footer: ({ close }) => (
            <FormFooter
              formAtom={formAtom}
              yesTitle={yesTitle}
              noTitle={noTitle}
              onClose={async (result) => {
                if (
                  result &&
                  showRecipients &&
                  ((formData as any).recipients || []).length < 1
                ) {
                  alerts.warn({
                    message: i18n.get("Add recipients to post your message."),
                  });
                  return;
                }
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

function Form({
  formAtom,
  onFormChanged,
  showRecipients = true,
}: {
  formAtom: PrimitiveAtom<Message>;
  onFormChanged: (data: Message) => void;
  showRecipients?: boolean;
}) {
  const [formData, setFormValues] = useAtom(formAtom);
  const { subject = "", body = "", recipients = [], files = [] } = formData;

  const searchEmails = useCallback(
    async (term?: string): Promise<MessageRecipient[]> => {
      const resp = await request({
        url: "ws/search/emails",
        method: "POST",
        body: {
          data: {
            search: term,
            selected: (recipients || []).map(function (x) {
              return x.address;
            }),
          },
        },
      });
      if (resp.ok) {
        const { status, data } = await resp.json();
        return status === 0 ? data : [];
      }
      return [];
    },
    [recipients],
  );

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
    onChange(
      "files",
      files?.filter((f) => f !== file),
    );
  }

  const handleRemove = useCallback(
    (recipient: MessageRecipient) => {
      onChange(
        "recipients",
        recipients?.filter((r) => r.address !== recipient.address),
      );
    },
    [onChange, recipients],
  );

  const getLabel = useCallback(
    (option: MessageRecipient) => option.personal || option.address,
    [],
  );

  const renderValue = useCallback(
    ({ option }: SelectOptionProps<MessageRecipient>) => {
      return (
        <SelectionTag
          title={getLabel(option)}
          onRemove={() => handleRemove(option)}
        />
      );
    },
    [getLabel, handleRemove],
  );

  useEffect(() => {
    onFormChanged?.(formData);
  }, [formData, onFormChanged]);

  return (
    <Box flex={1} d="flex" flexDirection="column" p={2}>
      {showRecipients && (
        <Box flex={1} mb={2}>
          <Select
            value={recipients}
            invalid={(recipients || []).length === 0}
            multiple={true}
            onChange={(vals) => onChange("recipients", vals)}
            options={[] as MessageRecipient[]}
            optionKey={(x) => x.address}
            optionLabel={getLabel}
            optionEqual={(x, y) => x.address === y.address}
            placeholder={i18n.get("Recipients")}
            fetchOptions={searchEmails}
            renderValue={renderValue}
          />
        </Box>
      )}
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
