package com.example.tutkdemo;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamDataSource extends BaseDataSource {

    @Nullable private Uri uri;
    private InputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    public InputStreamDataSource(InputStream inputStream) {
        super(false);
        this.inputStream = inputStream;
    }

    @Override
    public long open(DataSpec dataspec)
    {
        try
        {
            uri = dataspec.uri;
            long skipped = inputStream.skip(dataspec.position);
            if (skipped < dataspec.position) {
                // assetManager.open() returns an AssetInputStream, whose skip() implementation only skips
                // fewer bytes than requested if the skip is beyond the end of the asset's data.
                throw new EOFException();
            }
            if (dataspec.length != C.LENGTH_UNSET) {
                bytesRemaining = dataspec.length - skipped;
            }
            else {
                bytesRemaining = C.LENGTH_UNSET;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        opened = true;
        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength)
    {
        if (bytesRemaining == 0) {
            return -1;
        }
        else {
            int bytesRead = 0;
            try {
                int bytesToRead = bytesRemaining == C.LENGTH_UNSET ? readLength
                        : (int) Math.min(bytesRemaining, readLength);
                bytesRead = inputStream.read(buffer, offset, bytesToRead);
            }
            catch (IOException e)
            {

            }

            if (bytesRead > 0) {
                if (bytesRemaining != C.LENGTH_UNSET) {
                    bytesRemaining -= bytesRead;
                }

            }
            return bytesRead;
        }
    }

    @Override
    @Nullable
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close()
    {
        if (inputStream != null) {
            try {
                inputStream.close();
            }
            catch (IOException e)
            {

            }
            finally
            {
                inputStream = null;
                if (opened) {
                    opened = false;
                }
            }
        }
    }
}
