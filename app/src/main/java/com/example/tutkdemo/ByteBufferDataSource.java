/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.tutkdemo;

import static java.lang.Math.min;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSourceException;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public final class ByteBufferDataSource extends BaseDataSource {

  private final ByteArrayOutputStream data;

  @Nullable private Uri uri;
  private int readPosition;
  private int bytesRemaining;
  private boolean opened;

  /** @param data The data to be read. */
  public ByteBufferDataSource(ByteArrayOutputStream data) {
    super(/* isNetwork= */ false);
    Assertions.checkNotNull(data);
    this.data = data;
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    uri = dataSpec.uri;
    transferInitializing(dataSpec);
    if (dataSpec.position > data.size()) {
      throw new DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE);
    }
    readPosition = (int) dataSpec.position;
    bytesRemaining = data.toByteArray().length - (int) dataSpec.position;
    if (dataSpec.length != C.LENGTH_UNSET) {
      bytesRemaining = (int) min(bytesRemaining, dataSpec.length);
    }
    opened = true;
    transferStarted(dataSpec);
    return C.LENGTH_UNSET;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) {
    if (length == 0) {
      return 0;
    } else if (bytesRemaining == 0) {
      return C.RESULT_END_OF_INPUT;
    }

    length = min(length, bytesRemaining);
    System.arraycopy(data.toByteArray(), readPosition, buffer, offset, length);
    readPosition += length;
    bytesRemaining -= length;
    bytesTransferred(length);
    return length;
  }

  @Override
  @Nullable
  public Uri getUri() {
    return uri;
  }

  @Override
  public void close() {
    if (opened) {
      opened = false;
      transferEnded();
    }
    uri = null;
  }
}
