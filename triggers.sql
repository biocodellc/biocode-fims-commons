ALTER TABLE expeditions ALTER COLUMN visibility NOT NULL;
CREATE TRIGGER set_bcids_createdtime BEFORE INSERT ON bcids FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_projects_createdtime BEFORE INSERT ON projects FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_user_invite_createdtime BEFORE INSERT ON user_invite FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_expeditions_createdtime BEFORE INSERT ON expeditions FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_oouth_nonces_createdtime BEFORE INSERT ON oauth_nonces FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER set_oauth_tokens_createdtime BEFORE INSERT ON oauth_tokens FOR EACH ROW EXECUTE PROCEDURE set_created_column();
CREATE TRIGGER config_history AFTER INSERT OR UPDATE ON projects FOR EACH ROW EXECUTE PROCEDURE project_config_history();
