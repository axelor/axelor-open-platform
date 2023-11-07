import { useAtomCallback } from "jotai/utils";
import React, { useCallback, useState } from "react";

import { ListItem, Panel } from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { dialogs } from "@/components/dialogs";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { useSession } from "@/hooks/use-session";
import { i18n } from "@/services/client/i18n";

import { WidgetProps } from "../../builder";
import { useAfterActions, useFormRefresh } from "../../builder/scope";
import { MessageUser } from "../mail-messages/message";
import { useMessagePopup } from "../mail-messages/message/message-form";
import { Message } from "../mail-messages/message/types";
import { Follower, follow, getFollowers, unfollow } from "./utils";

import classes from "./mail-followers.module.scss";

export function MailFollowers({ schema, formAtom }: WidgetProps) {
  const [followers, setFollowers] = useState<Follower[]>([]);
  const { data: session } = useSession();
  const { model, modelId } = schema;

  const loadFollowers = useAfterActions(getFollowers);
  const showMessagePopup = useMessagePopup();
  const getRecordTitle = useAtomCallback(
    useCallback(
      (get) => {
        const { record, fields } = get(formAtom);
        const nameColumn = Object.keys(fields ?? {}).find(
          (k: string) => fields[k]?.nameColumn === true,
        );
        return record[nameColumn!] || record.name || record.code || "";
      },
      [formAtom],
    ),
  );

  const onRefresh = useCallback(async () => {
    const list = await loadFollowers(model, modelId);
    setFollowers(list ?? []);
  }, [loadFollowers, model, modelId]);

  useAsyncEffect(async () => {
    onRefresh();
  }, [onRefresh]);

  const handleFollow = useCallback(
    async (data?: any) => {
      const list = await follow(model, modelId, data);
      setFollowers(list);
    },
    [model, modelId],
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
    [model, modelId],
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
        ({ $author }) => String($author?.code) === String(session?.user?.login),
      ),
    [followers, session],
  );

  // register form:refresh
  useFormRefresh(onRefresh);

  return (
    <Panel
      header={i18n.get("Followers")}
      toolbar={{
        iconOnly: true,
        items: [
          {
            key: "follow",
            iconProps: {
              icon: "star",
              fill: false,
            },
            hidden: isLoginUserFollowing,
            onClick: () => handleFollow(),
          },
          {
            key: "unfollow",
            iconProps: {
              icon: "star",
              fill: true,
            },
            hidden: !isLoginUserFollowing,
            onClick: () => handleUnfollow(),
          },
          {
            key: "add",
            iconProps: {
              icon: "add",
            },
            onClick: () => handleAddFollower(),
          },
        ],
      }}
    >
      {followers.map(({ id, $author, $authorModel }) => {
        const keys = [
          session?.user?.nameField ?? "",
          "fullName",
          "displayName",
          "personal",
          "name",
        ];
        const key = keys.find((key) => $author?.[key]);
        const title = $author?.[key ?? ""];
        return (
          <ListItem key={id} className={classes.follower} py={1} border={false}>
            <div
              title={i18n.get("Remove")}
              className={classes.icon}
              onClick={() => handleUnfollow(id)}
            >
              <MaterialIcon icon="close" fontSize={20} />
            </div>
            <MessageUser title={title!} id={$author?.id} model={$authorModel} />
          </ListItem>
        );
      })}
    </Panel>
  );
}
