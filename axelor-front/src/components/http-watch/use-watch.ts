import { $request, $use } from "@/services/http";
import { useCallback, useEffect, useState } from "react";

type Pending = () => Promise<any>;

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
