package org.shark.melian.domain.service;


import dev.langchain4j.data.segment.TextSegmenter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChunkingService {

    private final TextSegmenter segmenter;

    public ChunkingService(TextSegmenter segmenter) {
        this.segmenter = segmenter;
    }

    public List<TextSegment> chunk(String rawText) {
        return segmenter.segment(rawText);
    }
}
