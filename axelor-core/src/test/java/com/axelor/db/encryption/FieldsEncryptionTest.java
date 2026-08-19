/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.encryption;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.file.temp.TempFiles;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.service.MetaService;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Request;
import com.axelor.rpc.Resource;
import com.axelor.rpc.Response;
import com.axelor.test.db.SecureEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/** Verifies that JPA field encryption works correctly for normal read/write operations: */
public class FieldsEncryptionTest extends BaseEncryptionTest {

  @Inject private Resource<SecureEntity> resource;

  @Inject private MetaService metaService;

  @Inject private ObjectMapper objectMapper;

  private static Long savedEntityId = 0L;

  @BeforeAll
  public static void beforeAll() {
    if (Query.of(SecureEntity.class).count() == 0) {
      JPA.runInTransaction(
          () -> {
            SecureEntity entity = new SecureEntity();
            entity.setMyString("PlainText");
            entity.setMySecureString("EncodedText");
            entity.setMyEncryptedString("EncodedOnlyText");
            entity.setMyBinary(("PlainByte").getBytes(StandardCharsets.UTF_8));
            entity.setMySecureBinary(("EncodedByte").getBytes(StandardCharsets.UTF_8));

            SecureEntity parent = new SecureEntity();
            parent.setCode("ParentSecretCode");
            parent.setMyString("ParentPlainText");
            parent.setMySecureString("ParentSecretString");
            parent.setMyEncryptedString("ParentEncryptedOnly");
            entity.setParent(parent);

            JPA.persist(entity);
            savedEntityId = entity.getId();
          });
    }
    savedEntityId = Query.of(SecureEntity.class).fetchOne().getId();
  }

  @SuppressWarnings("rawtypes")
  @Test
  @Order(1)
  public void dataInDatabaseShouldBeEncoded() {
    List result =
        JPA.em()
            .createNativeQuery(
                "SELECT my_string, my_secure_string, my_binary, my_secure_binary"
                    + " FROM secure_entity WHERE id = "
                    + savedEntityId)
            .getResultList();

    Object[] data = (Object[]) result.getFirst();

    assertEquals("PlainText", String.valueOf(data[0]));
    assertTrue(getStringEncryptor().isEncrypted(data[1].toString()));
    assertEquals("EncodedText", getStringEncryptor().decrypt(data[1].toString()));

    assertEquals("PlainByte", new String((byte[]) data[2], StandardCharsets.UTF_8));
    assertTrue(getBytesEncryptor().isEncrypted((byte[]) data[3]));
    assertArrayEquals(
        ("EncodedByte").getBytes(StandardCharsets.UTF_8),
        getBytesEncryptor().decrypt((byte[]) data[3]));
  }

  @Test
  @Order(2)
  public void nullEncryptedFieldsShouldStayNull() {
    final long[] savedId = {0L};

    inTransaction(
        () -> {
          SecureEntity entity = new SecureEntity();
          entity.setMySecureString(null);
          entity.setMySecureBinary(null);
          JPA.save(entity);
          savedId[0] = entity.getId();
        });

    inTransaction(
        () -> {
          SecureEntity entity = JPA.em().find(SecureEntity.class, savedId[0]);
          assertNotNull(entity);
          assertNull(entity.getMySecureString());
          assertNull(entity.getMySecureBinary());
        });
  }

  @Test
  @Order(3)
  public void reEncryptedFieldsShouldNotBeDoubleEncrypted() {
    final long[] savedId = {0L};
    final String plainText = "ToBeReEncrypted";
    final byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);

    inTransaction(
        () -> {
          SecureEntity entity = new SecureEntity();
          entity.setMySecureString(plainText);
          entity.setMySecureBinary(plainBytes);
          JPA.save(entity);
          savedId[0] = entity.getId();
        });

    // Read and re-save — simulates an update cycle
    inTransaction(
        () -> {
          SecureEntity entity = JPA.em().find(SecureEntity.class, savedId[0]);
          entity.setMyString("Hello");
          JPA.save(entity);
        });

