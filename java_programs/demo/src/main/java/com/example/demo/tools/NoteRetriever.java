package com.example.demo.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoteRetriever {

    // 1. Chunk 加一列预存小写
    public record Chunk(String src, String text, String lower) {};

    private final List<Chunk> chunks;

    // 测试专用：同包可见，绕过文件系统
    public NoteRetriever(List<Chunk> chunks) {
        this.chunks = chunks;
    }

    @Autowired
    public NoteRetriever(@Value("${notes.dir:./notes}") String dir) throws IOException {
        this.chunks = load(dir);
    }

    private static List<Chunk> load(String dir) throws IOException {
        Path path = Path.of(dir);
        if (!Files.exists(path)) {                        // 目录不存在：空索引启动，不炸
            System.out.println("[NoteRetriever] 目录不存在，空索引启动: " + dir);
            return List.of();
        }
        List<Chunk> all = new ArrayList<>();
        try (Stream<Path> files = Files.list(Path.of(dir))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".md")).toList()) {
                String text = Files.readString(f);
                for (int i = 0; i < text.length(); i += 250) {  
                     // 步长250=300-50重叠
                     String name = f.getFileName().toString();
                     String textTemp = text.substring(i, Math.min(i + 300, text.length()));
                    all.add(new Chunk(name, textTemp, textTemp.toLowerCase()));
                }
            }
        }
        return all;
    }

    public List<Chunk> top3(String query) {
        Set<Character> qs = query.toLowerCase().chars()
                .mapToObj(c -> (char) c).collect(Collectors.toSet());
        return chunks.stream()
                .sorted(Comparator.comparingInt(c -> -score(qs, c.text())))
                .limit(3)
                .toList();
    }

    private int score(Set<Character> qs, String text) {
        int s = 0;
        for (char c : text.toLowerCase().toCharArray()) if (qs.contains(c)) s++;
        return s;
    }
}