import { $request, $use } from "@/services/http";
import { useCallback, useEffect, useState } from "react";

type Pending = () => Promise<any>;

function isLogin(input: RequestInfo | URL) {
  const url = input instanceof Request ? input.url : input.toString();
  return url.startsWith(`./callback`) || url.startsWith("callback");
}

export function useHttpWatch() {
  const [count, setCount] = useState(0);
  const [pending, setPending] = useState<Pending[]>([]);

  const resume = useCallback(() => {
    const all = [...pending];
    setPending([]);
    all.forEach((fn) => fn());
  }, [pending]);

  useEffect(() => {
    return $use(async (args, next) => {
      // don't intercept login request
      if (isLogin(args.input)) return next();

      setCount((count) => count + 1);
      try {
        const res = await next();
        if (res instanceof Response && res.status === 401) {
          setCount((count) => count - 1);
          return new Promise((resolve, reject) => {
            setPending((pending) => [
              ...pending,
              () => $request(args.input, args.init).then(resolve, reject),
            ]);
          });
        }
        return res;
      } finally {
        setCount((count) => count - 1);
      }
    });
  }, []);

  return pending.length ? { count, resume } : { count };
}
