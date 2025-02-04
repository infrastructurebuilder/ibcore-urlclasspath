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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

import com.google.common.base.Supplier;

import io.github.classgraph.Resource;

/**
 * A class that fakes a SeekableByteChannel by wrapping an easy-to-reopen element
 */
public class InputStreamReadOnlySeekableByteChannel implements SeekableByteChannel {

  private final Supplier<InputStream> res;
  private boolean open = true;
  private transient ReadableByteChannel ins = null;
  private long position = 0L;
  private long current = 0L;
  private final int bufferSize;
  private final ByteBuffer buf;
  private final long length;

  /**
   * Open or creates a file, returning a seekable byte channel
   *
   * @param res              the path open or create
   * @param options          options specifying how the file is opened
   * @param tempFileRequired true if a temp file wanted, false in case of a in-memory solution option.
   * @throws IOException if an I/O error occurs
   */
  public InputStreamReadOnlySeekableByteChannel(final Supplier<InputStream> resource, long len, int bufferSize) throws IOException {
    this.res = resource;
    this.bufferSize = bufferSize;
    this.length = len;
    this.buf = ByteBuffer.allocate(bufferSize);
    reset();
  }

  private void reset() throws IOException {
    if (isOpen()) {
      if (this.ins != null)
        this.ins.close(); // Dunno if it closes underlying inputstream
      this.ins = Channels.newChannel(res.get());
      this.current = 0L;
    }
  }

  // Slow AF
  private void advanceTo(long p) throws IOException {
    if (p == 0 && this.current != 0) {
      reset();
    } else if (p >= this.current) {
      skipRead(p - current);
    } else {
      reset();
      skipRead(p);
    }
  }

  private void skipRead(long p) throws IOException {
    long count = p;
    while (count > 0) {
      for (int i = 0; i < (p / this.bufferSize); ++i) {
        int read = this.ins.read(buf);
        if (read == -1)
          throw new IOException("Error.  Ran out of bytes.");
        this.current += read;
        count -= read;
        buf.clear();
      }
      ByteBuffer one = ByteBuffer.allocate((int) count);
      int read = this.ins.read(one);
      if (read == -1)
        throw new IOException("Error. Ran out of bytes.");
      this.current += read;
      count -= read;
      one.clear();
    }
  }

  @Override
  public boolean isOpen() {
    return this.open;
  }

  @Override
  public void close() throws IOException {
    try {
      if (!isOpen())
        return;
    } finally {
      if (this.ins != null)
        this.ins.close();
      this.ins = null;
    }
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    throw new UnsupportedOperationException("No modification allowed");
  }

  @Override
  public SeekableByteChannel truncate(long size) throws IOException {
    throw new UnsupportedOperationException("No modification allowed");
  }

  @Override
  public long size() throws IOException {
    return this.length;
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    return 0;
  }

  @Override
  public SeekableByteChannel position(long newPosition) throws IOException {
    if (newPosition < 0 || newPosition > size())
      throw new IllegalArgumentException("Illegal new position %d".formatted(newPosition));
    this.position = newPosition;
    advanceTo(this.position);
    return this;
  }

  @Override
  public long position() throws IOException {
    return this.position;
  }

}
