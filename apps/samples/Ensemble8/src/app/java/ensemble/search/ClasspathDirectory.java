/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

/**
 * A very simple implementation of lucene Directory, it reads a index from the classpath in a directory called index
 * under the package that contains this file. It depends on a "listAll.txt" file written into that directory containing
 * the names of all the other files and their sizes. In the format "name:length" one file per line. When a file needs
 * to be read the whole file is loaded into memory.
 */
public class ClasspathDirectory extends Directory {
    private String[] allFiles;
    private final Map<String,Long> fileLengthMap = new HashMap<>();

    public ClasspathDirectory() {
        // load list of all files
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("index/listAll.txt")));
            String line;
            List<String> fileNames = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                fileNames.add(parts[0]);
                fileLengthMap.put(parts[0], Long.parseLong(parts[1]));
            }
            reader.close();
            allFiles = fileNames.toArray(new String[fileNames.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public String[] listAll() throws IOException {
        return allFiles;
    }

    @Override public IndexInput openInput(String s) throws IOException {
        return new ClassPathIndexInput(
            getClass().getResourceAsStream("index/"+s),
            fileLengthMap.get(s).intValue()
        );
    }

    private static class ClassPathIndexInput extends IndexInput {
        private byte[] data;
        private int pointer = 0;
        private int length;

        private ClassPathIndexInput(InputStream in, int length) throws IOException {
            this.length = length;
            // read whole file into memory, so we can provide random access
            data = new byte[length];
            // read in upto 20k chunks
            // this is needed as the amount of bytes read in any call in not
            // garenteed to be number asked for
            final byte[] buf = new byte[1024*20];
            int offset = 0, remaining = length, read;
            do {
                read = in.read(buf,0,Math.min(remaining, buf.length));
                // copy read bytes to data
                if (read > 0) {
                    System.arraycopy(buf, 0, data, offset, read);
                    offset += read;
                    remaining -= read;
                }
            } while (read != -1 && remaining > 0);
            in.close();
        }

        @Override public byte readByte() throws IOException {
            return data[pointer ++];
        }

        @Override public void readBytes(byte[] bytes, int offset, int len) throws IOException {
            System.arraycopy(data, pointer, bytes, offset, len);
            pointer += len;
        }

        @Override public void close() throws IOException {}

        @Override public long getFilePointer() { return pointer; }

        @Override public void seek(long l) throws IOException { pointer = (int)l; }

        @Override public long length() { return length; }
    }

    @Override public void close() throws IOException {}
    @Override public boolean fileExists(String s) throws IOException { throw new UnsupportedOperationException("Not implemented"); }
    @Override public long fileModified(String s) throws IOException { throw new UnsupportedOperationException("Not implemented"); }
    @Override @Deprecated public void touchFile(String s) throws IOException { throw new UnsupportedOperationException("Not implemented"); }
    @Override public void deleteFile(String s) throws IOException { throw new UnsupportedOperationException("Not implemented"); }
    @Override public long fileLength(String s) throws IOException { throw new UnsupportedOperationException("Not implemented"); }
    @Override public IndexOutput createOutput(String s) throws IOException { throw new UnsupportedOperationException("Not implemented"); }
}
