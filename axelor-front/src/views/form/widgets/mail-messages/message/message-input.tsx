import { ChangeEvent, useState } from "react";
import { Message, MessageFile, MessageInputProps } from "../message/types";
import { Box, Input, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { i18n } from "@/services/client/i18n";

export function MessageInput({
  focus = false,
  parent,
  onSave,
  onBlur,
}: MessageInputProps) {
  const [value, setValue] = useState("");
  const [files, setFiles] = useState<MessageFile[]>([]);
  const hasValue = Boolean(value);

  function resetState() {
    setValue("");
    setFiles([]);
  }

  function handleInputChange({
    target: { value },
  }: ChangeEvent<HTMLInputElement>) {
    setValue(value);
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
    // TODO: open DMS view
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
            <MaterialIcon icon="attach_file" weight={300} opticalSize={20} />
          </Button>
          <Button
            size="sm"
            ms={2}
            variant="primary"
            outline
            onClick={handleEdit}
            d="inline-flex"
          >
            <MaterialIcon icon="edit" weight={300} opticalSize={20} />
          </Button>
        </Box>
      )}
    </Box>
  );
}
