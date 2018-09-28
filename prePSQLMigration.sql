alter table project_templates alter column worksheet DROP NOT NULL;
ALTER TABLE expeditions ALTER COLUMN visibility DROP NOT NULL;
ALTER TABLE expeditions ALTER COLUMN identifier DROP NOT NULL;
ALTER TABLE projects ALTER COLUMN config DROP NOT NULL;
ALTER TABLE projects ADD COLUMN validation_xml TEXT NOT NULL;
alter table project_templates alter COLUMN columns TYPE text;

DROP TABLE IF EXISTS bcids_tmp;

CREATE TABLE bcids_tmp (
  id SERIAL PRIMARY KEY NOT NULL,
  ezid_made BOOLEAN NOT NULL DEFAULT '0',
  ezid_request BOOLEAN NOT NULL DEFAULT '1',
  identifier TEXT,
  user_id INTEGER NOT NULL REFERENCES users (id),
  doi TEXT,
  title TEXT,
  web_address TEXT,
  graph TEXT,
  source_file TEXT,
  resource_type TEXT NOT NULL,
  sub_resource_type TEXT,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS expedition_bcids;

CREATE TABLE expedition_bcids (
  expedition_id INTEGER NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
  bcid_id INTEGER NOT NULL REFERENCES bcids_tmp (id)
);

CREATE INDEX expedition_bcids_expedition_id_idx ON expedition_bcids (expedition_id);
CREATE INDEX expedition_bcids_bcid_id_idx ON expedition_bcids (bcid_id);
