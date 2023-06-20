import React, { useCallback, useState } from "react";
import { Box, Link, ListItem } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";
import { useAtomCallback } from "jotai/utils";

import { WidgetProps } from "../../builder";
import { i18n } from "@/services/client/i18n";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { dialogs } from "@/components/dialogs";
import { Follower, follow, getFollowers, unfollow } from "./utils";
import { useSession } from "@/hooks/use-session";
import { useMessagePopup } from "../mail-messages/message/message-form";
import { useFormRefresh } from "../../builder/scope";
import { Message } from "../mail-messages/message/types";
import classes from "./mail-followers.module.scss";

export function MailFollowers({ schema, formAtom }: WidgetProps) {
  const [followers, setFollowers] = useState<Follower[]>([]);
  const { data: session } = useSession();
  const { model, modelId } = schema;
  const showMessagePopup = useMessagePopup();

  const getRecordTitle = useAtomCallback(
    useCallback(
      (get) => {
        const { record, fields } = get(formAtom);
        const nameColumn = Object.keys(fields ?? {}).find(
          (k: string) => fields[k]?.nameColumn === true
        );
        return record[nameColumn!] || record.name || record.code || "";
      },
      [formAtom]
    )
  );

  const onRefresh = useCallback(async () => {
    const list = await getFollowers(model, modelId);
    setFollowers(list);
  }, [model, modelId]);

  useAsyncEffect(async () => {
    onRefresh();
  }, [onRefresh]);

  const handleFollow = useCallback(
    async (data?: any) => {
      const list = await follow(model, modelId, data);
      setFollowers(list);
    },
    [model, modelId]
  );

  const handleUnfollow = useCallback(
    async (id?: number) => {
      const confirmed = await dialogs.confirm({
        title: i18n.get("Question"),
        content: i18n.get("Are you sure to unfollow this document?"),
      });
      if (confirmed) {
        const list = await unfollow(model, modelId, id ? [id] : []);
        setFollowers(list);
      }
    },
    [model, modelId]
  );

  const handleAddFollower = useCallback(async () => {
    showMessagePopup({
      title: i18n.get("Add followers"),
      yesTitle: i18n.get("Add"),
      record: { subject: getRecordTitle() } as Message,
      onSave: handleFollow,
    });
  }, [getRecordTitle, handleFollow, showMessagePopup]);

  const isLoginUserFollowing = React.useMemo(
    () =>
      followers?.some(
        ({ $author }) => String($author?.code) === String(session?.user.login)
      ),
    [followers, session]
  );
  
// register form:refresh
  useFormRefresh(onRefresh);

  return (
    <Box d="flex" flexDirection="column" rounded border>
      <Box d="flex" fontWeight={"bold"} borderBottom px={3} py={2}>
        <Box flex={1}>{i18n.get("Followers")}</Box>
        <Box>
          <Box
            as="span"
            className={classes.icon}
            onClick={() =>
              isLoginUserFollowing ? handleUnfollow() : handleFollow()
            }
          >
            <MaterialIcon icon="star" fill={isLoginUserFollowing} />
          </Box>
          <Box as="span" className={classes.icon} onClick={handleAddFollower}>
            <MaterialIcon icon="add" />
          </Box>
        </Box>
      </Box>
      <Box p={3}>
        {followers.map(({ id, $author, $authorModel }) => {
          const title = $author?.fullName || $author?.name;
          return (
            <ListItem
              key={id}
              className={classes.follower}
              py={1}
              border={false}
            >
              <Box
                d="flex"
                title={i18n.get("Remove")}
                pe={1}
                ps={1}
                className={classes.icon}
                onClick={() => handleUnfollow(id)}
              >
                <MaterialIcon icon="close" fontSize={20} />
              </Box>
              <Link
                title={title!}
                href={`#/ds/form::${$authorModel}/edit/${$author?.id || ""}`}
              >
                {title!}
              </Link>
            </ListItem>
          );
        })}
      </Box>
    </Box>
  );
}
