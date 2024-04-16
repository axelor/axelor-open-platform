import { Box, CommandBar, CommandItemProps, clsx } from "@axelor/ui";
import { FunctionComponent, memo } from "react";

import { EvalContextOptions } from "@/hooks/use-parser/context";
import { SearchResult } from "@/services/client/data";
import { CardsView, Property } from "@/services/client/meta.types";
import { DataContext, DataRecord } from "@/services/client/data.types";
import { i18n } from "@/services/client/i18n";
import { CardTemplate } from "./card-template";
import { useCardClassName } from "./use-card-classname";
import classes from "./card.module.scss";

export const Card = memo(function Card({
  record,
  fields,
  view,
  onEdit,
  onView,
  onDelete,
  onRefresh,
  Template,
  width,
  minWidth,
}: {
  record: DataRecord;
  view: CardsView;
  fields?: Record<string, Property>;
  onEdit?: (record: DataRecord) => void;
  onView?: (record: DataRecord) => void;
  onDelete?: (record: DataRecord) => void;
  onRefresh?: () => Promise<SearchResult>;
  Template: FunctionComponent<{
    context: DataContext;
    options?: EvalContextOptions;
  }>;
  width?: string;
  minWidth?: string;
}) {
  const className = useCardClassName(view, record);

  function handleClick() {
    onView?.(record);
  }

  const commandItems: CommandItemProps[] = [
    {
      key: "menu",
      iconProps: { icon: "arrow_drop_down" },
      items: [
        {
          key: "edit",
          text: i18n.get("Edit"),
          hidden: !onEdit,
          onClick: () => onEdit?.(record),
        },
        {
          key: "delete",
          text: i18n.get("Delete"),
          hidden: !onDelete,
          onClick: () => onDelete?.(record),
        },
      ],
    },
  ];

  const showActions = onEdit || onDelete;
  return (
    <>
      <Box
        d="flex"
        px={{ base: 1, md: 2 }}
        mb={{ base: 2, md: 3 }}
        className={classes.card}
        style={{
          width,
          minWidth,
        }}
      >
        <Box
          className={clsx(classes.cardContent, className)}
          p={{ base: 2, md: 3 }}
          bgColor="body"
          w={100}
          rounded
          shadow="sm"
          onClick={handleClick}
        >
          <CardTemplate
            component={Template}
            record={record}
            fields={fields}
            onRefresh={onRefresh}
          />
        </Box>

        {showActions && (
          <CommandBar className={classes.menuIcon} items={commandItems} />
        )}
      </Box>
    </>
  );
});
