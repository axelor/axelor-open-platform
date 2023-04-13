import { useAtomValue } from "jotai";
import { useCallback } from "react";

import { Box, Button as Btn, Image } from "@axelor/ui";

import { dialogs } from "@/components/dialogs";
import { legacyClassNames } from "@/styles/legacy";

import { FieldContainer, WidgetProps } from "../../builder";
import { useFormScope } from "../../builder/scope";

import clsx from "clsx";
import styles from "./button.module.scss";

function ButtonIcon({ schema }: WidgetProps) {
  const { icon, iconHover } = schema;
  const isImage = icon?.includes(".");

  if (!icon) return null;
  if (isImage) {
    return <Image src={icon} alt={icon} />;
  }

  return (
    <Box d="inline" className={styles.button}>
      <Box
        as="i"
        me={2}
        className={clsx(styles.icon, legacyClassNames("fa", icon), {
          [styles.hideOnHover]: iconHover,
        })}
      />
      {iconHover && (
        <Box
          as="i"
          me={2}
          className={clsx(
            styles.iconHover,
            styles.showOnHover,
            legacyClassNames("fa", iconHover)
          )}
        />
      )}
    </Box>
  );
}

export function Button(props: WidgetProps) {
  const { schema, widgetAtom } = props;
  const { showTitle = true, link, icon } = schema;
  const { attrs } = useAtomValue(widgetAtom);
  const { actionExecutor } = useFormScope();
  const { title } = attrs;

  const variant = link ? "link" : "primary";
  const readonly = props.readonly || attrs.readonly;

  const handleClick = useCallback(async () => {
    const { prompt, onClick } = schema;
    if (prompt) {
      const confirmed = await dialogs.confirm({
        content: prompt,
      });
      if (!confirmed) return;
    }
    actionExecutor.execute(onClick);
  }, [actionExecutor, schema]);

  return (
    <FieldContainer readonly={readonly}>
      <Btn variant={variant} onClick={handleClick}>
        {icon && <ButtonIcon {...props} />}
        {showTitle && title}
      </Btn>
    </FieldContainer>
  );
}
