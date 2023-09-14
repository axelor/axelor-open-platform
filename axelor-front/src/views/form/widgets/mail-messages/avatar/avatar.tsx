import clsx from "clsx";
import React, { useEffect, useMemo, useState } from "react";

import { legacyClassNames } from "@/styles/legacy";
import { MessageAuthor } from "../message/types";
import { checkUrl, getAvatarText, getColor, getName } from "./utils";

import classes from "./avatar.module.scss";

function Avatar({
  user,
  image: propImage,
  title: propTitle,
  text: propText,
  color: propColor,
  ...props
}: {
  user?: MessageAuthor;
  image?: string;
  title?: string;
  text?: string;
  color?: string;
  className?: string;
}) {
  const [canShowImage, setCanShowImage] = useState<boolean | undefined>();
  const userName = useMemo(() => user && getName(user), [user]);
  const image = propImage || user?.$avatar;
  const title = propTitle || userName;
  const [text, color] = useMemo(() => {
    if (image && canShowImage == null) {
      // Still checking for image access
      // Show empty avatar to avoid blinking
      return ["", ""];
    }
    return [
      propText || (user && getAvatarText(user)),
      propColor || (user ? legacyClassNames(getColor(user)) : classes.text),
    ];
  }, [propText, propColor, user, image, canShowImage]);

  useEffect(() => {
    setCanShowImage(false);
    checkUrl(
      image,
      () => setCanShowImage(true),
      () => setCanShowImage(false)
    );
  }, [image]);

  if (canShowImage) {
    return (
      <img
        {...props}
        className={clsx(classes.avatar, classes.image, props.className)}
        src={image}
        alt={userName}
        title={title}
      />
    );
  }

  return (
    <span
      {...props}
      className={clsx(classes.avatar, classes.letter, color, props.className)}
      title={title}
    >
      {text}
    </span>
  );
}

export default React.memo(Avatar);
