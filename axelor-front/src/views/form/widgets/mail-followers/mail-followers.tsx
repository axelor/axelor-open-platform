import React, { useCallback, useState } from "react";
import { WidgetProps } from "../../builder";
import { Box, Link, ListItem } from "@axelor/ui";
import { i18n } from "@/services/client/i18n";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { useAsyncEffect } from "@/hooks/use-async-effect";
import { dialogs } from "@/components/dialogs";
import { Follower, follow, getFollowers, unfollow } from "./utils";
import { useSession } from "@/hooks/use-session";
import classes from "./mail-followers.module.scss";

export function MailFollowers({ schema }: WidgetProps) {
  const [followers, setFollowers] = useState<Follower[]>([]);
  const { data: session } = useSession();
  const { model, modelId } = schema;

  useAsyncEffect(async () => {
    const list = await getFollowers(model, modelId);
    setFollowers(list);
  }, [model, modelId]);

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

  const isLoginUserFollowing = React.useMemo(
    () =>
      followers?.find(
        ({ $author }) => String($author?.code) === String(session?.user.login)
      ),
    [followers, session]
  );

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
            <MaterialIcon icon="star" fill={isLoginUserFollowing ? 1 : 0} />
          </Box>
          <Box as="span" className={classes.icon}>
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
                title="Remove"
                pe={1}
                ps={1}
                className={classes.icon}
                onClick={() => handleUnfollow(id)}
              >
                <MaterialIcon icon="close" weight={700} opticalSize={20} />
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
