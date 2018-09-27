drop trigger config_history on projects;
drop function project_config_history;

CREATE OR REPLACE FUNCTION config_history() RETURNS TRIGGER AS $body$
DECLARE
  audit_table_name text;
  audit_table_foreign_key_column text;
  audit_session_user text;
  config jsonb;
BEGIN
  IF TG_WHEN <> 'AFTER' THEN
    RAISE EXCEPTION 'config_history() may only run as an AFTER trigger';
  END IF;

  IF TG_LEVEL = 'STATEMENT' THEN
    RAISE EXCEPTION 'config_history() does not support being a STATEMENT trigger';
  END IF;

  IF TG_NARGS <> 2 THEN
    RAISE EXCEPTION 'config_history() trigger must be called with 2 parameters. (audit_table_name text, audit_table_foreign_key_column text)';
  END IF;

--   audit_table_name = 'config_history';
  audit_table_name = TG_ARGV[0];
  audit_table_foreign_key_column = TG_ARGV[1];

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
    RAISE EXCEPTION '[config_history] - Trigger func added as trigger for unhandled case: %, %',TG_OP, TG_LEVEL;
    RETURN NULL;
  END IF;

  EXECUTE 'INSERT INTO ' || audit_table_name || ' (user_name, ts, action, config, ' || audit_table_foreign_key_column || ') ' ||
          'VALUES (' || quote_literal(audit_session_user) || ', CURRENT_TIMESTAMP, ' || quote_literal(substring(TG_OP,1,1)) ||
          ', ' || quote_literal(config) || ', ' || quote_literal(NEW.id) || ')';

  RETURN NULL;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER;

COMMENT ON FUNCTION config_history() IS $body$
Track changes to the projects/networks.config column.

This will create an entry in the config_history for each config that is updated or inserted.
This is useful for creating a history of all changes to project/network configs.
$body$;

CREATE OR REPLACE FUNCTION create_network(network_id integer, network_title text, user_id integer)
  RETURNS VOID AS
$func$
DECLARE
  db_owner text;
BEGIN
  db_owner := (SELECT pg_catalog.pg_get_userbyid(d.datdba) FROM pg_catalog.pg_database d WHERE d.datname = (select current_database ()) ORDER BY 1);

  IF network_id IS NULL THEN
    EXECUTE format(E'insert into networks(title, config, user_id) VALUES (\'%s\', \'{}\', %s)', network_title, user_id);
    network_id := (SELECT id from networks order by id desc limit 1);
  ELSE
    EXECUTE format(E'insert into networks(id, title, config, user_id) VALUES (%s, \'%s\', \'{}\', %s)', network_id, network_title, user_id);
  END IF;


  EXECUTE format('CREATE SCHEMA network_%s', network_id);

  EXECUTE format(E'
        CREATE TABLE network_%s.audit_table
        (
          event_id bigserial primary key,
          table_name text not null, -- table the change was made to
          user_name text, -- user who made the change
          ts TIMESTAMP WITH TIME ZONE NOT NULL, -- timestamp the change happened
          action TEXT NOT NULL CHECK (action IN (\'I\',\'D\',\'U\', \'T\')), -- INSERT, DELETE, UPDATE, or TRUNCATE
          row_data jsonb, -- For INSERT this is the new row values. For DELETE and UPDATE it is the old row values.
          changed_fields jsonb -- Null except UPDATE events. This is the result of jsonb_diff_val(NEW data, OLD data)
        );
        ', network_id);

  EXECUTE format('alter schema network_%s owner to %s', network_id, db_owner);
  EXECUTE format('alter table network_%s.audit_table owner to %s', network_id, db_owner);

  IF EXISTS (
      SELECT                       -- SELECT list can stay empty for this
      FROM   pg_catalog.pg_roles
      WHERE  rolname = 'readaccess') THEN

    EXECUTE format('GRANT USAGE ON SCHEMA network_%s TO readaccess', network_id);
    EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA network_%s TO readaccess', network_id);
    EXECUTE format('GRANT SELECT ON ALL SEQUENCES IN SCHEMA network_%s TO readaccess', network_id);
  END IF;

END
$func$  LANGUAGE plpgsql;

DROP TABLE IF EXISTS networks;
CREATE TABLE networks (
  id SERIAL PRIMARY KEY NOT NULL,
  title TEXT,
  description TEXT,
  config JSONB NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id INTEGER NOT NULL REFERENCES users (id)
);

CREATE INDEX networks_user_id_idx ON networks (user_id);

CREATE TRIGGER update_networks_modtime BEFORE INSERT OR UPDATE ON networks FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_networks_createdtime BEFORE INSERT ON networks FOR EACH ROW EXECUTE PROCEDURE set_created_column();

DROP TABLE IF EXISTS network_config_history;
CREATE TABLE network_config_history
(
  id bigserial primary key,
  user_name text,
  ts TIMESTAMP WITH TIME ZONE NOT NULL,
  action TEXT NOT NULL CHECK (action IN ('I','D','U', 'T')),
  config jsonb,
  network_id INTEGER NOT NULL REFERENCES networks (id) ON DELETE CASCADE
);

CREATE TRIGGER network_config_history AFTER INSERT OR UPDATE ON networks FOR EACH ROW EXECUTE PROCEDURE config_history('network_config_history', 'network_id');

COMMENT ON COLUMN network_config_history.user_name is 'user who made the change';
COMMENT ON COLUMN network_config_history.ts is 'timestamp the change happened';
COMMENT ON COLUMN network_config_history.action is 'INSERT, DELETE, UPDATE, or TRUNCATE';
COMMENT ON COLUMN network_config_history.config is 'For INSERT this is the new config values. For DELETE and UPDATE it is the old config values.';

CREATE TRIGGER project_config_history AFTER INSERT OR UPDATE ON projects FOR EACH ROW EXECUTE PROCEDURE config_history('project_config_history', 'project_id');

-- create network
select create_network(1, 'GeOMe Network', 1);

-- add existing projects to network
alter TABLE projects add COLUMN network_id INTEGER NOT NULL REFERENCES networks (id) DEFAULT 1;
alter table projects alter column network_id drop default;

-- rename project_templates to worksheet_templates
alter table project_templates rename to worksheet_templates;

update projects set config = '{}';

-- now you need to upload the network config. After successful upload, run the bash script networks_migrations_post_config.sh
-- curl -X PUT -H 'Content-Type: application/json' -d @geome.json http://localhost:8080/networks/1/config?access_token=jCVcTBzTnyxpqEsmNs9r

