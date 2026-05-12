import { useAtom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom, useAtomCallback } from "jotai/utils";
import {
  ChangeEvent,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";

import { Box, Input, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { FileDroppable } from "@/components/file-droppable";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { focusAtom } from "@/utils/atoms";
import { validateFileSize } from "@/utils/files";
import { useViewDirtyAtom } from "@/view-containers/views/scope";

import { FieldControl, FieldProps } from "../../builder";
import { formDirtyUpdater } from "../../builder/atoms";
import { useFormFieldSetter } from "../../builder/hooks";
import { META_FILE_MODEL, makeImageURL } from "./utils";

import styles from "./image.module.scss";

export function Image(
  props: FieldProps<string | DataRecord | undefined | null>,
) {
  const { schema, readonly, formAtom, widgetAtom, valueAtom, invalid } = props;
  const { type, serverType, accept = "image/*", $json } = schema;
  const isBinary = (serverType || type || "").toLowerCase() === "binary";
  const id = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);
  const [value, setValue] = useAtom(valueAtom);
  const {
    attrs: { title, required },
  } = useAtomValue(widgetAtom);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const setUpload = useFormFieldSetter(formAtom, "$upload");
  const dirtyAtom = useViewDirtyAtom();

  const setDirty = useAtomCallback(
    useCallback(
      (get, set) => {
        set(formAtom, formDirtyUpdater);
        set(dirtyAtom, true);
      },
      [formAtom, dirtyAtom],
    ),
  );

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
    file?.click();
  }

  function handleRemove() {
    if (inputRef.current) {
      inputRef.current.value = "";
    }
    setValue(null, true);
    if (isBinary) {
      setUpload(undefined);
      setPreviewUrl(null);
      if (required) {
        setValid(false);
      }
    }
  }

  const handleFileUpload = useCallback(
    async (file?: File) => {
      if (file && validateFileSize(file)) {
        if (isBinary) {
          setUpload({
            field: schema.name,
            file,
          });
          setPreviewUrl(URL.createObjectURL(file));
          setDirty();
          if (required) {
            setValid(true);
          }
        } else {
          const dataStore = new DataStore(META_FILE_MODEL);
          try {
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
          } catch {
            // Error is already handled upstream.
          }
        }
      }
    },
    [
      isBinary,
      record,
      required,
      schema.name,
      setDirty,
      setUpload,
      setValid,
      setValue,
    ],
  );

  async function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e?.target?.files?.[0];

    if (inputRef.current) {
      inputRef.current.value = "";
    }

    await handleFileUpload(file);
  }

  const { target, name } = isBinary
    ? { target: parentModel, name: schema.name }
    : schema;

  const isBinaryImage = isBinary && value !== null && !value;
  const url = previewUrl
    ? previewUrl
    : isBinary && value === null
      ? makeImageURL(null)
      : makeImageURL(record, target, name, parent);

  // Clean up object URL on unmount or when preview changes
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  // Reset preview on record change (save, prev/next navigation)
  useEffect(() => {
    setPreviewUrl(null);
  }, [parentId, parentVersion]);

  useEffect(() => {
    let ok = true;
    if (isBinaryImage && required && url && !url.startsWith("data:")) {
      const img = new window.Image();
      img.onload = function () {
        const exist = img.height > 1 && img.width > 1;
        if (ok) {
          setValid(exist);
        }
      };
      img.src = url;
    }
    return () => {
      ok = false;
    };
  }, [isBinaryImage, required, url, setValid]);

  return (
    <FieldControl {...props} inputId={id}>
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
          <Box className={styles.overlay} data-testid="input">
            <MaterialIcon icon="upload" aria-hidden="true" />
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
          data-testid="image"
        />
        <form>
          <Input
            id={id}
            onChange={handleInputChange}
            type="file"
            accept={accept}
            ref={inputRef}
            d="none"
            data-testid="input"
          />
        </form>
        <Box
          className={styles.actions}
          d={readonly ? "none" : "flex"}
          alignItems={"center"}
          justifyContent={"center"}
        >
          <MaterialIcon
            icon="upload"
            onClick={handleUpload}
            data-testid="upload-button"
          />
          <MaterialIcon
            icon="close"
            onClick={handleRemove}
            data-testid="remove-button"
          />
        </Box>
      </FileDroppable>
    </FieldControl>
  );
}
