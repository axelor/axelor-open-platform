import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";
import {
  openTab_internal as openTab,
  useActiveTab_internal as useActiveTab,
} from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";
import { uniqueId } from "lodash";

const openView: typeof openTab = (view, options) => {
  if (view && typeof view !== "string" && !view.name) {
    return openTab({ ...view, name: uniqueId("$act") }, options);
  }
  return openTab(view, options);
};

export const axelor = {
  i18n,
  alerts,
  dialogs,
  openView,
  useActiveTab,
};

// expose the module as a global variable
(window as any).axelor = axelor;
