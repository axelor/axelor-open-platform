import { DataRecord } from "@/services/client/data.types";
import { download } from "@/utils/download";
import { Box, Button, ButtonGroup } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { useAtomValue, useSetAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import { ChangeEvent, useMemo, useRef } from "react";
import { FieldControl, FieldProps, FormAtom } from "../../builder";
import {
  META_FILE_MODEL,
  makeImageURL,
  validateFileSize,
} from "../image/utils";

function useFormFieldSetter(formAtom: FormAtom, fieldName: string) {
  return useSetAtom(
    useMemo(
      () => focusAtom(formAtom, (o) => o.prop("record").prop(fieldName)),
      [formAtom, fieldName]
    )
  );
}

export function Binary(
  props: FieldProps<string | DataRecord | undefined | null>
) {
  const { schema, readonly, formAtom } = props;
  const { name, accept } = schema;
  const inputRef = useRef<HTMLInputElement>(null);
  const formRef = useRef<HTMLFormElement>(null);

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
    const { target, name } = schema;
    const imageURL = makeImageURL(record, target, name, record);
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
    <FieldControl {...props}>
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
        <ButtonGroup>
          {!readonly && (
            <Button variant="secondary" outline d="flex" alignItems="center">
              <MaterialIcon icon="upload" onClick={handleUpload} />
            </Button>
          )}
          {canDownload() && (
            <Button variant="secondary" outline d="flex" alignItems="center">
              <MaterialIcon icon="download" onClick={handleDownload} />
            </Button>
          )}
          {!readonly && (
            <Button variant="secondary" outline d="flex" alignItems="center">
              <MaterialIcon icon="close" onClick={handleRemove} />
            </Button>
          )}
        </ButtonGroup>
      </Box>
    </FieldControl>
  );
}
