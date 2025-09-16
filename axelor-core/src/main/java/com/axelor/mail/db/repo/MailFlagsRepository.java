/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail.db.repo;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;
import java.util.List;

public class MailFlagsRepository extends JpaRepository<MailFlags> {

  public MailFlagsRepository() {
    super(MailFlags.class);
  }

  public MailFlags findBy(MailMessage message, User user) {
    return all()
        .filter("self.message = :message AND self.user = :user")
        .bind("message", message)
        .bind("user", user)
        .fetchOne();
  }

  @Override
  public MailFlags save(MailFlags entity) {
    final MailFlags flags = super.save(entity);
    final MailMessage message = flags.getMessage();
    final MailMessage root = message.getRoot();

    if (Boolean.FALSE.equals(flags.getIsStarred())) {
      // message is root, so unflag children
      if (root == null) {
        List<MailFlags> childFlags =
            all().filter("self.message.root.id = ?", message.getId()).fetch();
        for (MailFlags child : childFlags) {
          child.setIsStarred(flags.getIsStarred());
        }
      }
    }

    if (root == null) {
      return flags;
    }

    MailFlags rootFlags = findBy(root, flags.getUser());
    if (rootFlags == null) {
      rootFlags = new MailFlags();
      rootFlags.setMessage(root);
      rootFlags.setUser(flags.getUser());
      super.save(rootFlags);
    }

    rootFlags.setIsStarred(flags.getIsStarred());

    // mark root as unread
    if (!Boolean.TRUE.equals(flags.getIsRead())) {
      rootFlags.setIsRead(false);
    }

    return flags;
  }
}
