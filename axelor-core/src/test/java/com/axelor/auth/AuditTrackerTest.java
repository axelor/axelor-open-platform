/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.JpaTest;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.mail.db.MailMessage;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.team.db.TeamTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

public class AuditTrackerTest extends JpaTest {

  @Inject private ObjectMapper objectMapper;

  @Inject private MetaFiles metaFiles;

  private User newUser(String code) {
    User user = new User(code, code);
    user.setPassword("secret");
    return JPA.save(user);
  }

  private TeamTask newTask(String name, User assignedTo) {
    TeamTask task = new TeamTask();
    task.setName(name);
    task.setAssignedTo(assignedTo);
    return JPA.save(task);
  }

  private List<MailMessage> messages(TeamTask task) {
    return Query.of(MailMessage.class)
        .filter("self.relatedModel = :model AND self.relatedId = :id")
        .bind("model", TeamTask.class.getName())
        .bind("id", task.getId())
        .order("id")
        .fetch();
  }

  @Test
  void testTrackedReferenceDeletedInSameTransaction() throws Exception {
    final User[] users = new User[2];
    final TeamTask task =
        JPA.withTransaction(
            () -> {
              users[0] = newUser("audit-ref-old");
              users[1] = newUser("audit-ref-new");
              return newTask("audit-ref-task", users[0]);
            });
    final Long oldUserId = users[0].getId();
    final Long newUserId = users[1].getId();

    JPA.clear();

    // update tracked many-to-one, then delete its previous target
    assertDoesNotThrow(
        () ->
            JPA.runInTransaction(
                () -> {
                  TeamTask found = JPA.find(TeamTask.class, task.getId());
                  found.setAssignedTo(JPA.find(User.class, newUserId));
                  JPA.flush();
                  JPA.remove(JPA.find(User.class, oldUserId));
                }));

    JPA.clear();

    assertNull(JPA.find(User.class, oldUserId));
    assertEquals(newUserId, JPA.find(TeamTask.class, task.getId()).getAssignedTo().getId());

    final MailMessage message =
        messages(task).stream()
            .filter(m -> "Record updated".equals(m.getSubject()))
            .findFirst()
            .orElse(null);
    assertNotNull(message);

    final Map<String, Object> body =
        objectMapper.readValue(message.getBody(), new TypeReference<Map<String, Object>>() {});
    @SuppressWarnings("unchecked")
    final List<Map<String, String>> tracks = (List<Map<String, String>>) body.get("tracks");
    final Map<String, String> track =
        tracks.stream().filter(t -> "assignedTo".equals(t.get("name"))).findFirst().orElse(null);
    assertNotNull(track);
    assertEquals("audit-ref-new", track.get("value"));
    assertEquals(String.valueOf(oldUserId), track.get("oldValue"));
  }

  @Test
  void testTrackedEntityDeletedInSameTransaction() {
    final TeamTask task = JPA.withTransaction(() -> newTask("audit-del-task", null));

    JPA.clear();

    // update tracked entity, then delete it
    assertDoesNotThrow(
        () ->
            JPA.runInTransaction(
                () -> {
                  TeamTask found = JPA.find(TeamTask.class, task.getId());
                  found.setName("audit-del-task-renamed");
                  JPA.flush();
                  JPA.remove(found);
                }));

    JPA.clear();

    assertNull(JPA.find(TeamTask.class, task.getId()));
    assertEquals(
        0, messages(task).stream().filter(m -> "Record updated".equals(m.getSubject())).count());
  }

  @Test
  void testDeleteAttachmentsFailureFailsTransaction() {
    final TeamTask task =
        JPA.withTransaction(
            () -> {
              TeamTask owner = newTask("audit-file-task", null);
              MetaFile file;
              try {
                file =
                    metaFiles.upload(
                        new ByteArrayInputStream("shared".getBytes(StandardCharsets.UTF_8)),
                        "audit-shared.txt");
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              metaFiles.attach(file, null, owner);
              // shared with other records, so it can't be deleted with the owner's attachments
              JPA.save(metaFiles.attach(file, newTask("audit-file-other1", null)));
              JPA.save(metaFiles.attach(file, newTask("audit-file-other2", null)));
              return owner;
            });

    JPA.clear();

    assertThrows(
        PersistenceException.class,
        () -> JPA.runInTransaction(() -> JPA.remove(JPA.find(TeamTask.class, task.getId()))));

    JPA.clear();

    assertNotNull(JPA.find(TeamTask.class, task.getId()));
  }
}
