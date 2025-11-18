import { DragEvent, useCallback, useEffect, useRef, useState } from "react";

function isExtensionAccepted(name: string, acceptItem: string) {
  return name.toLowerCase().endsWith(acceptItem.toLowerCase());
}

function matchesExtensionWithType(
  type: string | undefined,
  acceptItem: string,
) {
  if (!type) return false;
  const subtype = type.split("/").pop();
  if (!subtype) return false;
  return `.${subtype}`.toLowerCase() === acceptItem.toLowerCase();
}

function isMimeAccepted(type: string, acceptItem: string) {
  if (acceptItem.endsWith("/*")) {
    const base = acceptItem.slice(0, acceptItem.length - 1);
    return type?.startsWith(base);
  }
  return type === acceptItem;
}

function isFileAccepted(
  file: { name?: string; type?: string },
  accept?: string,
) {
  if (!accept) return true;

  const acceptList = accept
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);

  if (acceptList.length === 0) return true;

  return acceptList.some((item) => {
    if (item.startsWith(".")) {
      if (file.name && isExtensionAccepted(file.name, item)) return true;
      return matchesExtensionWithType(file.type, item);
    }
    return !!file.type && isMimeAccepted(file.type, item);
  });
}

type UseFileDropOptions = {
  disabled?: boolean;
  accept?: string;
  onDropFile?: (file: File) => void | Promise<void>;
};

export function useFileDrop({
  disabled = false,
  accept,
  onDropFile,
}: UseFileDropOptions) {
  const dragCounterRef = useRef(0);
  const [isDragging, setIsDragging] = useState(false);

  useEffect(() => {
    if (disabled && dragCounterRef.current) {
      dragCounterRef.current = 0;
      setIsDragging(false);
    }
  }, [disabled]);

  const isFileDrag = useCallback(
    (event: DragEvent<HTMLElement>) => {
      if (!Array.from(event.dataTransfer?.types || []).includes("Files")) {
        return false;
      }
      const item = event.dataTransfer?.items?.[0];
      if (!item) return true;
      const file = item.getAsFile?.() ?? event.dataTransfer?.files?.[0];
      const type = file?.type || item.type;
      const name = file?.name;
      return isFileAccepted({ name, type }, accept);
    },
    [accept],
  );

  const handleDragOver = useCallback(
    (event: DragEvent<HTMLElement>) => {
      if (disabled || !isFileDrag(event)) return;

      event.preventDefault();
      event.dataTransfer.dropEffect = "copy";
    },
    [disabled, isFileDrag],
  );

  const handleDragEnter = useCallback(
    (event: DragEvent<HTMLElement>) => {
      if (disabled || !isFileDrag(event)) return;
      event.preventDefault();
      dragCounterRef.current += 1;
      setIsDragging(true);
    },
    [disabled, isFileDrag],
  );

  const handleDragLeave = useCallback(
    (event: DragEvent<HTMLElement>) => {
      if (disabled || !isFileDrag(event)) return;
      event.preventDefault();
      dragCounterRef.current = Math.max(0, dragCounterRef.current - 1);
      if (dragCounterRef.current === 0) {
        setIsDragging(false);
      }
    },
    [disabled, isFileDrag],
  );

  const handleDrop = useCallback(
    async (event: DragEvent<HTMLElement>) => {
      if (disabled || !isFileDrag(event)) return;

      event.preventDefault();
      dragCounterRef.current = 0;
      setIsDragging(false);
      const file = event.dataTransfer?.files?.[0];
      if (file && isFileAccepted(file, accept) && onDropFile) {
        await onDropFile(file);
      }
    },
    [accept, disabled, isFileDrag, onDropFile],
  );

  return {
    isDragging,
    dropZoneProps: {
      onDragEnter: handleDragEnter,
      onDragLeave: handleDragLeave,
      onDragOver: handleDragOver,
      onDrop: handleDrop,
    },
  };
}
