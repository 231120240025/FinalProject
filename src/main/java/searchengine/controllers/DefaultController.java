package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import searchengine.services.IndexingService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

@Controller
public class DefaultController {

    private final IndexingService indexingService;
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);

    @Autowired
    public DefaultController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/api/startIndexing")
    @ResponseBody
    public Map<String, Object> startIndexing() {
        if (isIndexing.get()) {
            return Map.of(
                    "result", false,
                    "error", "Индексация уже запущена"
            );
        }

        isIndexing.set(true);
        try {
            indexingService.startFullIndexing();
            return Map.of("result", true);
        } catch (Exception e) {
            return Map.of(
                    "result", false,
                    "error", "Ошибка при запуске индексации: " + e.getMessage()
            );
        } finally {
            isIndexing.set(false);
        }
    }
}
