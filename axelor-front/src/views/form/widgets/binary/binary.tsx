import { atom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { ChangeEvent, useCallback, useId, useMemo, useRef } from "react";

import { Box, Button, ButtonGroup } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { FileDroppable } from "@/components/file-droppable";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { download } from "@/utils/download";
import { validateFileSize } from "@/utils/files";
import { useViewDirtyAtom } from "@/view-containers/views/scope";

import { FieldControl, FieldProps, FormAtom } from "../../builder";
import { formDirtyUpdater } from "../../builder/atoms";
import { META_FILE_MODEL, makeImageURL } from "../image/utils";

function useFormFieldSetter(formAtom: FormAtom, fieldName: string) {
  return useSetAtom(
    useMemo(
      () =>
        atom(null, (get, set, value: any) => {
          set(formAtom, ({ record, ...rest }) => ({
            ...rest,
            record: { ...record, [fieldName]: value },
          }));
        }),
      [formAtom, fieldName],
    ),
  );
}

export function Binary(
  props: FieldProps<string | DataRecord | undefined | null>,
) {
  const { schema, readonly, valueAtom, formAtom } = props;
  const { name, accept } = schema;
  const inputRef = useRef<HTMLInputElement>(null);
  const formRef = useRef<HTMLFormElement>(null);

  const id = useId();

  const setValue = useSetAtom(valueAtom);
  const dirtyAtom = useViewDirtyAtom();
  const record = useAtomValue(
    useMemo(
      () =>
        selectAtom(formAtom, (o) => ({
          id: o.record.id,
          version: o.record.version,
          fileName: o.record.fileName,
          _model: o.model,
        })),
      [formAtom],
    ),
  );

  const setUpload = useFormFieldSetter(formAtom, "$upload");
  const setFileSize = useFormFieldSetter(formAtom, "fileSize");
  const setFileName = useFormFieldSetter(formAtom, "fileName");
  const setFileType = useFormFieldSetter(formAtom, "fileType");

  const isMetaModel = record._model === META_FILE_MODEL;

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
    const imageURL = makeImageURL(record, target, name, record, true);
    download(imageURL, record?.fileName || name);
  }

  function handleRemove() {
    const input = inputRef.current;
    input && (input.value = "");
    setUpload(undefined);
    setFileSize(undefined);
    setFileType(undefined);
    isMetaModel && setFileName(undefined);
    setValue(null);
  }

  const setDirty = useAtomCallback(
    useCallback(
      (get, set) => {
        set(formAtom, formDirtyUpdater);
        set(dirtyAtom, true);
      },
      [formAtom, dirtyAtom],
    ),
  );

  const handleFileUpload = useCallback(
    (file?: File) => {
      if (file && validateFileSize(file)) {
        setUpload({
          field: name,
          file,
        });
        setFileSize(file.size);
        setFileType(file.type);
        isMetaModel && setFileName(file.name);
        setDirty();
      }
    },
    [
      isMetaModel,
      name,
      setDirty,
      setFileName,
      setFileSize,
      setFileType,
      setUpload,
    ],
  );

  function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e?.target?.files?.[0];

    inputRef.current && (inputRef.current.value = "");

    handleFileUpload(file);
  }

  return (
    <FieldControl {...props} inputId={id}>
      <Box d="flex">
        <form ref={formRef}>
          <Box
            as="input"
            id={id}
            onChange={handleInputChange}
            type="file"
            ref={inputRef}
            d="none"
            accept={accept}
            data-testid="input"
          />
        </form>
        <FileDroppable
          accept={accept}
          disabled={readonly}
          onDropFile={handleFileUpload}
        >
          <ButtonGroup border>
            {!readonly && (
              <Button
                title={i18n.get("Upload")}
                variant="light"
                d="flex"
                alignItems="center"
                onClick={handleUpload}
                data-testid="btn-upload"
              >
                <MaterialIcon icon="upload" aria-hidden="true" />
              </Button>
            )}
            {canDownload() && (
              <Button
                title={i18n.get("Download")}
                variant="light"
                d="flex"
                alignItems="center"
                onClick={handleDownload}
                data-testid="btn-download"
              >
                <MaterialIcon icon="download" aria-hidden="true" />
              </Button>
            )}
            {!readonly && (
              <Button
                title={i18n.get("Remove")}
                variant="light"
                d="flex"
                alignItems="center"
                onClick={handleRemove}
                data-testid="btn-remove"
              >
                <MaterialIcon icon="close" aria-hidden="true" />
              </Button>
            )}
          </ButtonGroup>
        </FileDroppable>
      </Box>
    </FieldControl>
  );
}
