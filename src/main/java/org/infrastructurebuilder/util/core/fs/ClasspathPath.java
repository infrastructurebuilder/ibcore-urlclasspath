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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import io.github.classgraph.Resource;

public class ClasspathPath implements Path {

  private final ClasspathFileSystem fileSystem;
  private final String path;
  private final String sep;
  private final URL url;
  private final String[] parts;

  public ClasspathPath(ClasspathFileSystem fileSystem, Path path) {
    this(fileSystem, Objects.requireNonNull(path).toString());
  }

  ClasspathPath(ClasspathFileSystem fileSystem, Resource res) {
    this(fileSystem, Objects.requireNonNull(res).getPath().toString());
  }

  public ClasspathPath(ClasspathFileSystem fileSystem, String path) {
    this.fileSystem = requireNonNull(fileSystem);
    this.sep = fileSystem.getSeparator();
    this.path = requireNonNull(path).toString();
    this.parts = this.path.split(getFileSystem().getSeparator());
    try {
      this.url = new URL("classpath:" + path.toString());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    if (!(path.toString().equals(fileSystem.getSeparator()) && path.startsWith(fileSystem.getSeparator())))
      throw new UnsupportedOperationException("Classpath paths must be relative {}".formatted(path));
  }

  @Override
  public FileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public boolean isAbsolute() {
    return this.path.startsWith(fileSystem.getSeparator());
  }

  @Override
  public Path getRoot() {
    return getFileSystem().getRootDirectories().iterator().next();
  }

  @Override
  public Path getFileName() {
    if (this.parts.length < 1)
      throw new UnsupportedOperationException("WTF?");
    return new ClasspathPath(this.fileSystem, this.parts[this.parts.length - 2]);

  }

  @Override
  public Path getParent() {
    StringJoiner sb = new StringJoiner(getFileSystem().getSeparator());
    for (int i = 0; i < parts.length - 1; ++i)
      sb.add(parts[i]);
    return new ClasspathPath(this.fileSystem, sb.toString());
  }

  private List<String> getListOfNormalizedPaths() {
    ClasspathPath n = (ClasspathPath) this.normalize();
    String[] split = n.toString().split(sep);
    return List.of(split);
  }

  @Override
  public int getNameCount() {
    return getListOfNormalizedPaths().size();
  }

  @Override
  public Path getName(int index) {
    String[] split = this.path.split(sep);
    if (index < 0 || index >= split.length || split.length == 0)
      throw new IllegalArgumentException("Value %d invalid for %s".formatted(index, this.path));
    return Path.of(split[index]); // FIXME Makes a default FS path?
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    StringJoiner j = new StringJoiner(getFileSystem().getSeparator());
    List<String> sublist = getListOfNormalizedPaths().subList(beginIndex, endIndex);
    if (beginIndex < 0 || beginIndex > endIndex || endIndex > sublist.size())
      throw new IllegalArgumentException("[%d,%d] not valid for %s".formatted(beginIndex, endIndex, this.path));
    sublist.forEach(j::add);
    return Paths.get(j.toString() + getFileSystem().getSeparator()); // FIXME Not a PathRefPath
  }

  @Override
  public boolean startsWith(Path other) {
    return flatten(this).startsWith(flatten(other));
  }

  private String flatten(Path p) {
    String ps = p.toString();
    return ps.startsWith(sep) ? ps : sep + ps;
  }

  @Override
  public boolean endsWith(Path other) {
    String ps = other.toString();
    return this.path.endsWith(ps);
  }

  @Override
  public Path normalize() {
    Path p = Paths.get(this.path).normalize(); // Cheater...
    if (p.toString().equals(this.path))
      return this;
    return new ClasspathPath(this.fileSystem, p.toString());
  }

  @Override
  public Path resolve(Path other) {
    if (other.isAbsolute())
      return other;
    ClasspathPath o = (ClasspathPath) other;
    String[] parts = new String[this.parts.length + o.parts.length];
    int i = 0;
    for (; i < parts.length; ++i)
      parts[i] = this.parts[i];
    for (int j = 0; j < o.parts.length; ++j) {
      parts[i + j] = o.parts[j];
    }
    StringJoiner sj = new StringJoiner(getFileSystem().getSeparator());
    for (String s : parts)
      sj.add(s);
    return new ClasspathPath(this.fileSystem, sj.toString()).normalize();
  }

  @Override
  public Path relativize(Path other) {
    // More cheating
    String r = Paths.get(this.path).relativize(Paths.get(other.toString())).toString();
    if (r.equals(this.path))
      return this;
    return new ClasspathPath(this.fileSystem, r);
  }

  @Override
  public URI toUri() {
    throw new UnsupportedOperationException("toUri");
  }

  @Override
  public Path toAbsolutePath() {
    return this;
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    throw new UnsupportedOperationException("toRealPath");
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
    throw new UnsupportedOperationException("register");
  }

  @Override
  public int compareTo(Path other) {
    return this.path.compareTo(other.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileSystem, path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ClasspathPath other = (ClasspathPath) obj;
    return Objects.equals(fileSystem, other.fileSystem) && Objects.equals(path, other.path);
  }
}
