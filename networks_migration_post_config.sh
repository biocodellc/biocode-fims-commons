#!/bin/bash

for id in 1 2 3 4 5 6 7 8 9 10 11 12
do
    psql -d biscicol_network -c "INSERT INTO network_1.event (local_identifier, expedition_id, data) (SELECT local_identifier, expedition_id, data FROM project_${id}.event)";
    psql -d biscicol_network -c "INSERT INTO network_1.event_photo (local_identifier, expedition_id, data, parent_identifier) (SELECT local_identifier, expedition_id, data, parent_identifier FROM project_${id}.event_photo)";
    psql -d biscicol_network -c "INSERT INTO network_1.sample (local_identifier, expedition_id, data, parent_identifier) (SELECT local_identifier, expedition_id, data, parent_identifier FROM project_${id}.sample)";
    psql -d biscicol_network -c "INSERT INTO network_1.sample_photo (local_identifier, expedition_id, data, parent_identifier) (SELECT local_identifier, expedition_id, data, parent_identifier FROM project_${id}.sample_photo)";
    psql -d biscicol_network -c "INSERT INTO network_1.tissue (local_identifier, expedition_id, data, parent_identifier) (SELECT local_identifier, expedition_id, data, parent_identifier FROM project_${id}.tissue)";
    psql -d biscicol_network -c "INSERT INTO network_1.fastasequence (local_identifier, expedition_id, data, parent_identifier) (SELECT local_identifier, expedition_id, data, parent_identifier FROM project_${id}.fastasequence)";
    psql -d biscicol_network -c "INSERT INTO network_1.fastqMetadata (local_identifier, expedition_id, data, parent_identifier) (SELECT local_identifier, expedition_id, data, parent_identifier FROM project_${id}.fastqMetadata)";
    psql -d biscicol_network -c "DROP SCHEMA project_${id} CASCADE";
done
