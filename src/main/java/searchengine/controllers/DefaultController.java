package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.services.IndexingService;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DefaultController {

    private final IndexingService indexingService;

    @Autowired
    public DefaultController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/api/startIndexing")
    public Map<String, Object> startIndexing() {
        Map<String, Object> response = new HashMap<>();
        if (indexingService.isIndexing()) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
        } else {
            indexingService.startIndexing();
            response.put("result", true);
        }
        return response;
    }
}
