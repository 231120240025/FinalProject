package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    private boolean indexingInProgress = false;

    public synchronized boolean isIndexingInProgress() {
        return indexingInProgress;
    }

    public synchronized void startFullIndexing() {
        if (indexingInProgress) {
            logger.warn("Попытка запустить индексацию, которая уже выполняется.");
            throw new IllegalStateException("Индексация уже запущена.");
        }
        indexingInProgress = true;
        logger.info("Индексация начата.");
        try {
            performIndexing(); // Логика индексации
        } catch (Exception e) {
            logger.error("Ошибка во время индексации: ", e);
            throw e; // Можно также обработать ошибку и вернуть её
        } finally {
            indexingInProgress = false;
            logger.info("Индексация завершена.");
        }
    }

    private void performIndexing() {
        try {
            // Здесь добавьте реальную логику индексации
            logger.info("Индексация сайтов выполняется...");
            Thread.sleep(5000); // Имитация длительного процесса
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Индексация была прервана: ", e);
            throw new RuntimeException("Индексация была прервана", e);
        }
    }
}
