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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import io.github.classgraph.ResourceList;
import io.github.classgraph.ResourceList.ResourceFilter;
import io.github.classgraph.ScanResult;

/**
 * The classpath filesystem is organized quite differently from "real" filesystems.
 *
 * The data is organized by groups, and directories within disparate eleements of the classpath mean that files
 * aggregate along "inside-the-zip-file" lines rather than traditional means.
 *
 * A full classpath scan is necessary to launch a classpath filesystem. This code currently uses
 *
 * ClassGraph to perform its scan. The ScanResult from the ClassGraph scan is used to navigate the "filesystem". Caching
 * the ScanResult has the potential to generate problems, but them's the breaks.
 */
public class ClasspathFileSystem extends FileSystem implements Comparable<ClasspathFileSystem> {
  private static final String GLOB_SYNTAX = "glob";
  private static final String REGEX_SYNTAX = "regex";
  private final transient ClassLoader classLoader;
  private final transient ClasspathFilesystemProvider provider;
  private final transient ClasspathPath root;
  private final ClasspathFileStore filestore;
  public final static Set<String> STANDARD_SUPPORTED_VIEWS;
  static {
    STANDARD_SUPPORTED_VIEWS = Set.of("basic", "posix", "user", "classpath");
  }

  public ClasspathFileSystem(ClasspathFilesystemProvider provider, ClasspathConfig config) {
    this.classLoader = Thread.currentThread().getContextClassLoader(); // FIXME
    this.provider = Objects.requireNonNull(provider);
    this.root = new ClasspathPath(this, Path.of("/"));
    this.filestore = new ClasspathFileStore(this, config);
  }

  @Override
  public int compareTo(ClasspathFileSystem o) {
    return classLoader.getName().compareTo(o.classLoader.getName());
  }

  @Override
  public FileSystemProvider provider() {
    return this.provider;
  }

  @Override
  public void close() throws IOException {
    this.filestore.selfDestruct();
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String getSeparator() {
    return "/";
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    return List.of(root);
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    return List.of(this.filestore);
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    return STANDARD_SUPPORTED_VIEWS;
  }

  @Override
  public Path getPath(String first, String... more) {
    Objects.requireNonNull(first);
    String path;
    if (more.length == 0) {
      path = first;
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(first);
      for (String segment : more) {
        if (!segment.isEmpty()) {
          if (sb.length() > 0)
            sb.append(getSeparator());
          sb.append(segment);
        }
      }
      path = sb.toString();
    }
    return new ClasspathPath(this, path);
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    int pos = syntaxAndPattern.indexOf(':');
    if (pos <= 0 || pos == syntaxAndPattern.length())
      throw new IllegalArgumentException();
    String syntax = syntaxAndPattern.substring(0, pos);
    String input = syntaxAndPattern.substring(pos + 1);

    String expr;
    if (syntax.equalsIgnoreCase(GLOB_SYNTAX)) {
      expr = Globs.toUnixRegexPattern(input);
    } else {
      if (syntax.equalsIgnoreCase(REGEX_SYNTAX)) {
        expr = input;
      } else {
        throw new UnsupportedOperationException("Syntax '" + syntax + "' not recognized");
      }
    }
    final Pattern pattern = Pattern.compile(expr);
    return new PathMatcher() {
      @Override
      public boolean matches(Path path) {
        return pattern.matcher(path.toString()).matches();
      }
    };
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    return FileSystems.getDefault().getUserPrincipalLookupService();
  }

  @Override
  public WatchService newWatchService() throws IOException {
    throw new UnsupportedOperationException("No WatchService Available");
  }

  @Override
  public int hashCode() {
    return this.filestore.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ClasspathFileSystem other = (ClasspathFileSystem) obj;
    return Objects.equals(filestore, other.filestore);
  }

}
