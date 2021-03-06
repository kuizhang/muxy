/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.muxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.addthis.basis.util.Bytes;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;

public abstract class AbstractMuxFile implements MuxFile {

    protected int fileId;
    protected String fileName;
    protected long length;
    protected long lastModified;

    @Nonnull  protected final ArrayList<MuxStream> streams;
    @Nullable protected final MuxyEventListener eventListener;

    protected AbstractMuxFile(@Nullable MuxyEventListener eventListener) {
        this.eventListener = eventListener;
        this.streams = new ArrayList<>();
    }

    protected AbstractMuxFile(ReadMuxFileDirectory dir, InputStream in) throws IOException {
        this(dir.eventListener);
        // throw away unused format flag (unused in read/write as well)
        Bytes.readLength(in);

        fileId = Bytes.readInt(in);
        fileName = Bytes.readString(in);
        in.read(); //throw away unused mode -- TODO: either have a real use or stop saving
        length = Bytes.readLength(in);
        lastModified = Bytes.readLength(in);
        int count = (int) Bytes.readLength(in);
        streams.ensureCapacity(count);
        for (int i = 0; i < count; i++) {
            int streamId = (int) Bytes.readLength(in);
            streams.add(dir.getStreamManager().findStream(streamId));
        }
    }

    @Override public String getName() {
        return fileName;
    }

    @Override public long getLength() {
        return length;
    }

    @Override public long getLastModified() {
        return lastModified;
    }

    @Override public IntStream getStreamIds() {
        return streams.stream()
                      .mapToInt(stream -> stream.streamId);
    }

    @Override public List<MuxStream> getStreams() throws IOException {
        return streams;
    }

    @Override public void writeRecord(OutputStream out) throws IOException {
        Bytes.writeLength(0, out);
        Bytes.writeInt(fileId, out);
        Bytes.writeString(fileName, out);
        out.write(0);
        Bytes.writeLength(length, out);
        Bytes.writeLength(lastModified, out);
        Bytes.writeLength(streams.size(), out);
        getStreamIds().forEachOrdered(streamId -> {
            try {
                Bytes.writeLength(streamId, out);
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        });
    }

    @Override public InputStream read(long offset, boolean uncompress) throws IOException {
        if (eventListener != null) {
            eventListener.fileEvent(MuxyFileEvent.FILE_READ, this);
        }
        // TODO potential array creation race with append
        return new MuxFileReader(getStreams().iterator(), uncompress);
    }

    @Override public InputStream read(long offset) throws IOException {
        if (eventListener != null) {
            eventListener.fileEvent(MuxyFileEvent.FILE_READ, this);
        }
        // TODO potential array creation race with append
        return new MuxFileReader(getStreams().iterator());
    }

    @Override public InputStream read() throws IOException {
        return read(0);
    }

    @Override public String toString() {
        return Objects.toStringHelper(this)
                      .add("fileId", fileId)
                      .add("fileName", fileName)
                      .add("length", length)
                      .add("lastModified", lastModified)
                      .add("streams.size", streams.size())
                      .toString();
    }

    @Override public String detail() throws IOException {
        return Objects.toStringHelper(this)
                      .add("fileId", fileId)
                      .add("fileName", fileName)
                      .add("length", length)
                      .add("lastModified", lastModified)
                      .add("streams.size", streams.size())
                      .add("streams", streams)
                      .toString();
    }
}
