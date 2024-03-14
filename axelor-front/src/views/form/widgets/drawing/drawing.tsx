import { useAtom, useAtomValue, useSetAtom } from "jotai";
import { selectAtom } from "jotai/utils";
import { useCallback, useEffect, useMemo, useRef } from "react";
import SignaturePad from "signature_pad";

import { Box, clsx } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { DialogButton, DialogOptions, dialogs } from "@/components/dialogs";
import { DataStore } from "@/services/client/data-store";
import { DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { focusAtom } from "@/utils/atoms";

import { FieldControl, FieldProps } from "../../builder";
import { META_FILE_MODEL, makeImageURL } from "../image/utils";
import DrawingCanvas from "./drawing-canvas";

import styles from "./drawing.module.scss";

export function Drawing(
  props: FieldProps<string | DataRecord | undefined | null>,
) {
  const { schema, readonly, formAtom, widgetAtom, valueAtom, invalid } = props;
  const { type, widgetAttrs, serverType, $json } = schema;
  const isBinary = (serverType || type || "").toLowerCase() === "binary";
  const signaturePadRef = useRef<SignaturePad | null>(null);
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

  const getDialogOptions = useCallback(() => {
    const { drawingWidth = 500 } = widgetAttrs;
    const margins = 20;
    let size;
    let maximize;
    if (drawingWidth <= 300 - margins) {
      size = "sm";
    } else if (drawingWidth <= 500 - margins) {
      size = "md";
    } else if (drawingWidth <= 800 - margins) {
      size = "lg";
    } else if (drawingWidth <= 1140 - margins) {
      size = "xl";
    } else {
      maximize = true;
    }

    return { size, maximize } as Partial<DialogOptions>;
  }, [widgetAttrs]);

  const handleClick = useCallback(
    async (fn: (result: boolean) => void) => {
      const drawingUrl = signaturePadRef.current?.toDataURL() ?? null;

      if (drawingUrl) {
        if (isBinary) {
          setValue(drawingUrl, true);
          required && setValid(true);
        } else {
          // Converts drawingUrl into a new File
          const byteString = atob(drawingUrl.split(",")[1]);
          const ab = new ArrayBuffer(byteString.length);
          const ia = new Uint8Array(ab);
          for (let i = 0; i < byteString.length; i++) {
            ia[i] = byteString.charCodeAt(i);
          }
          const blob = new Blob([ab], { type: "image/png" });
          const file = new File([blob], "drawing.png");

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

      fn(false);
    },
    [
      isBinary,
      record?.$version,
      record?.id,
      record?.version,
      required,
      setValid,
      setValue,
    ],
  );

  function handleEdit() {
    const { size, maximize } = getDialogOptions();
    dialogs.modal({
      content: (
        <DrawingCanvas
          {...widgetAttrs}
          setSignaturePad={(value: SignaturePad) =>
            (signaturePadRef.current = value)
          }
          maximize={maximize}
        />
      ),
      onClose: () => {},
      title,
      size,
      maximize,
      buttons: [
        {
          name: "close",
          title: i18n.get("Close"),
          variant: "secondary",
          onClick: (fn) => {
            fn(false);
          },
        } as DialogButton,
        {
          name: "validate",
          title: i18n.get("Validate"),
          variant: "primary",
          onClick: handleClick,
        } as DialogButton,
      ],
      open: true,
    });
  }

  function handleRemove() {
    setValue(null, true);
    isBinary && required && setValid(false);
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
          [styles.inGridEditor]: schema.inGridEditor,
          [styles.invalid]: !isBinary && !readonly && invalid,
        })}
      >
        <Box
          as="img"
          p={schema.inGridEditor ? 0 : 1}
          d="inline-block"
          src={url}
          alt={title}
        />
        <Box
          className={styles.actions}
          d={readonly ? "none" : "flex"}
          alignItems="center"
          justifyContent="center"
        >
          <MaterialIcon icon="edit" onClick={handleEdit} />
          <MaterialIcon icon="close" onClick={handleRemove} />
        </Box>
      </Box>
    </FieldControl>
  );
}
