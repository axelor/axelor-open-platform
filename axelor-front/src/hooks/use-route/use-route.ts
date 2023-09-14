import { useCallback, useEffect, useMemo, useRef } from "react";
import {
  createSearchParams,
  generatePath,
  useLocation,
  useNavigate,
} from "react-router-dom";

export function useRoute() {
  const navigate = useNavigate();
  const location = useLocation();
  const refs = useRef({ location, navigate });

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
        refs.current.navigate({
          pathname,
          search,
        });
      }
    },
    []
  );

  useEffect(() => {
    refs.current = { location, navigate };
  }, [location, navigate]);

  return useMemo(() => ({ ...refs.current, redirect }), [redirect]);
}
