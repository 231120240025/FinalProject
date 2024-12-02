package searchengine.services;

import org.springframework.stereotype.Service;

@Service
public class IndexingService {
    private boolean indexingInProgress = false;

    public synchronized boolean isIndexing() {
        return indexingInProgress;
    }

    public synchronized void startIndexing() {
        indexingInProgress = true;
        new Thread(() -> {
            try {
                // Логика индексации всех сайтов
                performIndexing();
            } finally {
                indexingInProgress = false;
            }
        }).start();
    }

    private void performIndexing() {
        // Реализация индексации сайтов
        // Например, проход по списку сайтов и выполнение их индексации
        System.out.println("Индексация началась...");
        try {
            Thread.sleep(5000); // Симуляция длительного процесса
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Индексация завершена.");
    }
}
