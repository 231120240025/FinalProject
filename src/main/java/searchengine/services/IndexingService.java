package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.IndexingStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Transactional
    private void performIndexing() {
        logger.info("Запуск индексации...");
        sitesList.getSites().forEach(siteConfig -> {
            String siteUrl = normalizeUrl(siteConfig.getUrl());
            String siteName = siteConfig.getName();

            try {
                deleteExistingData(siteUrl);

                Site site = new Site();
                site.setUrl(siteUrl);
                site.setName(siteName);
                site.setStatus(IndexingStatus.INDEXING);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                logger.info("Создана новая запись для сайта: {}", siteName);

                crawlSite(site, siteUrl, new HashSet<>());

                site.setStatus(IndexingStatus.INDEXED);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                logger.info("Индексация сайта '{}' завершена успешно.", siteName);
            } catch (Exception e) {
                logger.error("Ошибка индексации сайта '{}': {}", siteName, e.getMessage(), e);
            }
        });
        logger.info("Индексация завершена.");
    }

    @Transactional
    private void deleteExistingData(String siteUrl) {
        Site existingSite = siteRepository.findByUrl(siteUrl);
        if (existingSite != null) {
            logger.info("Удаление данных сайта: {}", siteUrl);
            pageRepository.deleteAllBySite(existingSite);
            siteRepository.delete(existingSite);
        }
    }

    private void crawlSite(Site site, String url, Set<String> visitedUrls) {
        url = normalizeUrl(url);

        if (visitedUrls.contains(url) || visitedUrls.size() > 1000) { // Ограничение на количество страниц
            return;
        }
        visitedUrls.add(url);

        try {
            Document document = Jsoup.connect(url).get();
            String content = document.outerHtml();
            int statusCode = 200;

            savePage(site, url, statusCode, content);

            Elements links = document.select("a[href]");
            for (Element link : links) {
                String absoluteUrl = link.absUrl("href");
                if (absoluteUrl.startsWith(site.getUrl())) {
                    crawlSite(site, absoluteUrl, visitedUrls);
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка при обработке URL '{}': {}", url, e.getMessage());
            savePage(site, url, 500, "Ошибка загрузки страницы");
        }
    }

    @Transactional
    private void savePage(Site site, String url, int statusCode, String content) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(url.replace(site.getUrl(), "")); // Сохраняем относительный путь
        page.setCode(statusCode);
        page.setContent(content);
        pageRepository.save(page);
        logger.info("Добавлена страница: {}", url);
    }

    private String normalizeUrl(String url) {
        if (url.endsWith("/") && !url.equals("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
