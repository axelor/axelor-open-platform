import { MouseEvent, useCallback, useRef } from "react";

export function useSingleClickHandler(
  handler: (e?: MouseEvent<HTMLButtonElement>) => void,
) {
  const clicking = useRef(false);

  return useCallback(
    async (e?: MouseEvent<HTMLButtonElement>) => {
      if (e?.detail === 2 || clicking.current) return;
      
      clicking.current = true;
      try {
        await handler(e);
      } finally {
        clicking.current = false;
      }
    },
    [handler],
  );
}
