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
package org.infrastructurebuilder.util.core.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClasspathFileSystemTest {

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
  }

  private FileSystem fs;

  @BeforeEach
  void setUp() throws Exception {
    URI u = URI.create("classpath:/");
    fs = FileSystems.newFileSystem(u, new HashMap<>());
  }

  @Test
  void testGetPathStringStringArray() throws IOException {
    Path p = fs.getPath("ABC.txt");
    String s = Files.readString(p);
    assertEquals("Hi there!", s);
  }

  @Test
  void testStrings2() throws IOException {
    Path p = fs.getPath("testFolder/testfile.xml");
    String s = Files.readString(p);
    assertEquals(3939, s.length());

  }

  @AfterEach
  void clean() throws Exception {
    fs.close();
  }

}
