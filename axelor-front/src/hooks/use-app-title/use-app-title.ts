import { useEffect } from "react";

import { useLoginInfo } from "@/components/login-form/login-info";

const NAME = "Axelor";
const DESC = "Axelor Entreprise Application";

export function useAppTitle() {
  const { data: info } = useLoginInfo();
  useEffect(() => {
    if (info) {
      const { name = NAME, description = DESC } = info.application ?? {};
      document.title = `${name} - ${description}`;
    }
  }, [info]);
}
