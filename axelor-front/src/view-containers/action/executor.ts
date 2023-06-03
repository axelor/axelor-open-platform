import { uniqueId } from "lodash";

import { openTab_internal as openTab } from "@/hooks/use-tabs";
import { i18n } from "@/services/client/i18n";
import { ActionResult, action as executeAction } from "@/services/client/meta";
import { ActionView, HtmlView, View } from "@/services/client/meta.types";
import { download } from "@/utils/download";

import { alerts } from "@/components/alerts";
import { dialogs } from "@/components/dialogs";

import { TaskQueue } from "./queue";
import { ActionExecutor, ActionHandler, ActionOptions } from "./types";

const queue = new TaskQueue();

export class DefaultActionExecutor implements ActionExecutor {
  #handler;

  constructor(handler: ActionHandler) {
    this.#handler = handler;
  }

  #ensureLast(actions: string[], name: string) {
    const index = actions.indexOf(name);
    const length = actions.length;

    if (index > -1 && index !== length - 1) {
      throw new Error(
        i18n.get('Invalid use of "{0}" action, must be the last action.', name)
      );
    }

    return index > -1;
  }

  #enqueue(action: string, options?: ActionOptions) {
    return queue.add(() => this.#execute(action, options));
  }

  async waitFor(interval = 50) {
    await new Promise<void>((resolve) => {
      setTimeout(() => resolve(), interval);
    });
  }

  async wait() {
    return queue.wait();
  }

  async execute(action: string, options?: ActionOptions) {
    try {
      return this.#enqueue(action, options);
    } catch (e) {
      if (typeof e === "string") {
        dialogs.error({
          content: e,
        });
      }
      return Promise.reject(e);
    }
  }

  async #execute(action: string, options?: ActionOptions) {
    const actions = action
      .split(",")
      .map((x) => x.trim())
      .filter((x) => {
        // we may not need `sync` now
        if (x === "sync") {
          console.warn(
            `We don't need "sync" action now, remove it from: ${action}`
          );
          return false;
        }
        return true;
      })
      .filter(Boolean);

    if (actions.length === 0) {
      return;
    }

    // `new` and `close` must be the last action
    this.#ensureLast(actions, "new");
    this.#ensureLast(actions, "close");

    // re-join to remove white spaces<
    action = actions.join(",");

    if (action === "close") {
      return this.#handler.close();
    }

    if (action === "new") {
      return this.#handler.edit(null);
    }

    if (action === "validate") {
      return this.#handler.validate();
    }

    if (action === "save") {
      return this.#handler.save();
    }

    const context = this.#handler.getContext();
    const model = context._model ?? options?.context?._model ?? "";
    const data = {
      ...options?.data,
      context: {
        ...context,
        ...options?.context,
      },
    };

    const result = await executeAction({
      action,
      model,
      data,
    });

    for (const item of result) {
      if (item) {
        await this.#handle(item, options);
      }
    }

    return result;
  }

  async #handle(data: ActionResult, options?: ActionOptions) {
    if (data.exportFile) {
      const link = "ws/files/data-export/" + data.exportFile;
      await download(link, data.exportFile);
    }

    if (data.signal === "refresh-app") {
      if (data.info) {
        await dialogs.box({
          title: data.info.title,
          content: data.info.message,
          yesNo: false,
          yesTitle: data.info.confirmBtnTitle,
        });
      }
      window.location.reload();
    }

    if (data.signal === "refresh-tab") {
      await this.#handler.refresh();
    }

    if (data.info) {
      await dialogs.box({
        title: data.info.title,
        content: data.info.message,
        yesNo: false,
        yesTitle: data.info.confirmBtnTitle,
      });
      if (data.pending) {
        return this.#execute(data.pending, options);
      }
    }

    if (data.notify) {
      if (Array.isArray(data.notify)) {
        data.notify.forEach((x) => alerts.info(x));
      } else {
        alerts.info(data.notify);
      }
    }

    if (data.error) {
      await dialogs.box({
        title: data.error.title,
        content: data.error.message,
        yesTitle: data.error.confirmBtnTitle,
        noTitle: data.error.confirmBtnTitle,
      });
      if (data.error.action) {
        await this.#execute(data.error.action, options);
      }
      return Promise.reject();
    }

    if (data.alert) {
      const confirmed = await dialogs.box({
        title: data.alert.title,
        content: data.alert.message,
        yesTitle: data.alert.confirmBtnTitle,
        noTitle: data.alert.confirmBtnTitle,
      });
      if (confirmed) {
        if (data.pending) {
          return await this.#execute(data.pending, options);
        }
        return;
      }
      if (data.alert.action) {
        await this.#execute(data.alert.action, options);
      }
      return Promise.reject();
    }

    if (data.errors) {
      let hasErrors = false;
      for (const [name, value] of Object.entries(data.errors)) {
        if (value.trim().length === 0) continue;
        hasErrors = true;
        this.#handler.setAttr(name, "error", value);
      }
      if (hasErrors) {
        return Promise.reject();
      }
    }

    if (data.values) {
      this.#handler.setValues(data.values);
    }

    if (data.reload) {
      await this.#handler.refresh();
      if (data.pending) {
        await this.#execute(data.pending, options);
      }
      if (data.view) {
        this.#openView(data.view);
      }
      return;
    }

    if (data.validate) {
      await this.#handler.validate();
      if (data.pending) {
        await this.#execute(data.pending, options);
      }
      return;
    }

    if (data.save) {
      await this.#handler.save();
      if (data.pending) {
        await this.#execute(data.pending, options);
      }
      return;
    }

    if (data.new) {
      await this.#handler.edit(null);
      if (data.pending) {
        await this.#execute(data.pending, options);
      }
      return;
    }

    if (data.signal) {
      await this.#handler.onSignal(data.signal, data.signalData);
    }

    if (data.attrs) {
      await this.#handleAttrs(data.attrs);
    }

    if (data.report) {
      if (data.attached) {
        const context = this.#handler.getContext();
        const attachments =
          context["$attachments"] ?? context["attachments"] ?? 0;
        this.#handler.setValue("$attachments", attachments + 1);
        const confirmed = await dialogs.confirm({
          title: i18n.get("Download"),
          content: i18n.get(
            "Report attached to current object. Would you like to download?"
          ),
        });

        if (confirmed) {
          var url = `ws/rest/com.axelor.meta.db.MetaFile/${data.attached.id}/content/download`;
          return download(url, data.attached.fileName);
        }
        return;
      }

      if (data.reportLink) {
        const url = `ws/files/report/${data.reportLink}?name=${data.reportFile}`;
        if (data.reportFormat) {
          await this.#openView({
            title: data.reportFile!,
            resource: url,
            viewType: "html",
          });
        } else {
          download(url);
        }
      }
    }

    if (data.view) {
      this.#openView(data.view);
    }

    if (data.close || data.canClose) {
      this.#handler.close();
    }
  }

  async #openView(view: {
    title?: string;
    model?: string;
    resource?: string;
    viewType?: string;
  }) {
    const name = uniqueId("$act");
    const title = view.title ?? name;
    const model = view.model ?? view.resource;
    const viewType = view.viewType ?? "grid";

    const tab: ActionView = {
      name,
      title,
      model,
      viewType,
      ...view,
    };

    if (!tab.views) {
      tab.views = [{ type: viewType } as View];
      if (tab.viewType === "html") {
        Object.assign(tab.views[0], {
          resource: tab.resource,
          title: tab.title,
        });
      }
    }

    if (tab.viewType === "html") {
      const view = tab.views.find((x) => x.type === "html") as HtmlView;
      const url = view?.name || view?.resource;

      if (view && url && tab.params?.download) {
        const fileName = tab.params?.fileName;
        return download(url, fileName);
      }

      if (view && url && tab.params?.target === "_blank") {
        window.open(url);
        return;
      }
    }

    return openTab(tab);
  }

  async #handleAttrs(data: Record<string, Record<string, any>>) {
    for (const [target, attrs] of Object.entries(data)) {
      for (const [name, value] of Object.entries(attrs)) {
        switch (name) {
          case "value":
          case "value:set":
            await this.#handler.setValue(target, value);
            break;
          case "value:add":
            await this.#handler.addValue(target, value);
            break;
          case "value:del":
            await this.#handler.delValue(target, value);
            break;
          case "focus":
            await this.#handler.setFocus(target);
            break;
          case "refresh":
            await this.#handler.refresh(target);
            break;
          default:
            await this.#handler.setAttr(target, name, value);
            break;
        }
      }
    }
  }
}
