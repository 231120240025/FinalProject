package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.IndexingStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private volatile boolean indexingInProgress = false; // Обновлено для корректной работы с многопоточностью

    public IndexingService(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
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

    @Transactional
    private void deleteSiteData(String siteUrl) {
        searchengine.model.Site site = siteRepository.findByUrl(siteUrl);
        if (site != null) {
            logger.info("Удаление данных для сайта: {}", siteUrl);
            pageRepository.deleteAllBySiteId(site.getId()); // Удаляем страницы, связанные с сайтом
            siteRepository.delete(site); // Удаляем сам сайт
            logger.info("Данные для сайта {} удалены.", siteUrl);
        } else {
            logger.info("Данные для сайта {} отсутствуют.", siteUrl);
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
                // Удаляем старые данные
                deleteSiteData(site.getUrl());

                // Создаем новую запись о сайте
                searchengine.model.Site newSite = new searchengine.model.Site();
                newSite.setName(site.getName());
                newSite.setUrl(site.getUrl());
                newSite.setStatus(IndexingStatus.INDEXING);
                newSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(newSite);

                // Логика индексации сайта (заменить на реальную)
                simulateIndexing(newSite);

                // Обновляем статус сайта
                newSite.setStatus(IndexingStatus.INDEXED);
                newSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(newSite);

                logger.info("Сайт {} успешно проиндексирован.", site.getName());
            } catch (Exception e) {
                logger.error("Ошибка при индексации сайта {}: {}", site.getName(), e.getMessage());
                handleIndexingError(site.getUrl(), e);
            }
        }
    }

    private void simulateIndexing(searchengine.model.Site site) throws InterruptedException {
        // Симуляция индексации: заменить на логику парсинга сайта и добавления страниц
        Thread.sleep(2000);
        logger.info("Сайт {} ({}) обработан.", site.getName(), site.getUrl());
    }

    @Transactional
    private void handleIndexingError(String siteUrl, Exception e) {
        searchengine.model.Site site = siteRepository.findByUrl(siteUrl);
        if (site != null) {
            site.setStatus(IndexingStatus.FAILED);
            site.setLastError(e.getMessage());
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
    }
}
