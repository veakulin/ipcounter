package ru.akulin.ipcounter;

import static org.junit.jupiter.api.Assertions.*;

class IndexTest {

    @org.junit.jupiter.api.Test
    void run() {
        Index index = new Index();
        assertEquals(0, index.cardinality());

        index.insert("0.0.0.0");
        index.insert("255.255.255.255");
        index.insert("0.0.0.0");
        assertEquals(2, index.cardinality());

        assertThrows(RuntimeException.class, () -> index.insert("1111.111.111"));
    }
}