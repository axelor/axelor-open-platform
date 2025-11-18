import { useAtom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom } from "jotai/utils";
import { ChangeEvent, useCallback, useEffect, useMemo, useRef } from "react";

import { Box, Input, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { focusAtom } from "@/utils/atoms";
import { validateFileSize } from "@/utils/files";

import { FieldControl, FieldProps } from "../../builder";
import { META_FILE_MODEL, makeImageURL } from "./utils";

import styles from "./image.module.scss";
import { FileDroppable } from "@/components/file-droppable";

export function Image(
  props: FieldProps<string | DataRecord | undefined | null>,
) {
  const { schema, readonly, formAtom, widgetAtom, valueAtom, invalid } = props;
  const { type, serverType, accept = "image/*", $json } = schema;
  const isBinary = (serverType || type || "").toLowerCase() === "binary";
  const inputRef = useRef<HTMLInputElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { title, required },
  } = useAtomValue(widgetAtom);

  const setValid = useSetAtom(
    useMemo(
      () =>
        focusAtom(
          widgetAtom,
          (state) => state.valid ?? false,
          (state, valid) => ({ ...state, valid }),
        ),
      [widgetAtom],
    ),
  );

  const recordAtom = useMemo(
    () =>
      selectAtom(formAtom, (x) => {
        const { record } = x;
        return $json ? record?.$record : record;
      }),
    [formAtom, $json],
  );

  const parentId = useAtomValue(
    useMemo(() => selectAtom(recordAtom, (x) => x.id), [recordAtom]),
  );

  const parentVersion = useAtomValue(
    useMemo(() => selectAtom(recordAtom, (x) => x.version), [recordAtom]),
  );

  const parentModel = useAtomValue(
    useMemo(() => selectAtom(formAtom, (x) => x.model), [formAtom]),
  );

  const parent = {
    id: parentId,
    version: parentVersion,
    _model: parentModel,
  } as DataRecord;

  const record = (isBinary ? parent : value) as DataRecord;

  function handleUpload() {
    const file = inputRef.current;
    file && file.click();
  }

  function handleRemove() {
    inputRef.current && (inputRef.current.value = "");
    setValue(null, true);
    isBinary && required && setValid(false);
  }

  const handleFileUpload = useCallback(
    async (file?: File) => {
      if (file && validateFileSize(file)) {
        if (isBinary) {
          const reader = new FileReader();
          reader.onload = (e) => {
            const value = e.target?.result ?? null;
            setValue(value, true);
            required && setValid(true);
          };
          reader.readAsDataURL(file);
        } else {
          const dataStore = new DataStore(META_FILE_MODEL);
          const metaFile = await dataStore.save({
            id: record?.id,
            version: record?.version ?? record?.$version,
            fileName: file.name,
            fileType: file.type,
            fileSize: file.size,
            $upload: { file },
          });
          if (metaFile.id) {
            setValue(metaFile, true, record?.id == null);
          }
        }
      }
    },
    [isBinary, record, required, setValid, setValue],
  );

  async function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e?.target?.files?.[0];

    inputRef.current && (inputRef.current.value = "");

    await handleFileUpload(file);
  }

  const { target, name } = isBinary
    ? { target: parentModel, name: schema.name }
    : schema;

  const isBinaryImage = isBinary && value !== null && !value;
  const url =
    isBinary && value === null
      ? makeImageURL(null)
      : isBinary && value
        ? (value as string)
        : makeImageURL(record, target, name, parent);

  useEffect(() => {
    let ok = true;
    if (isBinaryImage && required && url && !url.startsWith("data:")) {
      const img = new window.Image();
      img.onload = function () {
        const exist = img.height > 1 && img.width > 1;
        ok && setValid(exist);
      };
      img.src = url;
    }
    return () => {
      ok = false;
    };
  }, [isBinaryImage, required, url, setValid]);

  return (
    <FieldControl {...props}>
      <FileDroppable
        bgColor="body"
        border
        flexGrow={1}
        position="relative"
        maxW={100}
        maxH={100}
        d="block"
        className={clsx(styles.image, {
          [styles.inGridEditor]: schema.inGridEditor,
          [styles.invalid]: !isBinary && !readonly && invalid,
        })}
        accept={accept}
        disabled={readonly}
        onDropFile={handleFileUpload}
        renderDragOverlay={({ children }) => (
          <Box className={styles.overlay}>
            <MaterialIcon icon="upload" />
            <span>{children}</span>
          </Box>
        )}
      >
        <Box
          ref={imageRef}
          as="img"
          p={schema.inGridEditor ? 0 : 1}
          d="inline-block"
          src={url}
          alt={title}
        />
        <form>
          <Input
            onChange={handleInputChange}
            type="file"
            accept={accept}
            ref={inputRef}
            d="none"
          />
        </form>
        <Box
          className={styles.actions}
          d={readonly ? "none" : "flex"}
          alignItems={"center"}
          justifyContent={"center"}
        >
          <MaterialIcon icon="upload" onClick={handleUpload} />
          <MaterialIcon icon="close" onClick={handleRemove} />
        </Box>
      </FileDroppable>
    </FieldControl>
  );
}
