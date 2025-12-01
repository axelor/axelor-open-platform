/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.cli.commands;

import com.axelor.app.cli.AbstractCliCommand;
import com.axelor.audit.db.AuditLog;
import com.axelor.common.Inflector;
import com.axelor.concurrent.ContextAware;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.audit.AuditProcessor;
import com.axelor.db.tenants.TenantConfigProvider;
import com.axelor.inject.Beans;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.Session;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "audit", description = "Process pending audit logs.", sortOptions = false)
public class AuditCommand extends AbstractCliCommand {

  @Option(
      names = {"-t", "--tenant"},
      arity = "1..*",
      description = "Process pending audit logs for specific tenant(s).",
      order = 1)
  List<String> tenants = new ArrayList<>();

  @Option(
      names = {"-e", "--export-dir"},
      description = "Directory to export audit logs data.",
      order = 2)
  Path exportDir;

  @Option(
      names = {"--clean"},
      description = "Clean processed audit logs after export.",
      order = 3)
  boolean clean;

  @Override
  public void run() {
    withSession(
        () -> {
          Beans.get(TenantConfigProvider.class).findAll().stream()
              .filter(x -> Boolean.TRUE.equals(x.getActive()))
              .map(x -> x.getTenantId())
              .filter(x -> tenants.isEmpty() || tenants.contains(x))
              .forEach(this::handle);
        });
  }

  private void handle(String tenantId) {
    if (exportDir == null) {
      process(tenantId);
    } else {
      export(tenantId);
    }
  }

  private void process(String tenantId) {
    ContextAware.of()
        .withTenantId(tenantId)
        .withTransaction(true)
        .build((Runnable) Beans.get(AuditProcessor.class)::process)
        .run();
  }

  private void export(String tenantId) {
    var count = new AtomicInteger(0);
    var baseName = Inflector.getInstance().dasherize(tenantId);
    var timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    var fileName = String.format("%s_audit-log-%s.sql", baseName, timeStamp);
    var filePath = exportDir.resolve(fileName);

    // Ensure audit logs are processed before export
    process(tenantId);

    // Export processed audit logs
    ContextAware.of()
        .withTenantId(tenantId)
        .withTransaction(false)
        .build(
            () -> {
              var session = JPA.em().unwrap(Session.class);
              session.doWork(
                  connection -> {
                    try {
                      // Disable auto-commit for efficient batch processing
                      connection.setAutoCommit(false);
                      count.set(export(connection, tenantId, filePath));
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  });
            })
        .run();

    // Clean processed audit logs after export
    if (clean && count.get() > 0) {
      log.info("Deleting processed audit logs for tenant '{}'", tenantId);
      try {
        var deleted =
            ContextAware.of()
                .withTenantId(tenantId)
                .withTransaction(true)
                .build(Query.of(AuditLog.class).filter("self.processed = true")::delete)
                .call();
        log.info("Deleted {} processed audit logs for tenant '{}'", deleted, tenantId);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private int export(Connection connection, String tenantId, Path filePath) throws IOException {
    var BATCH_SIZE = 500;
    var session = JPA.em().unwrap(Session.class);
    var count = new AtomicInteger(0);

    Files.createDirectories(exportDir);

    session.doWork(
        conn -> {
          var select =
              List.of(
                  "id",
                  "tx_id",
                  "created_on",
                  "user_id",
                  "related_model",
                  "related_id",
                  "event_type",
                  "current_state",
                  "previous_state");
          var orderBy = List.of("tx_id", "related_model", "related_id", "created_on");
          var query =
              String.format(
                  """
                  SELECT %s
                  FROM audit_log
                  WHERE processed = TRUE
                  ORDER BY %s
                  """,
                  String.join(", ", select), String.join(", ", orderBy));

          log.info("Exporting audit logs for tenant '{}' to file: {}", tenantId, filePath);

          try (var writer = Files.newBufferedWriter(filePath);
              var ps = conn.prepareStatement(query); ) {

            ps.setFetchSize(BATCH_SIZE);
            try (var rs = ps.executeQuery()) {
              while (rs.next()) {
                var values = new ArrayList<String>();
                for (var i = 1; i <= select.size(); i++) {
                  var value = rs.getObject(i);
                  if (value == null) {
                    values.add("NULL");
                  } else if (value instanceof String || value instanceof java.sql.Timestamp) {
                    values.add("'" + value.toString().replace("'", "''") + "'");
                  } else {
                    values.add(value.toString());
                  }
                }
                var insert =
                    String.format(
                        "INSERT INTO audit_log (%s, version) VALUES (%s, 0);",
                        String.join(", ", select), String.join(", ", values));
                writer.write(insert);
                writer.newLine();
                count.incrementAndGet();
              }
            }

            if (count.get() == 0) {
              Files.delete(filePath);
              log.info("No audit logs to export for tenant '{}'", tenantId);
            } else {
              log.info(
                  "Exported {} audit logs for tenant '{}' to file: {}",
                  count.get(),
                  tenantId,
                  filePath);
            }

          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

    return count.get();
  }
}
