import React, { MouseEvent, useMemo, useState } from "react";

import {
  clsx,
  Badge,
  Box,
  Button,
  Panel,
  TBackground,
  useTheme,
} from "@axelor/ui";
import { MaterialIcon } from "@axelor/ui/icons/material-icon";

import { useEditor } from "@/hooks/use-relation";
import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import { findView } from "@/services/client/meta-cache";
import { MetaData } from "@/services/client/meta.ts";
import { SanitizedContent } from "@/utils/sanitize.ts";
import { FormProps } from "@/views/form/builder";

import Avatar from "../avatar";
import { MessageFiles } from "./message-files";
import { MessageInput } from "./message-input";
import { MessageMenu } from "./message-menu";
import { MessageTracks } from "./message-track";
import * as TYPES from "./types";
import { MessageInputProps, MessageProps } from "./types";
import { getUser, getUserName } from "./utils";
import { isDevelopment } from "@/utils/app-settings";

import styles from "./message.module.scss";

const TagStyle: Record<TYPES.MessageBodyTag["style"], TBackground> = {
  important: "danger",
  warning: "warning",
  info: "info",
  success: "success",
  inverse: "dark",
};

function MessageEvent({ data }: { data: TYPES.Message }) {
  const { $eventText, $eventTime } = data;
  return (
    <>
      {" "}
      {$eventText} {" - "}
      <Box as="a" title={moment($eventTime).format("L LT")}>
        {moment($eventTime).fromNow()}
      </Box>
    </>
  );
}

export function MessageUser({
  id,
  model = "com.axelor.auth.db.User",
  title,
}: {
  title: string;
  id?: number;
  model?: string;
}) {
  const showEditor = useEditor();

  async function handleClick(e: MouseEvent<HTMLAnchorElement>) {
    e.preventDefault();
    if (model !== "com.axelor.auth.db.User") {
      return;
    }
    const name = "user-info-form";
    try {
      const { view } =
        (await findView({
          type: "form",
          name: "user-info-form",
          model,
        })) || {};
      if (view && id) {
        showEditor({
          title: view.title ?? i18n.get("User"),
          model,
          viewName: view.name,
          record: { id },
          readonly: true,
          canSave: false,
          canAttach: false,
        });
      } else if (isDevelopment() && !view) {
        console.log(`${name} view doesn't exist`);
      }
    } catch (err) {
      // handle error
    }
  }

  return (
    <Box as="a" href="" onClick={handleClick}>
      {title}
    </Box>
  );
}

export const Message = React.memo(function Message(props: MessageProps) {
  const [input, setInput] = useState(false);
  const {
    parentId,
    data,
    fields,
    jsonFields,
    onComment,
    onFetch,
    onAction,
    onRemove,
  } = props;
  const {
    subject,
    relatedId,
    relatedModel,
    summary,
    $name,
    $thread,
    $numReplies = 0,
    $children = [],
    $files,
    $avatar,
    $author,
    $authorModel,
  } = data;

  const body = useMemo<TYPES.MessageBody | null>(() => {
    let body: string | null = data.body || "{}";
    try {
      body = JSON.parse(data.body || "");
      if (typeof body !== "object") {
        body = null;
      }
    } catch (e) {
      body = null;
    }
    return body;
  }, [data.body]);

  const $title = body?.title || subject;

  function doRemove() {
    onRemove &&
      onRemove({ ...data, parent: { id: parentId } as TYPES.Message });
  }

  function doAction(attrs: Partial<TYPES.MessageFlag>, reload?: boolean) {
    onAction &&
      onAction(
        { ...data, parent: { id: parentId } as TYPES.Message },
        attrs,
        reload,
      );
  }

  function doReply() {
    setInput(true);
  }

  function checkReply(value?: string) {
    !value && setInput(false);
  }

  async function addReply(_data: TYPES.Message) {
    setInput(false);
    if (onComment) {
      const msg = await onComment({
        ..._data,
        parent: { id: data.id } as TYPES.Message,
      });
      msg && fetchReplies();
      return msg;
    }
  }

  function fetchReplies() {
    onFetch && onFetch({ parent: parentId || data.id });
  }

  function renderLink(label: string) {
    return relatedId ? (
      <a href={`#/ds/form::${relatedModel}/edit/${relatedId}`}>
        {i18n.get(label)}
      </a>
    ) : (
      <span>{i18n.get(label)}</span>
    );
  }

  return (
    <Box
      position="relative"
      d="flex"
      className={clsx(styles["message-container"], "message")}
    >
      <Box as="span" className={styles.avatar}>
        <Avatar user={getUser(data)!} image={$avatar} />
      </Box>
      <MessageMenu
        data={data}
        onReply={doReply}
        onAction={doAction}
        onRemove={doRemove}
      />
      <Box d="flex" flex={1} flexDirection="column">
        <Box w={100} border rounded bgColor="body">
          <Box
            as="span"
            className={styles["arrow-left"]}
            position="absolute"
            border
            borderEnd={false}
            borderBottom={false}
          />
          {($name || $title) && (
            <Box
              d="flex"
              alignItems="center"
              flexWrap="wrap"
              flex={1}
              p={2}
              borderBottom
            >
              <Box as="p" mb={0}>
                {$thread ? (
                  <>
                    {$name && renderLink($name)}
                    {$name && $title && <span>{" - "}</span>}
                    {$title && renderLink($title)}
                  </>
                ) : (
                  <>
                    {$name && <span>{$name}</span>}
                    {$name && <span>{" - "}</span>}
                    {$title && <span>{i18n.get($title)}</span>}
                  </>
                )}
              </Box>
              {body && body?.tags && body?.tags?.length > 0 && (
                <>
                  <span> {" : "} </span>
                  <span>
                    {body.tags.map((tag, index) => (
                      <Box d="inline-block" key={index} as="h6" m={0} ms={1}>
                        <Badge bg={TagStyle[tag.style]}>
                          {i18n.get(tag.title!)}
                        </Badge>
                      </Box>
                    ))}
                  </span>
                </>
              )}
            </Box>
          )}
          <Box pt={1} ps={4} pe={4}>
            {body?.tracks && (
              <MessageTracks
                fields={fields}
                jsonFields={jsonFields}
                data={body.tracks}
              />
            )}

            {body && body.content && (
              <SanitizedContent content={body.content} />
            )}
            {!body && (
              <SanitizedContent content={(summary || data.body) as string} />
            )}

            {body?.files && <MessageFiles data={body.files} />}
            {$files && <MessageFiles data={$files} />}
          </Box>
          <Box p={1} ps={4} pe={4} style={{ fontSize: "smaller" }}>
            {$numReplies > 0 && (
              <span className={styles["pull-right"]}>
                <Box
                  color="primary"
                  as="a"
                  mb={0}
                  onClick={fetchReplies}
                  className={styles["pull-right"]}
                >
                  {i18n.get(
                    "replies ({0} of {1})",
                    $children.length,
                    $numReplies,
                  )}
                </Box>
              </span>
            )}
            <span>
              <MessageUser
                title={getUserName(data)}
                id={$author?.id}
                model={$authorModel}
              />
              <MessageEvent data={data} />
            </span>
          </Box>
        </Box>
        {input && (
          <Box mt={1} mb={1}>
            <MessageInput
              parent={data}
              focus={true}
              onSave={addReply}
              onBlur={checkReply}
            />
          </Box>
        )}
        {$children.length > 0 && (
          <Box p={2} pe={0}>
            {$children.map((msg) => (
              <Message {...props} parentId={data.id} key={msg.id} data={msg} />
            ))}
          </Box>
        )}
      </Box>
    </Box>
  );
});

