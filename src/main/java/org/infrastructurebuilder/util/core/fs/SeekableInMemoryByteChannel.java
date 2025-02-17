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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeekableInMemoryByteChannel implements SeekableByteChannel {

  private static final int NAIVE_RESIZE_LIMIT = Integer.MAX_VALUE >> 1;
  public static final byte[] EMPTY_BYTE_ARRAY = {};

  private byte[] data;
  private final AtomicBoolean closed = new AtomicBoolean();
  private int position, size;

  /**
   * Parameterless constructor - allocates internal buffer by itself.
   */
  public SeekableInMemoryByteChannel() {
    this(EMPTY_BYTE_ARRAY);
  }

  /**
   * Constructor taking a byte array.
   *
   * <p>
   * This constructor is intended to be used with pre-allocated buffer or when reading from a given byte array.
   * </p>
   *
   * @param data input data or pre-allocated array.
   */
  public SeekableInMemoryByteChannel(final byte[] data) {
    this.data = data;
    size = data.length;
  }

  /**
   * Constructor taking a size of storage to be allocated.
   *
   * <p>
   * Creates a channel and allocates internal storage of a given size.
   * </p>
   *
   * @param size size of internal buffer to allocate, in bytes.
   */
  public SeekableInMemoryByteChannel(final int size) {
    this(new byte[size]);
  }

  /**
   * Obtains the array backing this channel.
   *
   * <p>
   * NOTE: The returned buffer is not aligned with containing data, use {@link #size()} to obtain the size of data
   * stored in the buffer.
   * </p>
   *
   * @return internal byte array.
   */
  public byte[] array() {
    return data;
  }

  @Override
  public void close() {
    closed.set(true);
  }

  private void ensureOpen() throws ClosedChannelException {
    if (!isOpen()) {
      throw new ClosedChannelException();
    }
  }

  @Override
  public boolean isOpen() {
    return !closed.get();
  }

  /**
   * Returns this channel's position.
   *
   * <p>
   * This method violates the contract of {@link SeekableByteChannel#position()} as it will not throw any exception when
   * invoked on a closed channel. Instead it will return the position the channel had when close has been called.
   * </p>
   */
  @Override
  public long position() {
    return position;
  }

  @Override
  public SeekableByteChannel position(final long newPosition) throws IOException {
    ensureOpen();
    if (newPosition < 0L || newPosition > Integer.MAX_VALUE) {
      throw new IOException("Position has to be in range 0.. " + Integer.MAX_VALUE);
    }
    position = (int) newPosition;
    return this;
  }

  @Override
  public int read(final ByteBuffer buf) throws IOException {
    ensureOpen();
    int wanted = buf.remaining();
    final int possible = size - position;
    if (possible <= 0) {
      return -1;
    }
    if (wanted > possible) {
      wanted = possible;
    }
    buf.put(data, position, wanted);
    position += wanted;
    return wanted;
  }

  private void resize(final int newLength) {
    int len = data.length;
    if (len <= 0) {
      len = 1;
    }
    if (newLength < NAIVE_RESIZE_LIMIT) {
      while (len < newLength) {
        len <<= 1;
      }
    } else { // avoid overflow
      len = newLength;
    }
    data = Arrays.copyOf(data, len);
  }

  /**
   * Returns the current size of entity to which this channel is connected.
   *
   * <p>
   * This method violates the contract of {@link SeekableByteChannel#size} as it will not throw any exception when
   * invoked on a closed channel. Instead it will return the size the channel had when close has been called.
   * </p>
   */
  @Override
  public long size() {
    return size;
  }

  /**
   * Truncates the entity, to which this channel is connected, to the given size.
   *
   * <p>
   * This method violates the contract of {@link SeekableByteChannel#truncate} as it will not throw any exception when
   * invoked on a closed channel.
   * </p>
   *
   * @throws IllegalArgumentException if size is negative or bigger than the maximum of a Java integer
   */
  @Override
  public SeekableByteChannel truncate(final long newSize) {
    if (newSize < 0L || newSize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Size has to be in range 0.. " + Integer.MAX_VALUE);
    }
    if (size > newSize) {
      size = (int) newSize;
    }
    if (position > newSize) {
      position = (int) newSize;
    }
    return this;
  }

  @Override
  public int write(final ByteBuffer b) throws IOException {
    ensureOpen();
    int wanted = b.remaining();
    final int possibleWithoutResize = size - position;
    if (wanted > possibleWithoutResize) {
      final int newSize = position + wanted;
      if (newSize < 0) { // overflow
        resize(Integer.MAX_VALUE);
        wanted = Integer.MAX_VALUE - position;
      } else {
        resize(newSize);
      }
    }
    b.get(data, position, wanted);
    position += wanted;
    if (size < position) {
      size = position;
    }
    return wanted;
  }

}
