/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.elodina.mesos.util;

import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class IOTest {
    @Test
    public void copyAndCloseTest() throws IOException {
        byte[] data = new byte[4 * 1024 * 1024];
        for (int i = 0; i < data.length; i++) data[i] = new Integer(i).byteValue();

        final AtomicBoolean inClosed = new AtomicBoolean(false);
        final AtomicBoolean outClosed = new AtomicBoolean(false);

        InputStream in = new ByteArrayInputStream(data) {
            public void close() throws IOException { super.close(); inClosed.set(true); }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            public void close() throws IOException { super.close(); outClosed.set(true); }
        };

        IO.copyAndClose(in, out);
        assertTrue(Arrays.equals(data, out.toByteArray()));
        assertTrue(inClosed.get());
        assertTrue(outClosed.get());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void findFile0() throws IOException {
        File dir = Files.createTempDirectory(IOTest.class.getSimpleName()).toFile();

        try {
            assertNull(IO.findDir(dir, "mask.*"));

            File matchedFile = new File(dir, "mask-123");
            matchedFile.createNewFile();

            assertNull(IO.findFile0(dir, "mask.*", true));
            assertEquals(matchedFile, IO.findFile0(dir, "mask.*", false));
        } finally {
            IO.delete(dir);
        }
    }

    @Test
    public void replaceInFile() throws IOException {
        File file = Files.createTempFile(IOTest.class.getSimpleName(), null).toFile();

        Map<String, String> map = new HashMap<>();
        map.put("a=*.", "a=1");
        map.put("c=*.", "c=3");

        IO.writeFile(file, "a=0\nb=0\nc=0");
        IO.replaceInFile(file, map);
        assertEquals("a=1\nb=0\nc=3", IO.readFile(file));

        // error on miss
        IO.writeFile(file, "a=0\nb=0");
        try { IO.replaceInFile(file, map); }
        catch (IllegalStateException e) { assertTrue(e.getMessage(), e.getMessage().contains("not found in file")); }

        // ignore misses
        IO.writeFile(file, "a=0\nb=0");
        IO.replaceInFile(file, map, true);
        assertEquals("a=1\nb=0", IO.readFile(file));
    }
}
