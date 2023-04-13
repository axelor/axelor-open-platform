import { useRef, useMemo, ChangeEvent } from "react";
import { Box, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { DataRecord } from "@/services/client/data.types";
import { download } from "@/utils/download";
import { DataStore } from "@/services/client/data-store";
import {
  META_FILE_MODEL,
  makeImageURL,
  validateFileSize,
} from "../image/utils";

export function BinaryLink({
  schema,
  readonly,
  formAtom,
  widgetAtom,
  valueAtom,
}: FieldProps<DataRecord | undefined | null>) {
  const { uid, name, accept, showTitle = true } = schema;
  const inputRef = useRef<HTMLInputElement>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { title },
  } = useAtomValue(widgetAtom);

  const parentId = useAtomValue(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop("id")),
      [formAtom]
    )
  );
  const parentVersion = useAtomValue(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop("version")),
      [formAtom]
    )
  );
  const parentModel = useAtomValue(
    useMemo(() => focusAtom(formAtom, (o) => o.prop("model")), [formAtom])
  );

  const parent = {
    id: parentId,
    version: parentVersion,
    _model: parentModel,
  } as DataRecord;

  function handleUpload() {
    const input = inputRef.current;
    input && input.click();
  }

  function handleDownload() {
    const fileURL = makeImageURL(value, parent, schema);
    download(fileURL, value?.fileName || name);
  }

  function handleRemove() {
    const input = inputRef.current;
    input && (input.value = "");
    setValue(null, true);
  }

  async function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e?.target?.files?.[0];

    inputRef.current && (inputRef.current.value = "");

    if (file && validateFileSize(file)) {
      const dataStore = new DataStore(META_FILE_MODEL);
      const metaFile = await dataStore.save({
        id: value?.id,
        version: value?.version ?? value?.$version,
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size,
        $upload: { file },
      });
      if (metaFile.id) {
        setValue(metaFile, true);
      }
    }
  }

  const text = value?.fileName;

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <Box d="flex" flexDirection="column">
        <Box>
          {text && (
            <Button
              variant="link"
              title={name}
              onClick={() => handleDownload()}
            >
              {text}
            </Button>
          )}
        </Box>
        {!readonly && (
          <Box d="flex" justifyContent="center">
            <form ref={formRef}>
              <Box
                as={"input"}
                onChange={handleInputChange}
                type="file"
                ref={inputRef}
                d="none"
                accept={accept}
              />
            </form>
            <Button variant="light" size="sm" d="flex" alignItems="center">
              <MaterialIcon icon="upload" onClick={handleUpload} />
            </Button>
            <Button variant="light" size="sm" d="flex" alignItems="center">
              <MaterialIcon icon="close" onClick={handleRemove} />
            </Button>
          </Box>
        )}
      </Box>
    </FieldContainer>
  );
}
