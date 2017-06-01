/**
* SQL for Fims postgresql tables
*/

CREATE OR REPLACE FUNCTION update_modified_column()
  RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' OR row(NEW.*) IS DISTINCT FROM row(OLD.*) THEN
    NEW.modified = now();
    RETURN NEW;
  ELSE
    RETURN OLD;
  END IF;
END;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION set_created_column()
  RETURNS TRIGGER AS $$
BEGIN
  NEW.created = now();
  RETURN NEW;
END;
$$ language 'plpgsql';

DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id SERIAL PRIMARY KEY NOT NULL,
  username TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL,
  email TEXT,
  first_name TEXT,
  last_name TEXT,
  institution TEXT,
  has_set_password BOOLEAN NOT NULL DEFAULT '0',
  password_reset_token TEXT,
  password_reset_expiration TIMESTAMP
);

COMMENT ON COLUMN users.password_reset_token is 'Unique token used to reset a users password';
COMMENT ON COLUMN users.password_reset_expiration is 'time when the reset token expires';

DROP TABLE IF EXISTS bcids;

CREATE TABLE bcids (
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

CREATE INDEX bcids_identifier_idx on bcids (identifier);
CREATE INDEX bcids_userId_idx on bcids (user_id);
CREATE INDEX bcids_resourceType_idx on bcids (resource_type);

CREATE TRIGGER update_bcids_modtime BEFORE INSERT OR UPDATE ON bcids FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_bcids_createdtime BEFORE INSERT ON bcids FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN bcids.ezid_made is 'indicates if EZID has been made';
COMMENT ON COLUMN bcids.ezid_request is 'indicates if we want system to request EZID, all bcids by default get an EZID request';
COMMENT ON COLUMN bcids.identifier is 'ark:/1234/ab1';
COMMENT ON COLUMN bcids.user_id is 'who created this data';
COMMENT ON COLUMN bcids.title is 'title for this bcid';
COMMENT ON COLUMN bcids.web_address is 'the target URL for this bcid';
COMMENT ON COLUMN bcids.graph is 'A reference to a graph, used by the biocode-fims expedition for storing graph references for a particular bcid';
COMMENT ON COLUMN bcids.source_file is 'The name of the source file for this bcid. This is useful for dataset backups.';
COMMENT ON COLUMN bcids.resource_type is 'resource type for this bcid, stored as a URI';
COMMENT ON COLUMN bcids.sub_resource_type is 'sub resource type for this bcid, stored as a URI';
COMMENT ON COLUMN bcids.created is 'timestamp of insertion';
COMMENT ON COLUMN bcids.modified is 'timestamp of last update';

DROP TABLE IF EXISTS projects;
CREATE DOMAIN project_code as text CHECK (length(value) <= 10);

CREATE TABLE projects (
  id SERIAL PRIMARY KEY NOT NULL,
  project_code project_code NOT NULL,
  project_title TEXT,
  project_url TEXT NOT NULL,
  description TEXT,
  validation_xml TEXT NOT NULL,
  config JSONB,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id INTEGER NOT NULL REFERENCES users (id),
  public BOOLEAN NOT NULL DEFAULT '1'
);

CREATE INDEX projects_user_id_idx ON projects (user_id);
CREATE INDEX projects_public_idx ON projects (public);
CREATE INDEX projects_project_url_idx ON projects (project_url);
CREATE UNIQUE INDEX projects_project_code_idx ON projects (project_code);
ALTER TABLE projects ADD CONSTRAINT projects_project_code_uniq UNIQUE USING INDEX projects_project_code_idx;

CREATE TRIGGER update_projects_modtime BEFORE INSERT OR UPDATE ON projects FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_projects_createdtime BEFORE INSERT ON projects FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN projects.project_code is 'The short name for this project';
COMMENT ON COLUMN projects.project_url is 'Where this project is located on the web';
COMMENT ON COLUMN projects.public is 'Whether or not this is a public project?';

DROP TABLE IF EXISTS user_projects;

CREATE TABLE user_projects (
  project_id INTEGER NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT user_projects_user_id_project_id_uniq UNIQUE (user_id, project_id)
);

CREATE INDEX user_projects_project_id_idx ON projects (id);
CREATE INDEX user_projects_user_id_idx ON users (id);

DROP TABLE IF EXISTS expeditions;

CREATE TABLE expeditions (
  id SERIAL NOT NULL PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  expedition_code TEXT NOT NULL,
  expedition_title TEXT,
  identifier TEXT,
  visibility TEXT NOT NULL,
  user_id INTEGER NOT NULL REFERENCES users (id),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  public BOOLEAN NOT NULL DEFAULT '1',
  CONSTRAINT expeditions_code_project_id_uniq UNIQUE (expedition_code, project_id)
);

CREATE INDEX expeditions_project_id_idx ON expeditions (project_id);

CREATE TRIGGER update_expeditions_modtime BEFORE INSERT OR UPDATE ON expeditions FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_expeditions_createdtime BEFORE INSERT ON expeditions FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN expeditions.expedition_code is 'The short name for this expedition';
COMMENT ON COLUMN expeditions.public is 'Whether or not this is a public expedition';

DROP TABLE IF EXISTS expedition_bcids;

CREATE TABLE expedition_bcids (
  expedition_id INTEGER NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
  bcid_id INTEGER NOT NULL REFERENCES bcids (id)
);

CREATE INDEX expedition_bcids_expedition_id_idx ON expedition_bcids (expedition_id);
CREATE INDEX expedition_bcids_bcid_id_idx ON expedition_bcids (bcid_id);

DROP TABLE IF EXISTS entity_identifiers;

CREATE TABLE entity_identifiers (
  id SERIAL,
  expedition_id INTEGER NOT NULL REFERENCES expeditions (id) ON DELETE CASCADE,
  concept_alias TEXT NOT NULL,
  identifier TEXT NOT NULL,
  CONSTRAINT entitiy_identifiers_expediton_id_concept_alias_uniq UNIQUE (expedition_id, concept_alias)
);

CREATE INDEX entity_identifiers_expedition_id ON entity_identifiers (expedition_id);

DROP TABLE IF EXISTS oauth_clients;

CREATE TABLE oauth_clients (
  id TEXT NOT NULL PRIMARY KEY,
  client_secret TEXT NOT NULL,
  callback TEXT NOT NULL
);

COMMENT ON COLUMN oauth_clients.id is 'the public unique client id';
COMMENT ON COLUMN oauth_clients.client_secret is 'the private shared secret';
COMMENT ON COLUMN oauth_clients.callback is 'The callback url of the client app';

DROP TABLE IF EXISTS oauth_nonces;

CREATE TABLE oauth_nonces (
  id SERIAL NOT NULL PRIMARY KEY,
  client_id TEXT NOT NULL REFERENCES oauth_clients (id) ON DELETE CASCADE,
  code TEXT NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  redirect_uri TEXT NOT NULL,
  CONSTRAINT oauth_nonces_code_client_id_uniq UNIQUE (client_id, code)
);

CREATE INDEX oauth_nonces_code_idx ON oauth_nonces (code);

CREATE TRIGGER set_oouth_nonces_createdtime BEFORE INSERT ON oauth_nonces FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN oauth_nonces.code is 'The generated code the client app can exchange for an access token';
COMMENT ON COLUMN oauth_nonces.redirect_uri is 'The redirectUri associated with this code';

DROP TABLE IF EXISTS oauth_tokens;

CREATE TABLE oauth_tokens (
  id SERIAL NOT NULL PRIMARY KEY,
  client_id TEXT NOT NULL REFERENCES oauth_clients (id) ON DELETE CASCADE,
  token TEXT NOT NULL UNIQUE,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  refresh_token TEXT NOT NULL
);

CREATE INDEX oauth_tokens_token_idx on oauth_tokens (token);
CREATE INDEX oauth_tokens_refresh_token_idx on oauth_tokens (refresh_token);

CREATE TRIGGER set_oauth_tokens_createdtime BEFORE INSERT ON oauth_tokens FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN oauth_tokens.token is 'The generated token used by the client app';
COMMENT ON COLUMN oauth_tokens.refresh_token is 'The generated token used to gain a new access_token';

CREATE OR REPLACE FUNCTION delete_expired_oAuthTokens()
  RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM oauth_tokens WHERE created < now() - INTERVAL '2 DAYS';
  RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER delete_expired_oauth_tokens AFTER INSERT ON oauth_tokens EXECUTE PROCEDURE delete_expired_oAuthTokens();

CREATE OR REPLACE FUNCTION time_to_sec(t INTERVAL)
  RETURNS integer AS
$BODY$
DECLARE
  s INTEGER;
BEGIN
  SELECT (EXTRACT (EPOCH FROM t)) INTO s;
  RETURN s;
END;
$BODY$
LANGUAGE 'plpgsql';


-- DROP TABLE IF EXISTS ldapNonces;
--
-- CREATE TABLE ldapNonces (
--   ldapNonceId SERIAL NOT NULL PRIMARY KEY,
--   username TEXT NOT NULL UNIQUE ,
--   s TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp of the ldap user lockout or when the users first login attempt was',
--   attempts int(11) UNSIGNED NOT NULL DEFAULT '1' COMMENT 'the number of ldap login attempts',
--   PRIMARY KEY (ldapNonceId),
--   UNIQUE KEY ldapNonces_usernamex (username)
-- );
--
-- COMMENT ON COLUMN ldapNonces.username is 'The username of the login attempt';

DROP TABLE IF EXISTS project_templates;

CREATE TABLE project_templates (
  id SERIAL NOT NULL,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  project_id INTEGER NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  attribute_uris jsonb NOT NULL,
  CONSTRAINT project_templates_name_project_id_uniq UNIQUE (name, project_id)
);

CREATE INDEX project_templates_project_id_idx ON project_templates (project_id);

COMMENT ON COLUMN project_templates.name is 'The name of the template';
COMMENT ON COLUMN project_templates.attribute_uris is 'The array of uris for this template';

-- Function that is added to each entity table in a trigger when created
CREATE OR REPLACE FUNCTION entity_tsv_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
begin
  new.tsv = to_tsvector(string_agg((j).value::text, ' ')) from jsonb_each(new.data) as j;
  return new;
end
$function$;

CREATE OR REPLACE FUNCTION get_fims_user() RETURNS text AS $$
  SELECT current_setting('fims.username', TRUE);
$$ LANGUAGE sql;

COMMENT ON FUNCTION get_fims_user() IS $body$
Fetches the value of current_setting('fims.username', true).
This value can be set by calling SET LOCAL "fims.username" = 'user';
This is useful to retrieve the logged in user from the fims applicaton in a postgresql trigger.
$body$;

CREATE OR REPLACE FUNCTION jsonb_diff_val(new JSONB, old JSONB)
  RETURNS JSONB AS $$
DECLARE
  result JSONB;
  object_result JSONB;
  v RECORD;
BEGIN
  IF jsonb_typeof(new) = 'null' or new = '{}'::jsonb THEN
    RAISE INFO 'Returning old';
    RETURN old;
  END IF;

  result = new;
  FOR v IN SELECT * FROM jsonb_each(old) LOOP
    IF jsonb_typeof(new->v.key) = 'object' AND jsonb_typeof(old->v.key) = 'object'THEN
      object_result = jsonb_diff_val(new->v.key, old->v.key);
      IF object_result = '{}'::jsonb THEN
        result = result - v.key; --if empty remove
      ELSE
        result = result || jsonb_build_object(v.key,object_result);
      END IF;
    ELSIF new->v.key = old->v.key THEN
      result = result - v.key;
    ELSIF result ? v.key THEN CONTINUE;
    ELSE
      result = result || jsonb_build_object(v.key, null);
    END IF;
  END LOOP;

  RETURN result;

END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION jsonb_diff_val(jsonb, jsonb) IS $body$
Generates a jsonb diff object where the 1st arg differs from the second. If the 1st arg does not include a
key in the 2nd arg, then the diff will contain '{key}: "null"'
$body$;

CREATE OR REPLACE FUNCTION entity_history() RETURNS TRIGGER AS $body$
DECLARE
  audit_table_name text;
  audit_session_user text;
  row_data jsonb;
  changed_fields jsonb;
BEGIN
  IF TG_WHEN <> 'AFTER' THEN
    RAISE EXCEPTION 'entity_history() may only run as an AFTER trigger';
  END IF;

  IF TG_LEVEL = 'STATEMENT' THEN
    RAISE EXCEPTION 'entity_history() does not support being a STATEMENT trigger';
  END IF;

  audit_table_name = TG_TABLE_SCHEMA::text || '.audit_table';

  audit_session_user = get_fims_user();
  IF NULLIF(audit_session_user, '') IS NULL THEN
    audit_session_user = 'postgres_role: ' || session_user::text;
  END IF;

  IF TG_OP = 'UPDATE' THEN
    row_data = to_jsonb(OLD.*) - 'tsv' - 'changed' - 'modified';
    changed_fields =  jsonb_diff_val(to_jsonb(NEW.*) - 'tsv' - 'changed' - 'modified', row_data);
    IF changed_fields = '{}'::jsonb THEN
      -- All changed fields are ignored. Skip this update.
      RETURN NULL;
    END IF;
  ELSIF TG_OP = 'DELETE' THEN
    row_data = to_jsonb(OLD.*) - 'tsv' - 'changed' - 'modified';
  ELSIF TG_OP = 'INSERT' THEN
    row_data = to_jsonb(NEW.*) - 'tsv' - 'changed' - 'modified';
  ELSE
    RAISE EXCEPTION '[entity_history] - Trigger func added as trigger for unhandled case: %, %',TG_OP, TG_LEVEL;
    RETURN NULL;
  END IF;

  EXECUTE 'INSERT INTO ' || audit_table_name || ' (table_name, user_name, ts, action, row_data, changed_fields) ' ||
   'VALUES (' || quote_literal(TG_TABLE_SCHEMA::text || '.' || TG_TABLE_NAME::text) || ', ' || quote_literal(audit_session_user)
   || ', CURRENT_TIMESTAMP, ' || quote_literal(substring(TG_OP,1,1)) || ', ' || quote_literal(row_data) || ', '
   || quote_nullable(changed_fields) || ')';

  RETURN NULL;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER;

COMMENT ON FUNCTION entity_history() IS $body$
Track changes to a table at the row level.

This will create an entry in the SCHEMA.audit_table for each row that is updated, inserted, or deleted.
This is useful for creating a history of all changes to entities.
$body$;
