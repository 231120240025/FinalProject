package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.IndexingStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

@Service
public class IndexingService {
    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    private boolean indexingInProgress = false;

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Autowired
    public IndexingService(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    public synchronized boolean isIndexing() {
        return indexingInProgress;
    }

    public synchronized void startIndexing() {
        if (indexingInProgress) {
            logger.warn("Индексация уже запущена.");
            return;
        }

        indexingInProgress = true;
        new Thread(() -> {
            try {
                performIndexing();
            } catch (Exception e) {
                logger.error("Ошибка во время индексации: {}", e.getMessage(), e);
            } finally {
                indexingInProgress = false;
            }
        }).start();
    }

    private void performIndexing() {
        logger.info("Запуск индексации...");
        sitesList.getSites().forEach(siteConfig -> {
            String siteUrl = siteConfig.getUrl();
            String siteName = siteConfig.getName();

            try {
                deleteExistingData(siteUrl);
                createNewSiteRecord(siteUrl, siteName);
                logger.info("Индексация сайта '{}' завершена успешно.", siteName);
            } catch (Exception e) {
                logger.error("Ошибка индексации сайта '{}': {}", siteName, e.getMessage(), e);
            }
        });

        logger.info("Индексация завершена.");
    }

    private void deleteExistingData(String siteUrl) {
        Site existingSite = siteRepository.findByUrl(siteUrl);
        if (existingSite != null) {
            logger.info("Удаление данных сайта: {}", siteUrl);
            pageRepository.deleteAllBySite(existingSite);
            siteRepository.delete(existingSite);
        }
    }

    private void createNewSiteRecord(String siteUrl, String siteName) {
        Site site = new Site();
        site.setUrl(siteUrl);
        site.setName(siteName);
        site.setStatus(IndexingStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        logger.info("Создана новая запись для сайта: {}", siteName);
    }
}
