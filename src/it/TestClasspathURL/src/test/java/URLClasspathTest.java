/*
 * @formatter:off
 * Copyright © 2019 admin (admin@infrastructurebuilder.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * @formatter:on
 */
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class URLClasspathTest {

  private static final String MYFILE_XML = "b.xml";
  private static final String CSUMVAL = "5cd814bd44716a73c2e380c443f573aa3f0a4aaf881f70810ec9c9552035433f6cca4c64161ce460e97db0089eefb1aad4d09e7151c330afc7a4caf528a6f475";
  private final static Logger log = LoggerFactory.getLogger(URLClasspathTest.class);

  @BeforeEach
  void setUp() throws Exception {
  }

  @AfterEach
  void tearDown() throws Exception {
  }

  @Test
  void testClasspath() throws Throwable {
    URL u = new URL("classpath:b.xml");
    URLConnection k = u.openConnection();
    try (InputStream ins = k.getInputStream()) {
    }

  }

}
