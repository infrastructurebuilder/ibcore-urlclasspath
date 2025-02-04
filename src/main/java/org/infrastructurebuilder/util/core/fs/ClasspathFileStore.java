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
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.infrastructurebuilder.exceptions.IBException;

import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ResourceList.ResourceFilter;
import io.github.classgraph.ScanResult;

public class ClasspathFileStore extends FileStore {

  private final static FileSystem fs = FileSystems.getDefault();
  private final ClasspathConfig config;
  private ScanResult scan;
  private int hash = Integer.MIN_VALUE;
  private ResourceList resourceList;
  private int maxBufferSize;
  private final ClasspathFileSystem cpfs;

  public ClasspathFileStore(ClasspathFileSystem fs, ClasspathConfig config) {
    this.config = config;
    this.cpfs = fs;
  }

  @Override
  public String name() {
    return "classpath-filestore-%05d".formatted(config.hashCode());
  }

  public ScanResult getScan() {
    if (this.scan == null) {
      this.scan = config.scan();
      this.hash = this.scan.getClasspath().hashCode();
    }
    return this.scan;
  }

  public ResourceList getResourceList() {
    if (this.resourceList == null) {
      this.resourceList = getScan().getAllResources();
    }
    return this.resourceList;

  }

  @Override
  public String type() {
    return "classpath";
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public long getTotalSpace() throws IOException {
    return 0;
  }

  @Override
  public long getUsableSpace() throws IOException {
    return 0;
  }

  @Override
  public long getUnallocatedSpace() throws IOException {
    return 0;
  }

  @Override
  public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
    return false;
  }

  @Override
  public boolean supportsFileAttributeView(String name) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object getAttribute(String attribute) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int hashCode() {
    if (this.scan == null)
      this.hash = getScan().getClasspath().hashCode();
    return this.hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ClasspathFileStore other = (ClasspathFileStore) obj;
    return Objects.equals(getScan().getClasspath(), other.getScan().getClasspath());
  }

  void selfDestruct() {
    this.scan = null;
    this.resourceList = null;
  }

  public ResourceList getResourceForPath(String string) {
    return getScan().getResourcesWithPath(string);
  }

  public SeekableByteChannel getSeekableByteChannelForPath(String path) throws IOException {
    ResourceList r = getResourceForPath(path);
    if (r.size() < 1)
      throw new IOException("No resource found matching %s".formatted(path.toString()));
    Resource res = r.get(0);

    long l = res.getLength();
    SeekableByteChannel sb = null;
    if (l <= this.maxBufferSize) {
      sb = new SeekableInMemoryByteChannel((int) l);
      try (ReadableByteChannel inc = Channels.newChannel(res.open())) {
        ByteBuffer buf = ByteBuffer.allocate((int) l);
        while (inc.read(buf) != -1) {
          buf.flip();
          sb.write(buf);
          buf.compact();
        }
        buf.flip();
        while (buf.hasRemaining())
          sb.write(buf);
      }
    } else {
      try {
        sb = new InputStreamReadOnlySeekableByteChannel(() -> IBException.cet.returns(() -> res.open()),
            res.getLength(), maxBufferSize);
      } catch (IBException e) {
        // Cheating just a little
        throw (IBException) e.getCause();
      }
    }
    return sb;
  }

  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) {
    String header = dir.toString();
    ResourceFilter rf = new ResourceFilter() {
      @Override
      public boolean accept(Resource resource) {
        return resource.getPath().startsWith(header);
      }

    };
    return new DirectoryStream<Path>() {

      @Override
      public void close() throws IOException {
      }

      @Override
      public Iterator<Path> iterator() {
        return (Iterator<Path>) (getResourceForPath(dir.toString()).filter(rf).stream()
            .map(res -> (Path) new ClasspathPath(cpfs, res)).toList().iterator());
      }
    };
  }

}
