DROP TRIGGER project_config_history on projects;

DROP TABLE IF EXISTS project_configurations;
CREATE TABLE project_configurations (
  id SERIAL PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  config JSONB NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_id INTEGER NOT NULL REFERENCES users (id),
  network_id INTEGER NOT NULL REFERENCES networks (id),
  network_approved BOOLEAN NOT NULL DEFAULT '0'
);
CREATE INDEX project_configurations_user_id_idx ON project_configurations (user_id);
CREATE TRIGGER update_project_configurations_modtime BEFORE INSERT OR UPDATE ON project_configurations FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_project_configurations_createdtime BEFORE INSERT ON project_configurations FOR EACH ROW EXECUTE PROCEDURE set_created_column();

DROP TABLE IF EXISTS project_config_history;
CREATE TABLE project_config_history
(
  id bigserial primary key,
  user_name text,
  ts TIMESTAMP WITH TIME ZONE NOT NULL,
  action TEXT NOT NULL CHECK (action IN ('I','D','U','T')),
  config jsonb,
  project_configuration_id INTEGER NOT NULL REFERENCES project_configurations (id) ON DELETE CASCADE
);

CREATE TRIGGER project_config_history AFTER INSERT OR UPDATE ON project_configurations FOR EACH ROW EXECUTE PROCEDURE config_history('project_config_history', 'project_configuration_id');

-- NOTE: This will need some manual cleanup
INSERT INTO project_configurations (config, name, user_id, network_id) SELECT config, project_code, user_id, network_id from projects where id in (1,2);

-- add existing projects to network
alter table projects drop column config;
alter TABLE projects add COLUMN config_id INTEGER NOT NULL REFERENCES project_configurations (id) DEFAULT 1;
alter table projects alter column config_id drop default;
alter table projects alter column project_code drop not null;
alter table projects alter column project_title set not null;
CREATE UNIQUE INDEX projects_project_title_idx ON projects (project_title);
update project_configurations set user_id = 1, network_approved = true;
update project_configurations set name = 'Biocode' where id = ? ;
