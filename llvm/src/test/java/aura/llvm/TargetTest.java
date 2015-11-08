/*
 * Copyright (C) 2013 RoboVM AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package aura.llvm;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests {@link Target}.
 */
public class TargetTest {

    @Test
    public void testGetTargets() throws Exception {
        List<Target> all = Target.getTargets();
        assertFalse(all.isEmpty());
    }

    @Test
    public void testGetTargetsMap() throws Exception {
        Map<String, Target> map = Target.getTargetsMap();
        Target x86_64 = map.get("x86-64");
        assertNotNull(x86_64);
    }

    @Test
    public void testGetTarget() throws Exception {
        Target x86 = Target.getTarget("x86");
        assertNotNull(x86);
        assertEquals("x86", x86.getName());
        assertEquals("32-bit X86: Pentium-Pro and above", x86.getDescription());
        
        try {
            Target.getTarget("foobar");
            fail("LlvmException expected");
        } catch (LlvmException e) {}
    }
    
    @Test
    public void testLookupTarget() throws Exception {
        Target t = Target.lookupTarget("x86_64-unknown-freebsd");
        assertNotNull(t);
        assertEquals("x86-64", t.getName());
        assertTrue(t.getDescription().contains("64-bit"));
        
        try {
            Target.lookupTarget("foobar");
            fail("LlvmException expected");
        } catch (LlvmException e) {}
    }
    
    @Test
    @Ignore // Ignore for now. getHostTarget() seems to return x86-64 even on
            // 32-bit Linux.
    public void testGetHostTarget() throws Exception {
        Target t = Target.getHostTarget();
        String archProp = System.getProperty("os.arch").toLowerCase();
        if (archProp.matches("amd64|x86[-_]64")) {
            assertEquals("x86-64", t.getName());
        } else if (archProp.matches("i386|x86")) {
            assertEquals("x86", t.getName());
        } else {
            fail("Unknown os.arch: " + archProp);
        }
    }

    
}
