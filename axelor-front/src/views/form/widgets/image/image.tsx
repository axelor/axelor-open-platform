import { useAtom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom } from "jotai/utils";
import { ChangeEvent, useEffect, useMemo, useRef } from "react";

import { Box, Input, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { focusAtom } from "@/utils/atoms";

import { FieldControl, FieldProps } from "../../builder";
import { META_FILE_MODEL, makeImageURL, validateFileSize } from "./utils";

import styles from "./image.module.scss";

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

  async function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e?.target?.files?.[0];

    inputRef.current && (inputRef.current.value = "");

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
          setValue(metaFile, true);
        }
      }
    }
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
      <Box
        bgColor="body"
        border
        flexGrow={1}
        position="relative"
        maxW={100}
        maxH={100}
        className={clsx(styles.image, {
          [styles.editable]: schema.editable,
          [styles.invalid]: !isBinary && !readonly && invalid,
        })}
      >
        <Box
          ref={imageRef}
          as="img"
          p={schema.editable ? 0 : 1}
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
      </Box>
    </FieldControl>
  );
}
