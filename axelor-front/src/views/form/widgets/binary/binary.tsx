import { useRef, useMemo, ChangeEvent } from "react";
import { Box, Button } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { FieldContainer, FieldProps, FormAtom } from "../../builder";
import { useAtomValue, useSetAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import {
  META_FILE_MODEL,
  makeImageURL,
  validateFileSize,
} from "../image/utils";
import { DataRecord } from "@/services/client/data.types";
import { download } from "@/utils/download";

function useFormFieldSetter(formAtom: FormAtom, fieldName: string) {
  return useSetAtom(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop(fieldName)),
      [formAtom, fieldName]
    )
  );
}

export function Binary({
  schema,
  readonly,
  widgetAtom,
  formAtom,
}: FieldProps<string | DataRecord | undefined | null>) {
  const { uid, name, accept, showTitle = true } = schema;
  const inputRef = useRef<HTMLInputElement>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const { attrs: { title } } = useAtomValue(widgetAtom);

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

  const setUpload = useFormFieldSetter(formAtom, "$upload");
  const setFileSize = useFormFieldSetter(formAtom, "fileSize");
  const setFileName = useFormFieldSetter(formAtom, "fileName");
  const setFileType = useFormFieldSetter(formAtom, "fileType");

  const record = {
    id: parentId,
    version: parentVersion,
    _model: parentModel,
  } as DataRecord;

  const isMetaModel = parentModel === META_FILE_MODEL;

  function canDownload() {
    if ((record?.id ?? -1) < 0) return false;
    if (isMetaModel) {
      return !!record.fileName;
    }
    return true;
  }

  function handleUpload() {
    const input = inputRef.current;
    input && input.click();
  }

  function handleDownload() {
    const imageURL = makeImageURL(record, record, schema);
    download(imageURL, record?.fileName || name);
  }

  function handleRemove() {
    const input = inputRef.current;
    input && (input.value = "");
    setUpload(null);
    setFileSize(null);
    setFileType(null);
    isMetaModel && setFileName(null);
  }

  async function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e?.target?.files?.[0];

    inputRef.current && (inputRef.current.value = "");

    if (file && validateFileSize(file)) {
      setUpload({
        field: name,
        file,
      });
      setFileSize(file.size);
      setFileType(file.type);
      isMetaModel && setFileName(file.name);
    }
  }

  return (
    <FieldContainer readonly={readonly}>
      {showTitle && <label htmlFor={uid}>{title}</label>}
      <Box d="flex">
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
        {!readonly && (
          <Button variant="light" size="sm" d="flex" alignItems="center">
            <MaterialIcon icon="upload" onClick={handleUpload} />
          </Button>
        )}
        {canDownload() && (
          <Button variant="light" size="sm" d="flex" alignItems="center">
            <MaterialIcon icon="download" onClick={handleDownload} />
          </Button>
        )}
        {!readonly && (
          <Button variant="light" size="sm" d="flex" alignItems="center">
            <MaterialIcon icon="close" onClick={handleRemove} />
          </Button>
        )}
      </Box>
    </FieldContainer>
  );
}
