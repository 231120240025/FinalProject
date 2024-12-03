package searchengine.services;

import org.springframework.stereotype.Service;

@Service
public class IndexingService {

    private boolean indexingInProgress = false;

    public synchronized boolean isIndexingInProgress() {
        return indexingInProgress;
    }

    public synchronized void startFullIndexing() {
        indexingInProgress = true;
        try {
            performIndexing(); // Логика индексации
        } finally {
            indexingInProgress = false;
        }
    }

    private void performIndexing() {
        // Здесь реализуйте основную логику индексации сайтов
        System.out.println("Индексация выполняется...");
        try {
            Thread.sleep(5000); // Имитация длительного процесса
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Индексация была прервана", e);
        }
        System.out.println("Индексация завершена.");
    }
}
