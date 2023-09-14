import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";
import {
  openTab_internal as openTab,
  useActiveTab_internal as useActiveTab,
} from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";
import { ActionView } from "@/services/client/meta.types";
import { uniqueId } from "lodash";

const openView: typeof openTab = (view, options) => {
  if (view && typeof view !== "string" && !view.name) {
    return openTab({ ...view, name: uniqueId("$act") }, options);
  }
  return openTab(view, options);
};

const $openHtmlTab = (url: string, title: string) => {
  return openView({
    title,
    viewType: "html",
    views: [{ type: "html", name: url }],
  } as ActionView);
};

export const axelor = {
  i18n,
  alerts,
  dialogs,
  openView,
  $openHtmlTab,
  useActiveTab,
};

// expose the module as a global variable
(window as any).axelor = axelor;
