package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;

import java.util.List;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private final SitesList sitesList;

    private boolean indexingInProgress = false;

    public IndexingService(SitesList sitesList) {
        this.sitesList = sitesList;
    }

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
            performIndexing();
        } catch (Exception e) {
            logger.error("Ошибка во время индексации: ", e);
            throw e;
        } finally {
            indexingInProgress = false;
            logger.info("Индексация завершена.");
        }
    }

    private void performIndexing() {
        List<Site> sites = sitesList.getSites();
        if (sites == null || sites.isEmpty()) {
            logger.warn("Список сайтов для индексации пуст.");
            return;
        }

        for (Site site : sites) {
            logger.info("Индексация сайта: {} ({})", site.getName(), site.getUrl());
            try {
                // Имитация индексации
                Thread.sleep(2000); // Здесь добавьте логику индексации для каждого сайта
                logger.info("Сайт {} успешно проиндексирован.", site.getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Индексация сайта {} была прервана: ", site.getName(), e);
            } catch (Exception e) {
                logger.error("Ошибка при индексации сайта {}: ", site.getName(), e);
            }
        }
    }
}
