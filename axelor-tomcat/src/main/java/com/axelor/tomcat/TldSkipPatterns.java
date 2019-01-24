/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.tomcat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

public final class TldSkipPatterns {

  private static final Set<String> PATTERNS;

  static {
    // Same as Tomcat
    Set<String> patterns = new LinkedHashSet<>();
    patterns.add("ant-*.jar");
    patterns.add("aspectj*.jar");
    patterns.add("commons-beanutils*.jar");
    patterns.add("commons-codec*.jar");
    patterns.add("commons-collections*.jar");
    patterns.add("commons-dbcp*.jar");
    patterns.add("commons-digester*.jar");
    patterns.add("commons-fileupload*.jar");
    patterns.add("commons-httpclient*.jar");
    patterns.add("commons-io*.jar");
    patterns.add("commons-lang*.jar");
    patterns.add("commons-logging*.jar");
    patterns.add("commons-math*.jar");
    patterns.add("commons-pool*.jar");
    patterns.add("geronimo-spec-jaxrpc*.jar");
    patterns.add("h2*.jar");
    patterns.add("hamcrest*.jar");
    patterns.add("hibernate*.jar");
    patterns.add("jmx*.jar");
    patterns.add("jmx-tools-*.jar");
    patterns.add("jta*.jar");
    patterns.add("junit-*.jar");
    patterns.add("httpclient*.jar");
    patterns.add("log4j-*.jar");
    patterns.add("mail*.jar");
    patterns.add("org.hamcrest*.jar");
    patterns.add("slf4j*.jar");
    patterns.add("tomcat-embed-core-*.jar");
    patterns.add("tomcat-embed-logging-*.jar");
    patterns.add("tomcat-jdbc-*.jar");
    patterns.add("tomcat-juli-*.jar");
    patterns.add("tools.jar");
    patterns.add("wsdl4j*.jar");
    patterns.add("xercesImpl-*.jar");
    patterns.add("xmlParserAPIs-*.jar");
    patterns.add("xml-apis-*.jar");

    // Additional typical for Axelor applications
    patterns.add("activation-*.jar");
    patterns.add("animal-sniffer-annotations-*.jar");
    patterns.add("antlr-*.jar");
    patterns.add("antlr-runtime-*.jar");
    patterns.add("aopalliance-*.jar");
    patterns.add("apache-mime4j-*.jar");
    patterns.add("axelor-*.jar");
    patterns.add("backport-util-concurrent-*.jar");
    patterns.add("bcmail-*.jar");
    patterns.add("bcprov-*.jar");
    patterns.add("bctsp-*.jar");
    patterns.add("boilerpipe-*.jar");
    patterns.add("byte-buddy-*.jar");
    patterns.add("cache-api-*.jar");
    patterns.add("cas-client-core-*.jar");
    patterns.add("classmate-*.jar");
    patterns.add("com.ibm.icu-*.jar");
    patterns.add("com.lowagie.text-*.jar");
    patterns.add("commons-*.jar");
    patterns.add("core-*.jar");
    patterns.add("dom4j-*.jar");
    patterns.add("ebics-*.jar");
    patterns.add("ecj-*.jar");
    patterns.add("ehcache-*.jar");
    patterns.add("error_prone_annotations-*.jar");
    patterns.add("flute-*.jar");
    patterns.add("flyway-core-*.jar");
    patterns.add("fontbox-*.jar");
    patterns.add("groovy-*.jar");
    patterns.add("gson-*.jar");
    patterns.add("guava-*.jar");
    patterns.add("guice-*.jar");
    patterns.add("hamcrest-core-*.jar");
    patterns.add("hibernate-*.jar");
    patterns.add("HikariCP-*.jar");
    patterns.add("hotswap-*.jar");
    patterns.add("hsqldb-*.jar");
    patterns.add("httpclient-*.jar");
    patterns.add("httpcore-*.jar");
    patterns.add("httpmime-*.jar");
    patterns.add("iban4j-*.jar");
    patterns.add("ical4j-*.jar");
    patterns.add("infinispan-*.jar");
    patterns.add("itextpdf-*.jar");
    patterns.add("j2objc-annotations-*.jar");
    patterns.add("jackrabbit-webdav-*.jar");
    patterns.add("jackson-*.jar");
    patterns.add("jandex-*.jar");
    patterns.add("jansi-*.jar");
    patterns.add("javassist-*.jar");
    patterns.add("javax.inject-*.jar");
    patterns.add("javax.json-*.jar");
    patterns.add("javax.mail-*.jar");
    patterns.add("javax.servlet-api-*.jar");
    patterns.add("javax.wsdl-*.jar");
    patterns.add("javax.xml.stream-*.jar");
    patterns.add("jaxb-*.jar");
    patterns.add("jboss-annotations-api_1.2_spec-*.jar");
    patterns.add("jboss-jaxrs-api_2.0_spec-*.jar");
    patterns.add("jboss-logging-*.jar");
    patterns.add("jboss-marshalling-osgi-*.jar");
    patterns.add("jboss-transaction-api_*.jar");
    patterns.add("jcip-annotations-*.jar");
    patterns.add("jcl-over-slf4j-*.jar");
    patterns.add("jcommander-*.jar");
    patterns.add("jdom-*.jar");
    patterns.add("jempbox-*.jar");
    patterns.add("jgroups-*.jar");
    patterns.add("jsoup-*.jar");
    patterns.add("jsr305-*.jar");
    patterns.add("jul-to-slf4j-*.jar");
    patterns.add("junit-*.jar");
    patterns.add("juniversalchardet-*.jar");
    patterns.add("log4j-over-slf4j-*.jar");
    patterns.add("logback-classic-*.jar");
    patterns.add("logback-core-*.jar");
    patterns.add("lucene-*.jar");
    patterns.add("lutung-*.jar");
    patterns.add("metadata-extractor-*.jar");
    patterns.add("mysql-connector-java-*.jar");
    patterns.add("opencsv-*.jar");
    patterns.add("opensaml-*.jar");
    patterns.add("org.apache.batik.*.jar");
    patterns.add("org.apache.commons.codec-*.jar");
    patterns.add("org.apache.xerces-*.jar");
    patterns.add("org.apache.xml.*.jar");
    patterns.add("org.eclipse.*.jar");
    patterns.add("org.mozilla.javascript-*.jar");
    patterns.add("org.w3c.css.sac-*.jar");
    patterns.add("org.w3c.dom.smil-*.jar");
    patterns.add("org.w3c.dom.svg-*.jar");
    patterns.add("pdfbox-*.jar");
    patterns.add("poi-*.jar");
    patterns.add("poi-ooxml-*.jar");
    patterns.add("poi-ooxml-schemas-*.jar");
    patterns.add("poi-scratchpad-*.jar");
    patterns.add("postgresql-*.jar");
    patterns.add("quartz-*.jar");
    patterns.add("resteasy-*.jar");
    patterns.add("rome-*.jar");
    patterns.add("serializer-*.jar");
    patterns.add("shiro-cas-*.jar");
    patterns.add("shiro-core-*.jar");
    patterns.add("shiro-guice-*.jar");
    patterns.add("slf4j-api-*.jar");
    patterns.add("snakeyaml-*.jar");
    patterns.add("snakeyaml-*.jar");
    patterns.add("ST4-*.jar");
    patterns.add("stax-api-*.jar");
    patterns.add("tagsoup-*.jar");
    patterns.add("Tidy-*.jar");
    patterns.add("tika-*.jar");
    patterns.add("tomcat-embed-core-*.jar");
    patterns.add("tomcat-embed-el-*.jar");
    patterns.add("tomcat-embed-jasper-*.jar");
    patterns.add("tomcat-embed-logging-log4j-*.jar");
    patterns.add("validation-api-*.jar");
    patterns.add("xalan-*.jar");
    patterns.add("xml-apis-*.jar");
    patterns.add("xmlbeans-*.jar");
    patterns.add("xmlpull-*.jar");
    patterns.add("xmlsec-*.jar");
    patterns.add("xmpcore-*.jar");
    patterns.add("xpp3_min-*.jar");
    patterns.add("xstream-*.jar");
    patterns.add("xz-*.jar");

    PATTERNS = Collections.unmodifiableSet(patterns);
  }

  static void apply(JarScanner jarScanner) {
    final StandardJarScanFilter jarFilter = new StandardJarScanFilter();
    jarFilter.setTldSkip(String.join(",", PATTERNS));
    jarScanner.setJarScanFilter(jarFilter);
  }
}
