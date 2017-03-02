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
import io.wcm.handler.link.Link;
import io.wcm.handler.link.LinkHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 *
 * @author deveth0
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class SearchQueryLinkHandler {

  @Self
  private SearchQuery searchQuery;

  @Self
  private LinkHandler linkHandler;


  public SearchQueryLinkBuilder get(Page page) {
    return new SearchQueryLinkBuilder(linkHandler, page);
  }

  public static class SearchQueryLinkBuilder {

    private final Map<String, NameValuePair> params = new HashMap<>();
    private final Page page;
    private final LinkHandler linkHandler;

    public SearchQueryLinkBuilder(LinkHandler linkHandler, Page page) {
      this.page = page;
      this.linkHandler = linkHandler;
    }

    public SearchQueryLinkBuilder param(String name, String value) {
      params.put(name, new BasicNameValuePair(name, value));
      return this;
    }

    public SearchQueryLinkBuilder searchQuery(SearchQuery searchQuery) {
      params.putAll(
              searchQuery.getQueryStringPairs()
                      .stream()
                      .collect(Collectors.toMap(
                              NameValuePair::getName,
                              item -> item,
                              // use first occurence
                              (item1, item2) -> {
                                return item1;
                              }
                      )));
      return this;
    }

    public SearchQueryLinkBuilder queryTerm(String queryTerm) {
      return param(SearchQuery.SEARCH_QUERY_PARAMETER, queryTerm);
    }

    public SearchQueryLinkBuilder start(long start) {
      return param(SearchQuery.SEARCH_QUERY_PAGE_PARAMETER, String.valueOf(start));
    }

    public String buildQueryString() {
      return URLEncodedUtils.format(new ArrayList(params.values()), "UTF-8");
    }

    public Link build() {
      return linkHandler.get(page).queryString(buildQueryString()).build();
    }

  }

}
