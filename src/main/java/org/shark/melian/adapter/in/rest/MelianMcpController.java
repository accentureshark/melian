package org.shark.melian.adapter.in.rest;



import org.shark.melian.application.usecase.MelianQueryService;
import dev.langchain4j.data.embedding.Embedding;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mcp")
public class MelianMcpController {

    private final MelianQueryService melianQueryService;

    public MelianMcpController(MelianQueryService melianQueryService) {
        this.melianQueryService = melianQueryService;
    }

    @PostMapping("/query")
    public List<Embedding> query(@RequestBody String queryOrConfig) {
        return melianQueryService.processQuery(queryOrConfig);
    }
}
