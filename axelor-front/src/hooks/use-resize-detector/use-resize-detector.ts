import { useEffect, useRef, useState } from "react";

export function useResizeDetector() {
  const ref = useRef<HTMLDivElement | null>(null);
  const [height, setHeight] = useState<number>();
  const [width, setWidth] = useState<number>();

  useEffect(() => {
    const { current } = ref;
    if (current == null) return;

    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === current) {
          const { height, width } = entry.contentRect;
          setHeight(height);
          setWidth(width);
        }
      }
    });

    observer.observe(current);

    return () => {
      observer.unobserve(current);
      observer.disconnect();
    };
  }, [ref]);

  return { ref, height, width };
}
