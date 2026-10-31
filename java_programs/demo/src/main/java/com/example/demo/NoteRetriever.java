package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class NoteRetriever {

    record Chunk(String src, String text) {}

    private final List<Chunk> chunks;

    public NoteRetriever() throws IOException {
        // 绝对路径更稳：Spring 的工作目录不一定是你想的那个
        this.chunks = load("/home/wanfch/ai-fde/notes");
    }

    private List<Chunk> load(String dir) throws IOException {
        List<Chunk> all = new ArrayList<>();
        try (Stream<Path> files = Files.list(Path.of(dir))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".md")).toList()) {
                String text = Files.readString(f);
                for (int i = 0; i < text.length(); i += 250) {   // 步长250=300-50重叠
                    all.add(new Chunk(f.getFileName().toString(),
                            text.substring(i, Math.min(i + 300, text.length()))));
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