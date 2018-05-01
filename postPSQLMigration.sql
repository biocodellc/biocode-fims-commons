update project_templates set columns = replace(columns, '\', '"');
alter table project_templates alter COLUMN columns TYPE jsonb USING columns::jsonb;
update project_templates set worksheet = 'Samples';
alter table project_templates alter column worksheet set NOT NULL;
update expeditions e set identifier = (select b.identifier from bcids_tmp b join expedition_bcids eb on b.id = eb.bcid_id join expeditions e2 on eb.expedition_id = e.id where b.resource_type = 'http://purl.org/dc/dcmitype/Collection' and e.id = e2.id);
update expeditions set visibility = 'ANYONE' where public = 't';
update expeditions set visibility = 'EXPEDITION' where public = 'f';
ALTER TABLE expeditions ALTER COLUMN visibility SET NOT NULL;
ALTER TABLE expeditions ALTER COLUMN identifier SET NOT NULL;
update projects set config = '{}';

-- CREATE TRIGGER set_bcids_createdtime BEFORE INSERT ON bcids FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_projects_createdtime BEFORE INSERT ON projects FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_user_invite_createdtime BEFORE INSERT ON user_invite FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_expeditions_createdtime BEFORE INSERT ON expeditions FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_oouth_nonces_createdtime BEFORE INSERT ON oauth_nonces FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_oauth_tokens_createdtime BEFORE INSERT ON oauth_tokens FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER config_history AFTER INSERT OR UPDATE ON projects FOR EACH ROW EXECUTE PROCEDURE project_config_history();
