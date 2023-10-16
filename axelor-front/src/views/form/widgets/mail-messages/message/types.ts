import { FormProps } from "@/views/form/builder";

export type MessageAuthor = {
  id: number;
  name: string;
  code: string;
  personal?: string;
  fullName?: string;
  displayName?: string;
  [key: string]: any;
};

export type MessageFile = {
  id: number;
  fileName: string;
  fileIcon?: string;
  typeIcon?: string;
  "metaFile.id"?: number;
};

export type MessageFlag = {
  isArchived?: boolean;
  isRead?: boolean;
  isStarred?: boolean;
  messageId: number;
};

export type MessageBodyTag = {
  title?: string;
  style: "important" | "warning" | "info" | "success" | "inverse";
};

export type MessageBody = {
  title?: string;
  content?: string;
  tags?: MessageBodyTag[];
  tracks?: MessageTrack[];
  files?: MessageFile[];
};

export type Message = {
  id: number;
  type: "comment" | "notification";
  body?: null | string;
  subject?: string;
  summary?: string;
  relatedId?: number;
  relatedModel?: string;
  relatedName?: string;
  parent?: Message;
  files?: MessageFile[];
  recipients?: MessageRecipient[];
  $flags?: MessageFlag;
  $author?: MessageAuthor;
  $authorModel?: string;
  $avatar?: string;
  $eventText?: string;
  $eventTime?: string;
  $eventType?: Message["type"];
  $files?: MessageFile[];
  $from?: MessageAuthor;
  $canDelete?: boolean;
  $name?: string;
  $thread?: boolean;
  $numReplies?: number;
  $children?: Message[];
};

export type MessageTrack = {
  name: string;
  title: string;
  value: string;
  oldValue?: string;
  displayValue?: string;
  oldDisplayValue?: string;
};

export type MessagePagination = {
  offset?: number;
  limit?: number;
  total?: number;
  hasNext?: boolean;
};

export type MessageFetchOptions = MessagePagination & {
  parent?: string | number;
  folder?: string;
  type?: string;
};

export interface MessageInputProps {
  focus?: boolean;
  parent?: Message;
  onSave?: (data: Message) => Promise<Message | undefined>;
  onBlur?: (value?: string) => void;
}

export interface MessageProps {
  input?: React.FunctionComponent<MessageInputProps>;
  parentId?: number;
  data: Message;
  fields?: FormProps["fields"];
  onComment?: (data: Message) => Promise<Message>;
  onRemove?: (data: Message) => Promise<Message | void>;
  onAction?: (
    data: Message,
    attrs: Partial<MessageFlag>,
    reload?: boolean
  ) => Promise<MessageFlag | undefined | void>;
  onFetch?: (
    options?: MessageFetchOptions,
    reload?: boolean
  ) => Promise<Message[] | void>;
}

export type MessageRecipient = { address: string; personal: string };
