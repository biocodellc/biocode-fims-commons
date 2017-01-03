#!/usr/bin/python

import sys, argparse, json, ipdb
from datetime import date
from elasticsearch import Elasticsearch
from elasticsearch.helpers import reindex as es_reindex

def reindexAll(host, port):
    """
    will create a new index, copy the mapping from the old index, and then copy the data from old index to the new
    for all indices in es. This is useful for changing the default template, etc.
    :param host:
    :param port:
    :return:
    """
    es = Elasticsearch([{'host': host, 'port': port}])

    indices = es.indices.get('_all')

    for index, settings in indices.items():
        doReindex(es, index, settings)


def doReindex(es, index, settings):
    """
    will create a new index, copy the mapping from the old index, and then copy the data from old index to the new index.
    This is useful for changing the default template, etc.

    :param es:
    :param index: the name of the old index
    :param settings: settings object returned from a GET index request
    :return:
    """
    today = date.today().strftime("%Y%m%d")

    projectId = index.split("_")[0]

    data = {}
    data['mappings'] = settings['mappings']

    es.indices.create(projectId + "_" + today, body=json.dumps(data))
    es_reindex(es, index, projectId + "_" + today, chunk_size=500)


def deleteOldIndexes(host, port):
    """
    WARNING: this needs to be updated to detect the latest index. currently deletes all indexes w/o "_" in the name
    :param host:
    :param port:
    :return:
    """
    es = Elasticsearch([{'host': host, 'port': port}])

    indices = es.indices.get('_all')

    for index, settings in indices.items():
        if index.find("_") == -1:
            es.indices.delete(index)


def addAliases(host, port):
    """
    This will parse the index name, and add an alias for all indices.

    Expects indexes to be named {projectId}_{date} and will create an alias with the {projectId} value

    :param host:
    :param port:
    :return:
    """
    es = Elasticsearch([{'host': host, 'port': port}])

    indices = es.indices.get('_all')

    for index, settings in indices.items():
        alias = index.split("_")[0]
        es.indices.put_alias(index, alias)



def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", help="the command to run. Can be reindexAll or addAliases")
    parser.add_argument("host", help="host for the elasticsearch instance")
    parser.add_argument("port", help="port for the elasticsearch instance")
    args = parser.parse_args()

    if (args.command == "reindexAll"):
        reindexAll(args.host, args.port)
    elif (args.command == "deleteOldIndexes"):
        # deleteOldIndexes(args.host, args.port)
    elif (args.command == "addAliases"):
        addAliases(args.host, args.port)


if __name__ == "__main__":
    main()
