import getValue from "lodash/get";

import { Box, Button, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import {
  ChangeEvent,
  KeyboardEvent,
  useCallback,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { Message, MessageFile, MessageInputProps } from "../message/types";

import { i18n } from "@/services/client/i18n";
import { useDMSPopup } from "@/views/dms/builder/hooks";
import { MessageFiles } from "./message-files";
import { useMessagePopup } from "./message-form";
import styles from "@/views/form/widgets/button/button.module.scss";

function TextareaAutoSizeInput(props: any) {
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const { value } = props;

  useLayoutEffect(() => {
    const input = inputRef.current;
    const getHeight = () => {
      if (!input || !value || !input?.scrollHeight) return "";
      const diff = input.offsetHeight - input.clientHeight;
      input.style.height = "auto";
      return `${input.scrollHeight + diff}px`;
    };
    input && (input.style.height = getHeight());
  }, [value]);

  return <Input ref={inputRef} as="textarea" rows={1} {...props} />;
}

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
  const showMessagePopup = useMessagePopup();

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

  const handleSave = useCallback(
    (data?: any) => {
      onSave &&
        onSave({
          type: "comment",
          ...(parent
            ? {
                relatedId: parent.relatedId,
                relatedModel: parent.relatedModel,
              }
            : {}),
          ...data,
          files: data?.files
            ?.map((x: MessageFile) => getValue(x, "metaFile.id"))
            .filter(Boolean),
        } as Message);
      resetState();
    },
    [onSave, parent],
  );

  const handlePost = useCallback(
    function () {
      handleSave({
        body: value,
        files,
      });
    },
    [files, handleSave, value],
  );

  function handleAttachment() {
    showDMSPopup({
      onSelect: (dmsFiles) => {
        dmsFiles &&
          setFiles((_files) => {
            const ids = _files.map((f) => f.id);
            return _files.concat(
              dmsFiles.filter(
                (f) => !ids.includes(f.id!) && f.isDirectory !== true,
              ) as MessageFile[],
            );
          });
      },
    });
  }

  async function handleEdit() {
    showMessagePopup({
      title: i18n.get("Email"),
      yesTitle: i18n.get("Send"),
      record: { body: value, files } as Message,
      onSave: handleSave,
    });
  }

  const handleKeyPress = useCallback(
    (e: KeyboardEvent) => {
      // Post message on Ctrl+Enter
      if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        handlePost();
      }
    },
    [handlePost],
  );

  return (
    <Box>
      <TextareaAutoSizeInput
        value={value}
        autoFocus={focus}
        placeholder={i18n.get("Write your comment here")}
        onChange={handleInputChange}
        onBlur={() => onBlur && onBlur(value)}
        onKeyPress={handleKeyPress}
      />
      {files && (
        <MessageFiles
          showIcon={false}
          stack
          data={files}
          onRemove={handleFileRemove}
        />
      )}

      <Box mt={2} d="flex">
        <Button
          variant="primary"
          size="sm"
          disabled={!hasValue}
          onClick={handlePost}
          {...(!focus && {
            onMouseDown: (e) => e.preventDefault(),
          })}
        >
          <div className={styles.title}>
            {i18n.get("Post")}
          </div>
        </Button>
        <Button
          size="sm"
          ms={2}
          variant="primary"
          outline
          onClick={handleAttachment}
        >
          <div className={styles.title}>
            <MaterialIcon icon="attach_file"/>
          </div>
        </Button>
        <Button
          size="sm"
          ms={2}
          variant="primary"
          outline
          onClick={handleEdit}
        >
          <div className={styles.title}>
            <MaterialIcon icon="edit"/>
          </div>
        </Button>
      </Box>
    </Box>
);
}
