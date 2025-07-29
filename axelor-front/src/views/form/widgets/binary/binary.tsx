import { atom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import { ChangeEvent, useCallback, useMemo, useRef } from "react";

import { Box, Button, ButtonGroup } from "@axelor/ui";

import { DataRecord } from "@/services/client/data.types";
import { download } from "@/utils/download";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { i18n } from "@/services/client/i18n";
import { FieldControl, FieldProps, FormAtom } from "../../builder";
import {
  META_FILE_MODEL,
  makeImageURL,
  validateFileSize,
} from "../image/utils";
import { useViewDirtyAtom } from "@/view-containers/views/scope";
import { formDirtyUpdater } from "../../builder/atoms";

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
      setDirty();
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
        <ButtonGroup border>
          {!readonly && (
            <Button
              title={i18n.get("Upload")}
              variant="light"
              d="flex"
              alignItems="center"
              onClick={handleUpload}
            >
              <MaterialIcon icon="upload" />
            </Button>
          )}
          {canDownload() && (
            <Button
              title={i18n.get("Download")}
              variant="light"
              d="flex"
              alignItems="center"
              onClick={handleDownload}
            >
              <MaterialIcon icon="download" />
            </Button>
          )}
          {!readonly && (
            <Button
              title={i18n.get("Remove")}
              variant="light"
              d="flex"
              alignItems="center"
              onClick={handleRemove}
            >
              <MaterialIcon icon="close" />
            </Button>
          )}
        </ButtonGroup>
      </Box>
    </FieldControl>
  );
}
