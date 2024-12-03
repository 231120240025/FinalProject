package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.services.IndexingService;

import java.util.Map;

@Controller
public class DefaultController {

    private final IndexingService indexingService;

    @Autowired
    public DefaultController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @RequestMapping("/")
    public String index() {
        return "index";
    }
    @GetMapping("/api/startIndexing")
    public ResponseEntity<?> startIndexing() {
        if (indexingService.isIndexingInProgress()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "result", false,
                            "error", "Индексация уже запущена"
                    )
            );
        }
        indexingService.startFullIndexing();
        return ResponseEntity.ok(
                Map.of("result", true)
        );
    }
}
