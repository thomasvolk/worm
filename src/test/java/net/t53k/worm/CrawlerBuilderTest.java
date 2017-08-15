package net.t53k.worm;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

public class CrawlerBuilderTest {
    private final String BASE = "pages/tree";
    private final Function<String, Body> RESOURCE_LOADER = (url) -> {
        try {
            try(InputStream is = getClass().getResourceAsStream(String.format("%s/%s", BASE, url))) {
                return new Body(IOUtils.toByteArray(is), new ContentType("text/html"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
    private final Function<String, Boolean> LINK_FILTER = (url) -> !url.contains("filterthis");

    private Set<String> documents;
    private List<String> errors;

    @Before
    public void init() {
        documents = new TreeSet<>();
        errors = new ArrayList<>();
    }

    private void onError(String e) {
       errors.add(e);
    }

    private void addDocument(Document doc) {
        documents.add(doc.getResource().getUrl());
    }

    @Test
    public void crawler() {
        CrawlerBuilder cb = new CrawlerBuilder();
        Crawler crawler = cb.onDocument(this::addDocument).loadResource(RESOURCE_LOADER).filterLinks(LINK_FILTER)
                .onError(this::onError).worker(4).build();
        List<String> pendingPages = crawler.start(Collections.singletonList("index.html"), InfinityTimeout.INSTANCE);
        assertEquals(0, pendingPages.size());
        assertEquals(new HashSet<>(Arrays.asList("index.html", "subpage.01.a.html", "subpage.01.b.html", "subpage.02.a.html")), documents);
        assertEquals(Collections.singletonList("notfound.html"), errors);
    }
}