export function MessageBox({
  isMail,
  inputProps: MessageInputProps,
  data,
  fields,
  jsonFields,
  filter,
  onFilterChange,
  onFetch,
  onLoad,
  onAction,
  onComment,
  onCommentRemove,
}: {
  isMail?: boolean;
  data: TYPES.Message[];
  fields?: FormProps["fields"];
  jsonFields?: MetaData["jsonFields"];
  inputProps?: MessageInputProps;
  filter?: string;
  onFilterChange?: (filter?: string) => void;
  onFetch?: MessageProps["onFetch"];
  onLoad?: () => void;
  onAction?: MessageProps["onAction"];
  onComment?: MessageProps["onComment"];
  onCommentRemove?: MessageProps["onRemove"];
}) {
  const rtl = useTheme().dir === "rtl";

  const filters = useMemo(
    () => [
      { title: i18n.get("All"), value: undefined },
      { title: i18n.get("Comments"), value: "comment" },
      { title: i18n.get("Notifications"), value: "notification" },
    ],
    [],
  );

  return (
    <Panel
      className={clsx(styles.root, "mail-messages", {
        [styles.rtl]: rtl,
        [styles["mail-box"]]: isMail,
      })}
    >
      {!isMail && (
        <Box d="flex" flexDirection="column" g={2} mb={3}>
          <Box d="flex" flexDirection={{ base: "column", md: "row" }} g={2}>
            {filters.map(({ title, value }, ind) => (
              <Button
                key={ind}
                variant="primary"
                outline={filter !== value}
                onClick={() => onFilterChange?.(value)}
                flexGrow={{ base: 1, md: 0 }}
              >
                {title}
              </Button>
            ))}
          </Box>
          {filter !== "notification" && (
            <MessageInput
              focus={false}
              onSave={onComment}
              {...MessageInputProps}
            />
          )}
        </Box>
      )}
      <Box ms={3} mt={1} borderStart>
        {data.map((message, index) => (
          <Message
            key={index}
            fields={fields}
            jsonFields={jsonFields}
            data={message}
            onComment={onComment}
            onAction={onAction}
            onRemove={onCommentRemove}
            onFetch={onFetch}
          />
        ))}
      </Box>
      {onLoad && (
        <Box d="flex" justifyContent="center">
          <Button
            size="sm"
            variant="primary"
            outline
            onClick={() => onLoad()}
            className="load-more"
            mt={2}
            ms={1}
            me={1}
            mb={3}
          >
            <Box d="flex" as="span" ms={1}>
              {i18n.get("load more")}
              <MaterialIcon icon="east" />
            </Box>
          </Button>
        </Box>
      )}
    </Panel>
  );
}
