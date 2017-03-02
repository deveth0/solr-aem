AEM Solr Sample
===========================================

This project provides an out of the box example how to use an external Solr instance in AEM.

# Implemented Features

The following search features can be tested with this project:

* [Paging](https://cwiki.apache.org/confluence/display/solr/Pagination+of+Results)
* [Highlighting](https://cwiki.apache.org/confluence/display/solr/Highlighting)
* [Spellcheck](https://cwiki.apache.org/confluence/display/solr/Spell+Checking)

Missing features (planned):
* [MoreLikeThis](https://cwiki.apache.org/confluence/display/solr/MoreLikeThis): already available through the query-parameters `mlt=on&mlt.fl=text`
* [Suggester](https://cwiki.apache.org/confluence/display/solr/Suggester)
* [Faceting](https://cwiki.apache.org/confluence/display/solr/Faceting): already available through the query-parameters `facet.field=jcr_mimeType&facet=on&indent=on&q=path_collapsed:*/content/dam/*` (requires geometrixx sample content)


# Quickstart

You don't need to configure anything manual using this project, just checkout the Git repository and perform the following steps:

## Requirements

If you want to run this project without any required modifications, this should be available on your machine:

* AEM 6.2 running on http://localhost:4502
* Vagrant
* Vagrant Plugin vbguest


## Start Solr using Vagrant

You can use the VagrantFile provided in /misc/vagrant to setup a local Solr instance. It assumes that you are using VirtualBox as provider.

````
vagrant up
````

This will create a local box running Debian and Solr 6.4. The vm listens on the private only IP 192.168.50.10, so Solr Admin can be reached on [192.168.50.10:8983](http://192.168.50.10:8983)


## Provision AEM

As soon as Solr is running, you can start the `clean_install_deploy_package.sh` which will install a sample-application in AEM and configure AEM to use Solr as Indexer.

The examples use the [wcm.io Sample Application](http://wcm.io/samples/) enriched with Wikipedia Content created using [wiki2aem](https://www.dev-eth0.de/blog/2017/02/18/wiki2aem.html).

To install the application, basic configuration and sample-content execute the deploy script `clean_install_deploy_package.sh`.

Afterwards you can find a Search Page on [http://localhost:4502/content/wcm-io-samples/en/search.html](http://localhost:4502/content/wcm-io-samples/en/search.html).


# Manual Setup

The following steps are not required if you use the Quickstart Guide above.

## Requirements

This readme assumes, that you have a Solr 6.x instance and AEM6.2 running. Depending on your configuration, you need to change the given urls/ports.
* AEM: http://localhost:4502
* Solr: http://192.168.50.10:8983 (see [start solr using vagrant]((#start-solr-using-vagrant)))

## Solr

As soon as Solr is running, you need to create a new core names "oak" and deploy a basic schema.

````
su - solr
/opt/solr/bin/solr create -c oak
````

You can find the schema definitions in `\misc\schema` and should copy them into your core's conf folder and reload the core.

## AEM

### System Console

[System Console](http://localhost:4502/system/console/configMgr):

__Apache Jackrabbit Oak Solr remote server configuration__

Solr HTTP Url: http://192.168.50.10:8983/solr/oak
Zookeeper Host: empty

__Apacke Jackrabbit Oak Solr server provider__

ServerType: Remote Solr


__Apache Jackrabbit Oak Solr indexing / search configuration__

collapse jcr:content nodes: Enabled

__Solr Search Server Configuration__

Solr HTTP Url: http://192.168.50.10:8983/solr/oak

### CRXDE Lite

[CRXDE Lite](http://localhost:4502/crx/de/index.jsp):

Create a new node `solrIndex` in `oak:index` with the following properties:

| Property | Type | Value |
| -------- | ---- | ----- |
|jcr:primaryType | Name | oak:QueryIndexDefinition|
|type | String | solr|
|async | String | async|
|reindex | Boolean | true|


As soon as you hit save, AEM will start to index the existing content in Solr. This might take some time, you can see the current amount of documents in the [Solr Admin UI](http://192.168.50.10:8983/solr/#/oak)


