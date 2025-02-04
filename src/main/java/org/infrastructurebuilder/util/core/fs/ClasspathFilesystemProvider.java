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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import static  java.nio.file.StandardOpenOption.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;

public class ClasspathFilesystemProvider extends FileSystemProvider {

  public static final String CLASSPATH = "classpath";
  private final AtomicReference<ClasspathFileSystem> fs = new AtomicReference<>();

  @Override
  public String getScheme() {
    return CLASSPATH;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ClasspathFileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    if (fs.get() == null)
      synchronized (fs) {
        fs.set(new ClasspathFileSystem(this, new ClasspathConfig((Map<String, Object>) env)));
      }
    return fs.get();
  }

  @Override
  public FileSystem getFileSystem(URI uri) {
    return fs.get();
  }

  @Override
  public Path getPath(URI uri) {
    if (!uri.getScheme().equals(getScheme()))
      throw new IllegalArgumentException("Wrong scheme %s".formatted(uri));
    return getFileSystem(uri).getPath(uri.getPath());
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    // Kill your call if you try
    if (attrs != null && attrs.length > 0)
      throw new IllegalArgumentException("Only READ is allowed here %s".formatted(attrs));
    for (OpenOption opt: options)
      if (opt != READ)
        throw new IllegalArgumentException("Only READ is allowed here %s".formatted(options));
    options = (Set<? extends OpenOption>) List.of(READ);

    return getFileStore(path).getSeekableByteChannelForPath(path.toString());
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    return getFileStore(dir).newDirectoryStream(dir, filter);
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(Path path) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    // Technically, different provider for path2 could be "allowed"
    if (path.getFileSystem().equals(path2.getFileSystem()))
      return ((ClasspathPath) path).equals(path2);
    return false;
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    return false;
  }

  @Override
  public ClasspathFileStore getFileStore(Path path) throws IOException {
    return (ClasspathFileStore) ((ClasspathPath)path).getFileSystem().getFileStores().iterator().next();
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    // This is conceptually irrelevant for classpaths
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
  }

}
