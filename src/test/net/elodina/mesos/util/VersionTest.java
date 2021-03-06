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

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VersionTest {
    @Test
    public void init() {
        assertEquals(Arrays.<Integer>asList(), new Version().values());
        assertEquals(Arrays.asList(1, 0), new Version(1, 0).values());
        assertEquals(Arrays.asList(1, 2, 3, 4), new Version("1.2.3.4").values());

        try { new Version(" "); fail(); }
        catch (IllegalArgumentException e) {}

        try { new Version("."); fail(); }
        catch (IllegalArgumentException e) {}

        try { new Version("a"); fail(); }
        catch (IllegalArgumentException e) {}
    }

    @Test
    public void values() {
        assertEquals(Arrays.<Integer>asList(), new Version("").values());
        assertEquals(Arrays.asList(1), new Version("1").values());
        assertEquals(Arrays.asList(1, 2), new Version("1.2").values());
        assertEquals(Arrays.asList(1, 2, 3), new Version("1.2.3").values());
    }

    @Test
    public void equals() {
        assertEquals(new Version(), new Version());
        assertEquals(new Version("1"), new Version("1"));
        assertEquals(new Version("1.2.3"), new Version("1.2.3"));

        assertEquals(new Version("01.02"), new Version("1.2"));
    }

    @Test
    public void compareTo() {
        assertEquals(0, new Version().compareTo(new Version()));
        assertEquals(0, new Version(0).compareTo(new Version(0)));

        assertTrue(new Version(0).compareTo(new Version(1)) < 0);
        assertTrue(new Version(0).compareTo(new Version(0, 0)) < 0);

        assertTrue(new Version(0, 9, 0, 0).compareTo(new Version(0, 8, 2, 0)) > 0);
    }

    @Test
    public void _toString() {
        assertEquals("", "" + new Version());
        assertEquals("1", "" + new Version(1));
        assertEquals("1.2", "" + new Version(1,2));

        assertEquals("1.2.3", "" + new Version(1,2,3));
        assertEquals("1.2.3", "" + new Version("1.2.3"));
    }
}
