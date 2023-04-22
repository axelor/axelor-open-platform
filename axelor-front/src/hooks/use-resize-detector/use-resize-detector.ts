import { useEffect, useRef, useState } from "react";

export function useResizeDetector() {
  const ref = useRef<HTMLDivElement | null>(null);
  const [height, setHeight] = useState<number>();
  const [width, setWidth] = useState<number>();

  useEffect(() => {
    const { current } = ref;
    if (current == null) return;

    let cancelled = false;
    let timer: NodeJS.Timeout;

    const observer = new ResizeObserver((entries) => {
      if (cancelled) return;
      clearTimeout(timer);
      timer = setTimeout(() => {
        for (const entry of entries) {
          if (entry.target === current) {
            const { height, width } = entry.contentRect;
            setHeight(height);
            setWidth(width);
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
  }, [ref]);

  return { ref, height, width };
}
