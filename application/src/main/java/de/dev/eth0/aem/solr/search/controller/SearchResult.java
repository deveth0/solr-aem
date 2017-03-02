/*
 * Copyright 2017 dev-eth0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dev.eth0.aem.solr.search.controller;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import de.dev.eth0.aem.solr.SolrConstants;
import de.dev.eth0.aem.solr.search.service.SolrSearchServerConfiguration;
import io.wcm.handler.link.Link;
import io.wcm.handler.link.LinkHandler;
import io.wcm.sling.models.annotations.AemObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author amuthmann
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class SearchResult {

  private static final Logger LOG = LoggerFactory.getLogger(SearchResult.class);

  @Self
  private SearchQuery searchQuery;

  @Self
  private LinkHandler linkHandler;

  @Self
  private SearchQueryLinkHandler searchQueryLinkHandler;

  @AemObject
  private PageManager pageManager;

  @AemObject
  private Page currentPage;

  private QueryResponse response;

  private List<SearchResultEntry> searchResults;
  private SpellcheckSuggestion spellcheckSuggestion;

  private GroupCommand groupCommand;

  @Inject
  private SolrSearchServerConfiguration searchServerConfiguration;

  @PostConstruct
  private void activate() {
    query(searchQuery.getSolrQuery());
  }

  private void query(SolrQuery query) {
    if (query != null) {
      try {
        HttpSolrClient.Builder builder = new HttpSolrClient.Builder(searchServerConfiguration.getSolrHttpUrl());
        HttpSolrClient client = builder.build();

        response = client.query(query);
        if (response.getGroupResponse() != null && !response.getGroupResponse().getValues().isEmpty()) {
          groupCommand = response.getGroupResponse().getValues().get(0);
        }
      }
      catch (Exception e) {
        LOG.error("Could not perform query " + query, e);
      }
    }
  }

  public long getNumberResults() {
    return groupCommand != null ? groupCommand.getNGroups() : 0;
  }

  public double getElapsedTime() {
    return response != null ? response.getElapsedTime() / 1000.0 : 0;
  }

  /**
   * @return link to this page using an alternative search term.
   */
  public SpellcheckSuggestion getSpellcheckSuggestion() {
    if (spellcheckSuggestion == null && response != null) {
      SpellCheckResponse spellcheck = response.getSpellCheckResponse();
      if (spellcheck != null && !spellcheck.getCollatedResults().isEmpty()) {
        SpellCheckResponse.Collation collation = spellcheck.getCollatedResults().get(0);
        Link link = searchQueryLinkHandler.get(currentPage).param(SearchQuery.SEARCH_QUERY_PARAMETER, collation.getCollationQueryString()).build();
        spellcheckSuggestion = new SpellcheckSuggestion(collation.getCollationQueryString(), collation.getNumberOfHits(), link);
      }
    }
    return spellcheckSuggestion;
  }

  public List<LinkItem> getPaging() {
    long numberPages = groupCommand.getNGroups() / SearchQuery.ROWS;
    int currentPageNumber = searchQuery.getStart() / SearchQuery.ROWS;
    List<LinkItem> ret = new ArrayList<>();
    long startNumber = Math.max(currentPageNumber - 5, 0);
    long endNumber = Math.min(startNumber + 10, numberPages);
    for (long i = startNumber; i <= endNumber && ret.size() < 10; i++) {
      Link link = searchQueryLinkHandler.get(currentPage).searchQuery(searchQuery).start(i * SearchQuery.ROWS).build();
      if (i == currentPageNumber) {
        link.getAnchor().addCssClass("active");
      }
      ret.add(new LinkItem(String.valueOf(i + 1), link));
    }
    return ret;
  }

  public List<SearchResultEntry> getSearchResults() {
    if (searchResults == null && groupCommand != null) {
      searchResults = new ArrayList<>();
      groupCommand.getValues().iterator().forEachRemaining(
              group -> {
                group.getResult().iterator().forEachRemaining(
                        solrDocument -> {
                          SearchResultEntry entry = getSearchResultEntry(solrDocument);
                          if (entry != null) {
                            searchResults.add(entry);
                          }
                        });
              });
    }
    return searchResults;
  }

  private SearchResultEntry getSearchResultEntry(SolrDocument solrDocument) {
    if (solrDocument.containsKey(SolrConstants.PATH_COLLAPSED)) {
      String path = (String)solrDocument.getFirstValue(SolrConstants.PATH_COLLAPSED);
      if (StringUtils.isNotBlank(path)) {
        Page page = pageManager.getContainingPage(path);
        if (page != null) {
          Link link = linkHandler.get(page).build();
          Map<String, List<String>> highlightings = response.getHighlighting().get((String)solrDocument.get(SolrConstants.PATH_EXACT));
          SearchResultEntry ret = new SearchResultEntry(page.getTitle(),
                  link,
                  highlightings != null && highlightings.containsKey("text")
                  ? highlightings.get("text").get(0)
                  : null);
          return ret;
        }
      }
    }
    return null;
  }

  public static final class LinkItem {

    private final String title;
    private final Link link;

    public LinkItem(String title, Link link) {
      this.title = title;
      this.link = link;
    }

    public String getTitle() {
      return title;
    }

    public Link getLink() {
      return link;
    }
  }

  public static final class SpellcheckSuggestion {

    private final String queryTerm;
    private final Link link;
    private final long numberOfHits;

    public SpellcheckSuggestion(String queryTerm, long numberOfHits, Link link) {
      this.queryTerm = queryTerm;
      this.link = link;
      this.numberOfHits = numberOfHits;
    }

    public String getQueryTerm() {
      return queryTerm;
    }

    public Link getLink() {
      return link;
    }

    public long getNumberOfHits() {
      return numberOfHits;
    }

  }

  public static final class SearchResultEntry {

    private final String title;
    private final Link link;
    private final String highlighting;

    private SearchResultEntry(String title, Link link, String highlighting) {
      this.title = title;
      this.link = link;
      this.highlighting = highlighting;
    }

    public String getTitle() {
      return title;
    }

    public Link getLink() {
      return link;
    }

    public String getHighlighting() {
      return highlighting;
    }

  }
}
