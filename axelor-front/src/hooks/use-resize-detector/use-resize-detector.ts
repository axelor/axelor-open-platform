import { useEffect, useRef, useState } from "react";

export function useResizeDetector() {
  const ref = useRef<HTMLDivElement | null>(null);
  const [size, setSize] = useState<{ height: number; width: number }>();

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
            setSize({ height, width });
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

  const { height, width } = size ?? {
    height: ref.current?.offsetHeight,
    width: ref.current?.offsetWidth,
  };

  return { ref, height, width };
}
