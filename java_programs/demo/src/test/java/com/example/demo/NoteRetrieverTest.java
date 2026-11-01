package com.example.demo;

import com.example.demo.tools.NoteRetriever;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NoteRetrieverTest {

    @Test
    void top3_sorted_by_keyword_cores() {
        var retriever = new NoteRetriever(List.of(
            new NoteRetriever.Chunk("a.md", "线程池的核心参数和拒绝策略"),
            new NoteRetriever.Chunk("b.md", "虚拟线程适合IO密集场景"),
            new NoteRetriever.Chunk("c.md", "线程池队列和线程数配置")
        ));

        var hits = retriever.top3("线程池配置");

        assertEquals(3, hits.size());
        assertEquals("c.md", hits.get(0).src());
    }
}