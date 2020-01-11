-- For Migration on 10/16/19

CREATE OR REPLACE FUNCTION set_project_last_modified()
  RETURNS TRIGGER AS $body$
BEGIN
  IF TG_WHEN <> 'AFTER'
  THEN
    RAISE EXCEPTION 'set_project_last_modified() may only run as an AFTER trigger';
  END IF;

  IF TG_LEVEL = 'STATEMENT'
  THEN
    RAISE EXCEPTION 'set_project_last_modified() does not support being a STATEMENT trigger';
  END IF;

  EXECUTE
  'UPDATE projects set latest_data_modification = CURRENT_TIMESTAMP where id = (select project_id from expeditions where id = '
  || quote_literal(NEW.expedition_id) || ')';

  RETURN NULL;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER;

COMMENT ON FUNCTION set_project_last_modified() IS $body$
Update the project's latest_data_modification timestamp when a record is inserted, updated, or deleted.
$body$;

ALTER TABLE projects
  ADD COLUMN latest_data_modification TIMESTAMP;

DO $$
DECLARE
  t TEXT;
BEGIN
  FOR t IN SELECT table_schema || '.' || table_name
           FROM information_schema.tables
           WHERE table_schema = 'network_1' AND table_type = 'BASE TABLE' AND table_name <> 'audit_table' LOOP

    RAISE NOTICE 'Setting trigger for: %', t;

    EXECUTE 'CREATE TRIGGER set_project_last_modified AFTER INSERT OR DELETE OR UPDATE ON ' || t ||
            ' FOR EACH ROW EXECUTE PROCEDURE set_project_last_modified()';
  END LOOP;
END;
$$;

-- set latest_data_modification on all projects

DO $$
DECLARE
  p  INT;
  t  TEXT;
  ts TIMESTAMP;
BEGIN
  FOR p IN SELECT id
           FROM projects
  LOOP
    ts = to_timestamp(0);
    FOR t IN SELECT table_schema || '.' || table_name
             FROM information_schema.tables
             WHERE table_schema = 'network_1' AND table_type = 'BASE TABLE' AND table_name <> 'audit_table' LOOP
      --       RAISE NOTICE 'previous ts: %', ts;
      EXECUTE 'SELECT greatest(coalesce(modified, to_timestamp(0)), ' || quote_nullable(ts) || ') as ts from ' || t ||
              ' where expedition_id in (select id from expeditions where project_id = ' || quote_literal(p) ||
              ') union all select ' || quote_nullable(ts) || ' as ts' ||
              ' where not exists (select 1 from ' || t ||
              ' where expedition_id in (select id from expeditions where project_id = ' || quote_literal(p) ||
              ')) order by ts desc limit 1'
      INTO ts;
      --             RAISE NOTICE 'Current ts: % - %', t, ts;

    END LOOP;

    IF ts > to_timestamp(0)
    THEN
--       RAISE NOTICE 'Latest Modification: % - %', p, ts;
      EXECUTE 'update projects set latest_data_modification = ' || quote_literal(ts) || ' where id = ' ||
              quote_literal(p);
    ELSE
      RAISE NOTICE 'NO data for project: %', p;
    END IF;
  END LOOP;
END;
$$;