    // Values should still be the original plain text, not double-encrypted
    inTransaction(
        () -> {
          SecureEntity entity = JPA.em().find(SecureEntity.class, savedId[0]);
          assertEquals(plainText, entity.getMySecureString());
          assertArrayEquals(plainBytes, entity.getMySecureBinary());
        });
  }

  @Test
  @Order(4)
  public void shouldDecodeEncodedData() {
    SecureEntity entity = JPA.em().find(SecureEntity.class, savedEntityId);

    assertEquals("PlainText", entity.getMyString());
    assertEquals("EncodedText", entity.getMySecureString());
    assertArrayEquals("PlainByte".getBytes(StandardCharsets.UTF_8), entity.getMyBinary());
    assertArrayEquals("EncodedByte".getBytes(StandardCharsets.UTF_8), entity.getMySecureBinary());
  }

  @Test
  @Order(5)
  @SuppressWarnings("unchecked")
  public void passwordFieldShouldNotBeExposedBySearchButEncryptedOnlyFieldShould() {
    Request request = new Request();
    request.setFields(List.of("id", "mySecureString", "myEncryptedString"));

    Response fetchResponse = resource.fetch(savedEntityId, request);
    Map<String, Object> fetched = (Map<String, Object>) fetchResponse.getItem(0);
    assertFalse(fetched.containsKey("mySecureString"));
    assertEquals("EncodedOnlyText", fetched.get("myEncryptedString"));

    Response response = resource.search(request);
    Map<String, Object> result =
        ((List<Map<String, Object>>) (List<?>) response.getData())
            .stream()
                .filter(item -> savedEntityId.equals(item.get("id")))
                .findFirst()
                .orElseThrow();

    assertFalse(result.containsKey("mySecureString"));
    assertEquals("EncodedOnlyText", result.get("myEncryptedString"));
  }

  @Test
  @Order(6)
  public void passwordFieldShouldNotBeExposedByActionResponseButEncryptedOnlyFieldShould()
      throws Exception {
    ActionResponse response = new ActionResponse();
    response.setValues(JPA.em().find(SecureEntity.class, savedEntityId));

    String modelResponse = objectMapper.writeValueAsString(response);
    assertFalse(modelResponse.contains("EncodedText"));
    assertTrue(modelResponse.contains("EncodedOnlyText"));
  }

  @Test
  @Order(7)
  @SuppressWarnings("unchecked")
  public void passwordFieldShouldNotBeExposedBySaveButEncryptedOnlyFieldShould() {
    Request request = new Request();
    request.setData(
        Map.of(
            "mySecureString",
            "SavePlaintext",
            "myEncryptedString",
            "SaveEncryptedOnlyPlaintext",
            "myString",
            "RegularValue"));

    Response response = resource.save(request);
    Map<String, Object> result = (Map<String, Object>) response.getItem(0);

    assertFalse(result.containsKey("mySecureString"));
    assertEquals("SaveEncryptedOnlyPlaintext", result.get("myEncryptedString"));
  }

  @Test
  @Order(8)
  @SuppressWarnings("unchecked")
  public void passwordFieldShouldNotBeExposedByExportButEncryptedOnlyFieldShould()
      throws Exception {
    Request request = new Request();
    request.setFields(List.of("myString", "mySecureString", "myEncryptedString"));

    Response response = resource.export(request, StandardCharsets.UTF_8);
    Map<String, Object> data = (Map<String, Object>) response.getData();
    String fileName = (String) data.get("fileName");
    String csv = Files.readString(TempFiles.findTempFile(fileName), StandardCharsets.UTF_8);

    assertFalse(csv.contains("EncodedText"));
    assertTrue(csv.contains("EncodedOnlyText"));
  }

  @Test
  @Order(9)
  public void passwordCodeFieldShouldNotBeExposedByCompactSerialization() {
    final long[] savedId = {0L};

    inTransaction(
        () -> {
          SecureEntity entity = new SecureEntity();
          entity.setCode("SecretCode");
          JPA.save(entity);
          savedId[0] = entity.getId();
        });

    Map<String, Object> map = Resource.toMapCompact(JPA.em().find(SecureEntity.class, savedId[0]));

    assertFalse(map.containsKey("code"));
  }

  @Test
  @Order(10)
  @SuppressWarnings("unchecked")
  public void passwordFieldShouldNotBeExposedByRecordName() {
    for (String name : List.of("mySecureString", "createdBy.password")) {
      Map<String, Object> data = new HashMap<>();
      data.put("id", savedEntityId);
      data.put("mySecureString", "RequestSecret");
      data.put("createdBy", Map.of("password", "RelatedRequestSecret"));

      Request request = new Request();
      request.setData(data);
      request.setFields(List.of(name));

      Response response = resource.getRecordName(request);
      Map<String, Object> result = (Map<String, Object>) response.getItem(0);

      assertFalse(result.containsKey("mySecureString"));
      assertFalse(((Map<String, Object>) result.get("createdBy")).containsKey("password"));
    }
  }

  @Test
  @Order(11)
  @SuppressWarnings("unchecked")
  void passwordFieldShouldNotBeExposedByMetaServiceRunSearch() {
    inTransaction(
        () -> {
          MetaView view = new MetaView("secure-search");
          view.setTitle("Test");
          view.setType("search");
          view.setXml(
              """
              <search title="Test" name="secure-search" limit="10">
                <search-fields>
                  <field name="str" type="string"/>
                </search-fields>
                <result-fields>
                  <field name="secureString" type="string"/>
                  <field name="entityCode" type="string"/>
                  <field name="encryptedString" type="string"/>
                  <field name="parentEntity" type="reference"/>
                </result-fields>
                <select model="com.axelor.test.db.SecureEntity">
                  <field name="mySecureString" as="secureString"/>
                  <field name="code" as="entityCode"/>
                  <field name="myEncryptedString" as="encryptedString"/>
                  <field name="parent" as="parentEntity"/>
                  <where match="all">
                    <input name="str" field="myString"/>
                  </where>
                </select>
              </search>
              """);
          JPA.save(view);
        });

    Request request = new Request();
    request.setData(Map.of("__name", "secure-search", "str", "PlainText"));

    Response response = metaService.runSearch(request);
    Map<String, Object> result =
        ((List<Map<String, Object>>) (List<?>) response.getData())
            .stream()
                .filter(item -> savedEntityId.equals(item.get("id")))
                .findFirst()
                .orElseThrow();

    assertEquals(
        "EncodedOnlyText", result.get("encryptedString"), "Encrypted-only field should be exposed");
    assertFalse(
        result.containsKey("secureString"), "Encrypted password field should not be exposed");
    assertFalse(result.containsKey("entityCode"), "Password field should not be exposed");

    assertNotNull(result.get("parentEntity"), "Parent relational entity should be returned");
    assertTrue(result.get("parentEntity") instanceof Map, "Parent should be a nested map");
    Map<String, Object> parentMap = (Map<String, Object>) result.get("parentEntity");
    assertFalse(
        parentMap.containsKey("mySecureString"),
        "Nested encrypted password field in relational entity should not be exposed");
    assertFalse(
        parentMap.containsKey("code"),
        "Nested password field in relational entity should not be exposed");
  }
}
