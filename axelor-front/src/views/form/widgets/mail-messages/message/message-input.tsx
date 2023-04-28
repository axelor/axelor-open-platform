import { ChangeEvent, useState } from "react";
import { Message, MessageFile, MessageInputProps } from "../message/types";
import { Box, Input, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { i18n } from "@/services/client/i18n";
import { useDMSPopup } from "@/views/dms/builder/hooks";
import { MessageFiles } from "./message-files";

export function MessageInput({
  focus = false,
  parent,
  onSave,
  onBlur,
}: MessageInputProps) {
  const [value, setValue] = useState("");
  const [files, setFiles] = useState<MessageFile[]>([]);
  const hasValue = Boolean(value);
  const showDMSPopup = useDMSPopup();

  function resetState() {
    setValue("");
    setFiles([]);
  }

  function handleInputChange({
    target: { value },
  }: ChangeEvent<HTMLInputElement>) {
    setValue(value);
  }

  function handleFileRemove(file: MessageFile) {
    setFiles((files) => files.filter((f) => f !== file));
  }

  function handlePost() {
    onSave &&
      onSave({
        ...(parent
          ? {
              relatedId: parent.relatedId,
              relatedModel: parent.relatedModel,
            }
          : {}),
        body: value,
        files: files.map((x) => x["metaFile.id"] || x.id),
        type: "comment",
      } as Message);
    resetState();
  }

  function handleAttachment() {
    showDMSPopup({
      onSelect: (dmsFiles) => {
        dmsFiles &&
          setFiles(
            dmsFiles.filter((f) => f.isDirectory !== true) as MessageFile[]
          );
      },
    });
  }

  function handleEdit() {
    // TODO: edit message, open dialog
  }

  return (
    <Box mb={3}>
      <Input
        name={"msgs"}
        value={value}
        onChange={handleInputChange}
        onBlur={() => onBlur && onBlur(value)}
        max={5}
        placeholder={i18n.get("Write your comment here")}
        autoFocus={focus}
      />
      {files && (
        <MessageFiles
          showIcon={false}
          stack
          data={files}
          onRemove={handleFileRemove}
        />
      )}
      {hasValue && (
        <Box mt={2}>
          <Button variant="primary" size="sm" onClick={handlePost}>
            {i18n.get("Post")}
          </Button>
          <Button
            size="sm"
            ms={2}
            variant="primary"
            outline
            onClick={handleAttachment}
            d="inline-flex"
          >
            <MaterialIcon icon="attach_file" fontSize={20} />
          </Button>
          <Button
            size="sm"
            ms={2}
            variant="primary"
            outline
            onClick={handleEdit}
            d="inline-flex"
          >
            <MaterialIcon icon="edit" fontSize={20} />
          </Button>
        </Box>
      )}
    </Box>
  );
}
