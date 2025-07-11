import React, { LegacyRef, useMemo } from "react";
import { Icon } from "@/components/icon";
import { legacyClassNames } from "@/styles/legacy";
import { i18n } from "@/services/client/i18n";
import { useTemplateContext } from "@/hooks/use-parser";
import { parseExpression } from "@/hooks/use-parser/utils";

export const ReportBox = React.forwardRef(function ReportBox(
  props: {
    icon: string;
    label: string;
    tag?: string;
    "tag-css"?: string;
    percent?: number | string;
    up?: string;
    value: number | string;
  },
  _,
) {
  const tagCssProp = props["tag-css"];
  const { icon, label } = props;
  const context = useTemplateContext();
  const ref = React.useRef<HTMLDivElement>();

  const { value, percent, tag, tagCss, up } = useMemo(
    () => ({
      value: parseExpression(String(props.value))(context),
      percent: parseExpression(String(props.percent))(context),
      tag: parseExpression(String(props.tag))(context),
      tagCss: parseExpression(String(tagCssProp))(context),
      up: parseExpression(String(props.up))(context),
    }),
    [context, props.value, props.percent, props.tag, props.up, tagCssProp],
  );

  function format(text: string | number) {
    return isNaN(text as number) ? text : context.__number(text);
  }

  function percentStyle() {
    let type;
    if (up == null) {
      type = "info";
    } else {
      type = up ? "success" : "error";
    }
    return "text-" + type;
  }

  React.useEffect(() => {
    const el = ref.current;
    if (el) {
      const dashlet = el?.parentElement?.parentElement;
      if (dashlet) {
        const className = legacyClassNames("report-box");
        dashlet.classList.add(className);
        return () => dashlet.classList.remove(className);
      }
    }
  }, []);

  return (
    <div
      ref={ref as LegacyRef<HTMLDivElement>}
      className={legacyClassNames("report-data", "hidden")}
    >
      {icon && icon.includes("fa-") && (
        <i className={legacyClassNames("report-icon", "fa", icon)} />
      )}
      {icon && !icon.includes("fa-") && (
        <Icon icon={icon} className={legacyClassNames("report-icon")} />
      )}
      <div>
        <h1>{format(value)}</h1>
        <small>{i18n.get(label)}</small>
        <div
          className={legacyClassNames(
            "report-percent",
            "font-bold",
            "pull-right",
            percentStyle(),
          )}
        >
          <span>{context.__percent(percent)}</span>
          {up != null && <Icon icon={"trending_" + (up ? "up" : "down")} />}
        </div>
      </div>
      {tag && (
        <div className={legacyClassNames("report-tags")}>
          <span className={legacyClassNames("label", tagCss)}>{tag}</span>
        </div>
      )}
    </div>
  );
});
