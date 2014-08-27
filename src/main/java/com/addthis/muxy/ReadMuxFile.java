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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.addthis.basis.util.Bytes;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;

/** */
public class ReadMuxFile {

    // I am making these non-final for ease of use in (writable) MuxFile.
    //   it is possible to avoid this by having non-final variables in MuxFile
    //   and gating all access to these variables through overridable methods,
    //   but that would be a lot less elegant in terms of inheritance and create
    //   a lot of unused heap data.
    protected int fileID;
    protected String fileName;
    protected long length;
    protected long lastModified;

    protected final ReadMuxFileDirectory dir;
    protected final int[] streamIDs;

    public ReadMuxFile(ReadMuxFileDirectory dir) {
        this.dir = dir;
        streamIDs = null;
    }

    public ReadMuxFile(InputStream in, ReadMuxFileDirectory dir) throws IOException {
        this.dir = dir;

        // throw away unused format flag (unused in read/write as well)
        Bytes.readLength(in);

        fileID = Bytes.readInt(in);
        fileName = Bytes.readString(in);
        in.read(); //throw away unused mode -- TODO: either have a real use or stop saving
        length = Bytes.readLength(in);
        lastModified = Bytes.readLength(in);
        int count = (int) Bytes.readLength(in);
        streamIDs = new int[count];
        for (int i = 0; i < count; i++) {
            streamIDs[i] = (int) Bytes.readLength(in);
        }
    }

    public String getName() {
        return fileName;
    }

    protected Collection<MuxStream> getStreams() throws IOException {
        LinkedList<MuxStream> streams = new LinkedList<>();
        for (Integer id : getStreamIDs()) {
            streams.add(dir.getStreamManager().findStream(id));
        }
        return streams;
    }

    public long getLength() {
        return length;
    }

    public long getLastModified() {
        return lastModified;
    }

    public List<Integer> getStreamIDs() {
        return Ints.asList(streamIDs);
    }

    public void writeRecord(OutputStream out) throws IOException {
        Bytes.writeLength(0, out);
        Bytes.writeInt(fileID, out);
        Bytes.writeString(fileName, out);
        out.write(0);
        Bytes.writeLength(length, out);
        Bytes.writeLength(lastModified, out);
        Bytes.writeLength(getStreamIDs().size(), out);
        for (Integer streamID : getStreamIDs()) {
            Bytes.writeLength(streamID, out);
        }
    }

    public InputStream read(long offset, boolean uncompress) throws IOException {
        dir.publishEvent(MuxyFileEvent.FILE_READ, this);
        // TODO potential array creation race with append
        return new MuxFileReader(dir, getStreamIDs().iterator(),
                uncompress);
    }

    public InputStream read(long offset) throws IOException {
        dir.publishEvent(MuxyFileEvent.FILE_READ, this);
        // TODO potential array creation race with append
        return new MuxFileReader(dir, getStreamIDs().iterator());
    }

    public InputStream read() throws IOException {
        return read(0);
    }

    @Override public String toString() {
        return Objects.toStringHelper(this)
                      .add("fileID", fileID)
                      .add("fileName", fileName)
                      .add("length", length)
                      .add("lastModified", lastModified)
                      .add("dir", dir)
                      .add("streamIDs.length", streamIDs.length)
                      .toString();
    }

    public String detail() throws IOException {
        return Objects.toStringHelper(this)
                      .add("fileID", fileID)
                      .add("fileName", fileName)
                      .add("length", length)
                      .add("lastModified", lastModified)
                      .add("dir", dir)
                      .add("streamIDs.length", streamIDs.length)
                      .add("streamIDs", getStreams())
                      .toString();
    }
}
