import { useRef, useMemo, ChangeEvent } from "react";
import { Box, Input } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { FieldContainer, FieldProps } from "../../builder";
import { useAtom, useAtomValue } from "jotai";
import { focusAtom } from "jotai-optics";
import { META_FILE_MODEL, makeImageURL, validateFileSize } from "./utils";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import classes from "./image.module.scss";

export function Image({
  schema,
  readonly,
  formAtom,
  valueAtom,
}: FieldProps<string | DataRecord | undefined | null>) {
  const { uid, title, type, serverType } = schema;
  const isBinary = (serverType || type || "").toLowerCase() === "binary";
  const inputRef = useRef<HTMLInputElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);
  const [value, setValue] = useAtom(valueAtom);

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

  const record = (isBinary ? parent : value) as DataRecord;

  function handleUpload() {
    const file = inputRef.current;
    file && file.click();
  }

  function handleRemove() {
    inputRef.current && (inputRef.current.value = "");
    setValue(null, true);
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

  return (
    <FieldContainer readonly={readonly}>
      <label htmlFor={uid}>{title}</label>
      <Box
        bgColor="body"
        border
        flexGrow={1}
        position="relative"
        maxW={100}
        maxH={100}
        className={classes.image}
      >
        <Box
          ref={imageRef}
          as="img"
          p={1}
          d="inline-block"
          src={
            isBinary && value === null
              ? makeImageURL(null)
              : isBinary && value
              ? (value as string)
              : makeImageURL(
                  record,
                  parent,
                  isBinary ? { ...schema, target: parentModel } : schema
                )
          }
          alt={title}
        />
        <form>
          <Input
            onChange={handleInputChange}
            type="file"
            accept="image/*"
            ref={inputRef}
            d="none"
          />
        </form>
        <Box
          className={classes.actions}
          bgColor="body"
          d={readonly ? "none" : "flex"}
          alignItems={"center"}
          justifyContent={"center"}
        >
          <MaterialIcon icon="upload" onClick={handleUpload} />
          <MaterialIcon icon="close" onClick={handleRemove} />
        </Box>
      </Box>
    </FieldContainer>
  );
}
