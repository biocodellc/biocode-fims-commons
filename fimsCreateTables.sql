/**
* SQL for Fims postgresql tables
*
* NOTE: some triggers have been commented out that are required. They are commented out incase you are
* migrating/restoring any data, as these trigger should not be run during a restore/migration.
*
* If you are restoring/migrating data:
*   1. run this script
*   2. import data
*   3. run triggers.sql script
*
* If you are creating a new instance, you can either uncomment the triggers here, or run this script
* followed by the trigger.sql script
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

CREATE OR REPLACE FUNCTION project_config_history() RETURNS TRIGGER AS $body$
DECLARE
  audit_table_name text;
  audit_session_user text;
  config jsonb;
BEGIN
  IF TG_WHEN <> 'AFTER' THEN
    RAISE EXCEPTION 'project_config_history() may only run as an AFTER trigger';
  END IF;

  IF TG_LEVEL = 'STATEMENT' THEN
    RAISE EXCEPTION 'project_config_history() does not support being a STATEMENT trigger';
  END IF;

  audit_table_name = 'project_config_history';

  audit_session_user = get_fims_user();
  IF NULLIF(audit_session_user, '') IS NULL THEN
    audit_session_user = 'postgres_role: ' || session_user::text;
  END IF;

  RAISE INFO 'OP_CODE %', TG_OP;
  IF TG_OP = 'UPDATE' THEN
    config = OLD.config;
    IF OLD.config = NEW.config THEN
      -- All changed fields are ignored. Skip this update.
      RETURN NULL;
    END IF;
--   ELSIF TG_OP = 'DELETE' THEN
--     config = OLD.config;
  ELSIF TG_OP = 'INSERT' THEN
    config = NEW.config;
  ELSE
    RAISE EXCEPTION '[project_config_history] - Trigger func added as trigger for unhandled case: %, %',TG_OP, TG_LEVEL;
    RETURN NULL;
  END IF;

  EXECUTE 'INSERT INTO ' || audit_table_name || ' (user_name, ts, action, config, project_id) ' ||
   'VALUES (' || quote_literal(audit_session_user) || ', CURRENT_TIMESTAMP, ' || quote_literal(substring(TG_OP,1,1)) ||
   ', ' || quote_literal(config) || ', ' || quote_literal(NEW.id) || ')';

  RETURN NULL;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER;

COMMENT ON FUNCTION project_config_history() IS $body$
Track changes to the projects.config column.

This will create an entry in the project_config_history for each config that is updated or inserted.
This is useful for creating a history of all changes to project configs.
$body$;

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


DROP TABLE IF EXISTS projects;
CREATE DOMAIN project_code as text CHECK (length(value) <= 10);

CREATE TABLE projects (
  id SERIAL PRIMARY KEY NOT NULL,
  project_code project_code NOT NULL,
  project_title TEXT,
  project_url TEXT NOT NULL,
  description TEXT,
  config JSONB NOT NULL,
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
-- CREATE TRIGGER set_projects_createdtime BEFORE INSERT ON projects FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN projects.project_code is 'The short name for this project';
COMMENT ON COLUMN projects.project_url is 'Where this project is located on the web';
COMMENT ON COLUMN projects.public is 'Whether or not this is a public project?';

DROP TABLE IF EXISTS project_config_history;
CREATE TABLE project_config_history
  (
    id bigserial primary key,
    user_name text,
    ts TIMESTAMP WITH TIME ZONE NOT NULL,
    action TEXT NOT NULL CHECK (action IN ('I','D','U', 'T')),
    config jsonb,
    project_id INTEGER NOT NULL REFERENCES projects (id) ON DELETE CASCADE
  );

-- CREATE TRIGGER config_history AFTER INSERT OR UPDATE ON projects FOR EACH ROW EXECUTE PROCEDURE project_config_history();

COMMENT ON COLUMN project_config_history.user_name is 'user who made the change';
COMMENT ON COLUMN project_config_history.ts is 'timestamp the change happened';
COMMENT ON COLUMN project_config_history.action is 'INSERT, DELETE, UPDATE, or TRUNCATE';
COMMENT ON COLUMN project_config_history.config is 'For INSERT this is the new config values. For DELETE and UPDATE it is the old config values.';

DROP TABLE IF EXISTS user_projects;

CREATE TABLE user_projects (
  project_id INTEGER NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT user_projects_user_id_project_id_uniq UNIQUE (user_id, project_id)
);

CREATE INDEX user_projects_project_id_idx ON projects (id);
CREATE INDEX user_projects_user_id_idx ON users (id);

DROP TABLE IF EXISTS user_invite;

CREATE TABLE user_invite (
  id UUID NOT NULL PRIMARY KEY ,
  project_id INTEGER NOT NULL REFERENCES projects (id),
  invited_by_id INTEGER NOT NULL REFERENCES users (id),
  email TEXT NOT NULL UNIQUE,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- CREATE TRIGGER set_user_invite_createdtime BEFORE INSERT ON user_invite FOR EACH ROW EXECUTE PROCEDURE set_created_column();

CREATE OR REPLACE FUNCTION delete_expired_user_invites()
  RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM user_invite WHERE created < now() - INTERVAL '7 DAYS';
  RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER delete_expired_user_invites AFTER INSERT ON user_invite EXECUTE PROCEDURE delete_expired_user_invites();

DROP TABLE IF EXISTS expeditions;

CREATE TABLE expeditions (
  id SERIAL NOT NULL PRIMARY KEY,
  project_id INTEGER NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  expedition_code TEXT NOT NULL,
  expedition_title TEXT,
  identifier TEXT NOT NULL,
  visibility TEXT NOT NULL,
  user_id INTEGER NOT NULL REFERENCES users (id),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  public BOOLEAN NOT NULL DEFAULT '1',
  CONSTRAINT expeditions_code_project_id_uniq UNIQUE (expedition_code, project_id)
);

CREATE INDEX expeditions_project_id_idx ON expeditions (project_id);

CREATE TRIGGER update_expeditions_modtime BEFORE INSERT OR UPDATE ON expeditions FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
-- CREATE TRIGGER set_expeditions_createdtime BEFORE INSERT ON expeditions FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN expeditions.expedition_code is 'The short name for this expedition';
COMMENT ON COLUMN expeditions.public is 'Whether or not this is a public expedition';

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

-- CREATE TRIGGER set_oouth_nonces_createdtime BEFORE INSERT ON oauth_nonces FOR EACH ROW EXECUTE PROCEDURE set_created_column();

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

-- CREATE TRIGGER set_oauth_tokens_createdtime BEFORE INSERT ON oauth_tokens FOR EACH ROW EXECUTE PROCEDURE set_created_column();

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
  sheet_name TEXT NOT NULL,
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
