import { useEffect, useState } from "react";

export function useResizeDetector({
  keepLastSize = false,
}: {
  keepLastSize?: boolean;
} = {}) {
  const [ref, setRef] = useState<HTMLDivElement | null>(null);
  const [size, setSize] = useState<{ height?: number; width?: number }>({});

  useEffect(() => {
    const current = ref;
    if (current == null) return;

    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    const observer = new ResizeObserver((entries) => {
      if (cancelled) return;
      clearTimeout(timer);
      timer = setTimeout(() => {
        for (const entry of entries) {
          if (entry.target === current) {
            const { height, width } = entry.contentRect;
            setSize((prev) => {
              const nextHeight =
                keepLastSize && !height ? prev.height : height;
              const nextWidth = keepLastSize && !width ? prev.width : width;
              if (prev.height === nextHeight && prev.width === nextWidth) {
                return prev;
              }
              return { height: nextHeight, width: nextWidth };
            });
          }
        }
      }, 500);
    });

    observer.observe(current);

    return () => {
      cancelled = true;
      clearTimeout(timer);
      observer.unobserve(current);
      observer.disconnect();
    };
  }, [ref, keepLastSize]);

  const height = size.height ?? ref?.offsetHeight;
  const width = size.width ?? ref?.offsetWidth;

  return { ref: setRef, height, width };
}
