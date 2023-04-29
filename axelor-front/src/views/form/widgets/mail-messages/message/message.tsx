import React, { useMemo, useState } from "react";
import clsx from "clsx";
import { Box, Button, Badge, TBackground } from "@axelor/ui";

import Avatar from "../avatar";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import { MessageMenu } from "./message-menu";
import { MessageFiles } from "./message-files";
import { MessageTracks } from "./message-track";
import { MessageInput } from "./message-input";
import { getUser, getUserName } from "./utils";
import { i18n } from "@/services/client/i18n";
import { moment } from "@/services/client/l10n";
import { FormProps } from "@/views/form/builder";
import * as TYPES from "./types";
import { MessageInputProps, MessageProps } from "./types";
import styles from "./message.module.scss";

const TagStyle: Record<TYPES.MessageBodyTag["style"], TBackground> = {
  important: "danger",
  warning: "warning",
  info: "info",
  success: "success",
  inverse: "dark",
};

function MessageHTMLContent({ html: __html }: { html: string }) {
  return <div dangerouslySetInnerHTML={{ __html }} />;
}

function MessageEvent({ data }: { data: TYPES.Message }) {
  const { $eventText, $eventTime } = data;
  return (
    <>
      {" "}
      {$eventText} {" - "}
      <Box as="a" title={moment($eventTime).format("DD/MM/YYYY HH:mm")}>
        {moment($eventTime).fromNow()}
      </Box>
    </>
  );
}

export const Message = React.memo(function Message(props: MessageProps) {
  const [input, setInput] = useState(false);
  const { parentId, data, fields, onComment, onFetch, onAction, onRemove } =
    props;
  const {
    subject,
    relatedId,
    relatedModel,
    summary,
    $name,
    $thread,
    $numReplies = 0,
    $data = [],
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
        reload
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
            className={styles["arrow-right"]}
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
              <MessageTracks fields={fields} data={body.tracks} />
            )}

            {body && body.content && <MessageHTMLContent html={body.content} />}
            {!body && (
              <MessageHTMLContent html={(summary || data.body) as string} />
            )}

            {body?.files && <MessageFiles data={body.files} />}
            {$files && <MessageFiles data={$files} />}
          </Box>
          <Box p={1} ps={4} pe={4}>
            {$numReplies > 0 && (
              <span className={styles["pull-right"]}>
                <Box
                  color="primary"
                  as="a"
                  mb={0}
                  onClick={fetchReplies}
                  className={styles["pull-right"]}
                >
                  {i18n.get("replies ({0} of {1})", $data.length, $numReplies)}
                </Box>
              </span>
            )}
            <span>
              <Box
                as="a"
                {...($author?.id && $authorModel
                  ? { href: `#/ds/form::${$authorModel}/edit/${$author.id}` }
                  : {})}
              >
                {getUserName(data)}
              </Box>
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
        {$data.length > 0 && (
          <Box p={2} pe={0}>
            {$data.map((msg) => (
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
  onFetch,
  onLoad,
  onAction,
  onComment,
  onCommentRemove,
}: {
  isMail?: boolean;
  data: TYPES.Message[];
  fields?: FormProps["fields"];
  inputProps?: MessageInputProps;
  onFetch?: MessageProps["onFetch"];
  onLoad?: () => void;
  onAction?: MessageProps["onAction"];
  onComment?: MessageProps["onComment"];
  onCommentRemove?: MessageProps["onRemove"];
}) {
  const rtl = false;
  return (
    <Box
      flex={1}
      d="flex"
      flexDirection="column"
      className={clsx(styles.root, "mail-messages", {
        [styles.rtl]: rtl,
      })}
    >
      {!isMail && (
        <MessageInput focus={false} onSave={onComment} {...MessageInputProps} />
      )}
      <Box ms={3} mt={1} borderStart>
        {data.map((message, index) => (
          <Message
            key={index}
            fields={fields}
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
            onClick={(e) => onLoad()}
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
    </Box>
  );
}
