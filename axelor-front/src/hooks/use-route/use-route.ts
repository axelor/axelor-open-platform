import { useCallback, useEffect, useRef } from "react";
import {
  createSearchParams,
  generatePath,
  useLocation,
} from "react-router-dom";

import { navigate } from "@/routes";

export function useRoute() {
  const location = useLocation();
  const refs = useRef({ location });

  const redirect = useCallback(
    (
      path: string,
      params?: Record<string, string | null>,
      query?: Record<string, string>
    ) => {
      let pathname = generatePath(path, params);
      let search = createSearchParams(query).toString();
      const current = refs.current.location;
      if (current.pathname !== pathname || current.search !== search) {
        navigate({
          pathname,
          search,
        });
      }
    },
    []
  );

  useEffect(() => {
    refs.current = { location };
  }, [location]);

  return {
    location,
    redirect,
  };
}
