package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageRepository extends JpaRepository<Page, Long> {
    void deleteAllBySite(Site site);
    boolean existsBySiteAndPath(Site site, String path);

}
