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
package de.dev.eth0.aem.solr.search.service;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

/**
 * Server configuration for Solr Search
 */
@Component(metatype = true, immediate = true, label = "Solr Search Server Configuration")
@Service(SolrSearchServerConfiguration.class)
public class SolrSearchServerConfiguration {

  private static final String DEFAULT_SOLR_HTTP_URL = "http://192.168.50.10:8983/solr/oak";

  @Property(value = DEFAULT_SOLR_HTTP_URL, label = "Solr HTTP URL")
  private static final String SOLR_HTTP_URL = "solr.http.url";

  private String solrHttpUrl;

  @Activate
  protected void activate(ComponentContext componentContext) throws Exception {
    solrHttpUrl = String.valueOf(componentContext.getProperties().get(SOLR_HTTP_URL));
  }

  @Deactivate
  protected void deactivate() throws Exception {
    solrHttpUrl = null;
  }

  public String getSolrHttpUrl() {
    return solrHttpUrl;
  }

}
