/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.JpaTestModule;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.file.store.FileStoreFactory;
import com.axelor.file.store.Store;
import com.axelor.file.store.s3.S3Store;
import com.axelor.test.GuiceModules;
import io.minio.messages.Bucket;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@GuiceModules(S3StoreTest.S3FileStoreTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S3StoreTest extends AbstractBaseFile {

  private static final String BUCKET_NAME = "my-bucket";

  public static class S3FileStoreTestModule extends JpaTestModule {
    @Override
    protected void configure() {
      resetAllSettings();
      Map<String, String> props = AppSettings.get().getInternalProperties();
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_ENABLED, "true");
      props.put(AvailableAppSettings.DATA_OBJECT_STORAGE_ENDPOINT, "127.0.0.1:9000");
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
  public void shouldCreateBucket() throws Exception {
    Store store = FileStoreFactory.getStore();

    S3Store s3Store = (S3Store) store;
    List<Bucket> buckets = s3Store.getClient().listBuckets();
    assertTrue(buckets.stream().anyMatch(e -> e.name().equals(BUCKET_NAME)));
  }
}
