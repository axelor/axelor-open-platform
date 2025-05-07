/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.file;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.axelor.JpaTestModule;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.file.store.FileStoreFactory;
import com.axelor.file.store.Store;
import com.axelor.file.store.s3.S3Store;
import com.axelor.test.GuiceModules;
import io.minio.messages.Bucket;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@GuiceModules(S3StoreTest.S3FileStoreTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S3StoreTest extends AbstractBaseFile {

  private static final String BUCKET_NAME = "my-bucket";
  private static final String ENDPOINT = "127.0.0.1:9000";

  @BeforeAll
  public static void checkMinioAvailability() {
    assumeTrue(isServerRunning(), "Object storage server is not running on " + ENDPOINT);
  }

  private static boolean isServerRunning() {
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("http://%s/minio/health/live".formatted(ENDPOINT)))
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  public static class S3FileStoreTestModule extends JpaTestModule {
    @Override
    protected void configure() {
      resetAllSettings();
      Map<String, String> props = AppSettings.get().getInternalProperties();
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_ENABLED, "true");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_ENDPOINT, ENDPOINT);
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_PATH_STYLE, "true");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_SECURE, "false");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_ACCESS_KEY, "root");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_SECRET_KEY, "password");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_BUCKET, BUCKET_NAME);
      super.configure();
    }
  }

  @Test
  @Order(1)
  void shouldCreateBucket() throws Exception {
    Store store = FileStoreFactory.getStore();

    S3Store s3Store = (S3Store) store;
    List<Bucket> buckets = s3Store.getClient().listBuckets();
    assertTrue(buckets.stream().anyMatch(e -> e.name().equals(BUCKET_NAME)));
  }

  private static class EndpointInfo {
    private final String host;
    private final int port;

    public EndpointInfo(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public static EndpointInfo parse(String endpoint) {
      int portSeparatorIdx = endpoint.lastIndexOf(':');
      int squareBracketIdx = endpoint.lastIndexOf(']');
      if (portSeparatorIdx > squareBracketIdx) {
        if (squareBracketIdx == -1 && endpoint.indexOf(':') != portSeparatorIdx) {
          throw new IllegalArgumentException();
        }
        var host = endpoint.substring(0, portSeparatorIdx);
        var port = Integer.parseInt(endpoint, portSeparatorIdx + 1, endpoint.length(), 10);
        return new EndpointInfo(host, port);
      } else {
        return new EndpointInfo(endpoint, 9000);
      }
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }
  }
}
