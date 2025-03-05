package com.axelor.gradle.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

public class CentralPortalUploadTask extends DefaultTask {

  static final String CRLF = "\r\n";
  static final String SONATYPE_URL = "https://central.sonatype.com/api/v1/publisher/upload";

  @Input public String username;

  public String getUsername() {
    return username;
  }

  @Input public String password;

  public String getPassword() {
    return password;
  }

  @InputFile public Provider<RegularFile> archiveFile;

  public File getArchiveFile() {
    return archiveFile.get().getAsFile();
  }

  @Input @Optional public Boolean autoPublish = false;

  public Boolean isAutoPublish() {
    return autoPublish;
  }

  @Input @Optional public String publicationName;

  public String getPublicationName() {
    return publicationName;
  }

  @TaskAction
  public void publish() throws Exception {
    String boundary = new BigInteger(256, new SecureRandom()).toString();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(getUrl()))
            .header("Authorization", "Bearer " + getToken())
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(
                HttpRequest.BodyPublishers.ofByteArray(
                    getMultiPartBody(getArchiveFile(), boundary)))
            .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    int statusCode = response.statusCode();
    String body = response.body();

    if (statusCode != 201) {
      throw new Exception(
          "Failed to upload artifact to central portal. Response: " + response.body());
    } else {
      getLogger()
          .lifecycle("Upload to central portal successfully, deployment identifier is {}", body);
    }
  }

  private byte[] getMultiPartBody(File archiveFile, String boundary) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    write("--" + boundary + CRLF, out);
    write(
        "Content-Disposition: form-data; name=\"bundle\"; filename=\""
            + archiveFile.getName()
            + "\"",
        out);
    write(CRLF, out);
    write("Content-Type: application/octet-stream" + CRLF + CRLF, out);
    Files.copy(archiveFile.toPath(), out);
    write(CRLF + "--" + boundary + "--" + CRLF, out);

    return out.toByteArray();
  }

  private String getUrl() {
    Map<String, String> map = new HashMap<>();
    if (getPublicationName() != null && !getPublicationName().isEmpty()) {
      map.put("name", getPublicationName());
    }
    map.put("publicationType", isAutoPublish() ? "AUTOMATIC" : "USER_MANAGED");
    return String.format(
        "%s?%s",
        SONATYPE_URL,
        map.keySet().stream()
            .map(key -> key + "=" + map.get(key))
            .collect(Collectors.joining("&")));
  }

  private String getToken() {
    return Base64.getEncoder()
        .encodeToString(String.format("%s:%s", getUsername(), getPassword()).getBytes());
  }

  private void write(String string, OutputStream out) throws IOException {
    out.write(string.getBytes(StandardCharsets.UTF_8));
  }
}
