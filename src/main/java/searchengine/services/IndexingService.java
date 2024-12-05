package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.IndexingStatus;
import searchengine.model.Page;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private volatile boolean indexingInProgress = false;

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
            int pagesDeleted = pageRepository.deleteAllBySiteId(site.getId());
            siteRepository.delete(site);
            logger.info("Удалено {} записей из таблицы page для сайта {}.", pagesDeleted, siteUrl);
            logger.info("Запись о сайте {} удалена из таблицы site.", siteUrl);
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
                deleteSiteData(site.getUrl());
                searchengine.model.Site newSite = new searchengine.model.Site();
                newSite.setName(site.getName());
                newSite.setUrl(site.getUrl());
                newSite.setStatus(IndexingStatus.INDEXING);
                newSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(newSite);
                crawlAndIndexPages(newSite, site.getUrl(), new HashSet<>());
                updateSiteStatusToIndexed(newSite);
                logger.info("Сайт {} успешно проиндексирован.", site.getName());
            } catch (Exception e) {
                handleIndexingError(site.getUrl(), e);
            }
        }
    }

    private void crawlAndIndexPages(searchengine.model.Site site, String url, Set<String> visitedUrls) {
        if (visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);

        try {
            String contentType = Jsoup.connect(url).ignoreContentType(true).execute().contentType();
            int statusCode = Jsoup.connect(url).ignoreContentType(true).execute().statusCode();

            if (contentType != null && contentType.startsWith("image/")) {
                Page page = new Page();
                page.setSite(site);
                page.setPath(new URL(url).getPath());
                page.setCode(statusCode);
                page.setContent("Image content: " + contentType);
                pageRepository.save(page);
                logger.info("Изображение добавлено: {}", url);
                updateSiteStatusTime(site);
                return;
            }

            if (contentType == null || !(contentType.startsWith("text/") || contentType.contains("xml"))) {
                logger.warn("Пропуск URL {}: неподдерживаемый тип контента {}", url, contentType);
                updateSiteStatusTime(site);
                return;
            }

            Document document = Jsoup.connect(url).get();
            String content = document.html();

            Page page = new Page();
            page.setSite(site);
            page.setPath(new URL(url).getPath());
            page.setCode(statusCode);
            page.setContent(content);
            pageRepository.save(page);

            logger.info("Страница добавлена: {}", url);
            updateSiteStatusTime(site);

            Elements links = document.select("a[href]");
            for (Element link : links) {
                String childUrl = link.absUrl("href");
                if (childUrl.startsWith(site.getUrl())) {
                    crawlAndIndexPages(site, childUrl, visitedUrls);
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка при обработке URL {}: {}", url, e.getMessage());
            updateSiteStatusTime(site);
        }
    }

    private void updateSiteStatusToIndexed(searchengine.model.Site site) {
        site.setStatus(IndexingStatus.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        logger.info("Сайт {} изменил статус на INDEXED.", site.getUrl());
    }

    private void updateSiteStatusTime(searchengine.model.Site site) {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        logger.info("Обновлено время status_time для сайта {}: {}", site.getUrl(), site.getStatusTime());
    }

    private void handleIndexingError(String siteUrl, Exception e) {
        searchengine.model.Site site = siteRepository.findByUrl(siteUrl);
        if (site != null) {
            site.setStatus(IndexingStatus.FAILED);
            site.setLastError(e.getMessage());
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            logger.error("Индексация сайта {} завершилась ошибкой: {}", site.getUrl(), e.getMessage());
        }
    }
}
