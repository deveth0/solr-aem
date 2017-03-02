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

import de.dev.eth0.aem.solr.SolrConstants;
import io.wcm.handler.url.ui.SiteRoot;
import java.nio.charset.Charset;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class)
public class SearchQuery {

  private static final Logger LOG = LoggerFactory.getLogger(SearchQuery.class);

  public static final String SEARCH_QUERY_PARAMETER = "q";

  public static final String SEARCH_QUERY_PAGE_PARAMETER = "start";

  public static final int ROWS = 10;

  @SlingObject
  private SlingHttpServletRequest request;

  private String queryString;
  private Integer start;

  @Self
  private SiteRoot siteRoot;

  @PostConstruct
  private void activate() {
    queryString = request.getParameter(SEARCH_QUERY_PARAMETER);
    start = NumberUtils.createInteger(request.getParameter(SEARCH_QUERY_PAGE_PARAMETER));
    if (start == null) {
      start = 0;
    }
  }

  public String getQueryString() {
    return queryString;
  }

  public int getStart() {
    return start;
  }

  public List<NameValuePair> getQueryStringPairs() {
    return URLEncodedUtils.parse(request.getQueryString(), Charset.forName("UTF-8"));
  }

  public SolrQuery getSolrQuery() {
    if (StringUtils.isNotBlank(queryString)) {
      SolrQuery query = new SolrQuery();
      query.setQuery(queryString);
      // show only results below the current siteroot
      query.setFilterQueries(SolrConstants.PATH_DES + ":" + ClientUtils.escapeQueryChars(siteRoot.getRootPath()));
      query.setRows(ROWS);
      if (start != null) {
        query.setStart(start);
      }

      query.set("df", "text");

      // enable highlighting
      query.set("hl", "true");
      query.set("hl.fl", "text");
      query.set("hl.fragsize", 400);
      query.set("hl.simple.pre", "<b>");
      query.set("hl.simple.post", "</b>");

      // enable grouping as we only want a single result per page
      query.set("group", "true");
      query.set("group.field", SolrConstants.PATH_COLLAPSED);
      query.set("group.ngroups", "true");
      LOG.debug(query.toQueryString());
      return query;
    }
    return null;
  }

}
