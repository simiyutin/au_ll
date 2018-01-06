package com.simiyutin;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class MainTest {
    @Test
    public void testSimple() {
        LockFreeSet<Integer> set = new LockFreeSetImpl<>();
        assertTrue(set.isEmpty());
        assertTrue(set.add(1));
        assertFalse(set.isEmpty());
        assertTrue(set.contains(1));
        assertFalse(set.add(1));
        assertTrue(set.remove(1));
        assertFalse(set.remove(1));
        assertTrue(set.isEmpty());
        assertFalse(set.contains(1));
    }
}
