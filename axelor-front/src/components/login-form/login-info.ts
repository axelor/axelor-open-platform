import { useAsync } from "@/hooks/use-async";
import { request } from "@/services/client/client";
import { LoadingCache } from "@/utils/cache";

export interface ApplicationInfo {
  name?: string;
  description?: string;
  logo?: string;
  copyright?: string;
  language?: string;
  callbackUrl?: string;
}

export interface ClientInfo {
  name: string;
  icon?: string;
  title?: string;
}

export interface PublicInfo {
  application: ApplicationInfo;
  clients?: ClientInfo[];
  defaultClient: string;
  exclusive?: string;
}

const cache = new LoadingCache<Promise<PublicInfo>>();

export function useLoginInfo() {
  return useAsync<PublicInfo>(async () => {
    return await cache.get("info", async () => {
      const url = "ws/public/app/info";
      const response = await request({ url });
      const info = await response.json();
      return info;
    });
  }, []);
}
