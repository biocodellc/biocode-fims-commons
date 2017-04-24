/**
* SQL for Fims postgresql tables
*/

CREATE OR REPLACE FUNCTION update_modified_column()
  RETURNS TRIGGER AS $$
BEGIN
  IF row(NEW.*) IS DISTINCT FROM row(OLD.*) THEN
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
  userId SERIAL PRIMARY KEY NOT NULL,
  username TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL,
  email TEXT DEFAULT NULL,
  firstName TEXT DEFAULT NULL,
  lastName TEXT DEFAULT NULL,
  institution TEXT DEFAULT NULL,
  hasSetPassword BOOLEAN NOT NULL DEFAULT '0',
  passwordResetToken TEXT DEFAULT NULL,
  passwordResetExpiration TIMESTAMP DEFAULT NULL
);

COMMENT ON COLUMN users.passwordResetToken is 'Unique token used to reset a users password';
COMMENT ON COLUMN users.passwordResetExpiration is 'time when the reset token expires';

DROP TABLE IF EXISTS bcids;

CREATE TABLE bcids (
  bcidId SERIAL PRIMARY KEY NOT NULL,
  ezidMade BOOLEAN NOT NULL DEFAULT '0',
  ezidRequest BOOLEAN NOT NULL DEFAULT '1',
  identifier TEXT NULL,
  userId INTEGER NOT NULL REFERENCES users (userId),
  doi TEXT,
  title TEXT,
  webAddress TEXT,
  graph TEXT,
  sourceFile TEXT,
  resourceType TEXT NOT NULL,
  subResourceType TEXT NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX bcids_identifier_idx on bcids (identifier);
CREATE INDEX bcids_userId_idx on bcids (userId);
CREATE INDEX bcids_resourceType_idx on bcids (resourceType);

CREATE TRIGGER update_bcids_modtime BEFORE UPDATE ON bcids FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_bcids_createdtime BEFORE INSERT ON bcids FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN bcids.ezidMade is 'indicates if EZID has been made';
COMMENT ON COLUMN bcids.ezidRequest is 'indicates if we want system to request EZID, all bcids by default get an EZID request';
COMMENT ON COLUMN bcids.identifier is 'ark:/1234/ab1';
COMMENT ON COLUMN bcids.userId is 'who created this data';
COMMENT ON COLUMN bcids.title is 'title for this bcid';
COMMENT ON COLUMN bcids.webAddress is 'the target URL for this bcid';
COMMENT ON COLUMN bcids.webAddress is 'A reference to a graph, used by the biocode-fims expedition for storing graph references for a particular bcid';
COMMENT ON COLUMN bcids.sourceFile is 'The name of the source file for this bcid. This is useful for dataset backups.';
COMMENT ON COLUMN bcids.resourceType is 'resource type for this bcid, stored as a URI';
COMMENT ON COLUMN bcids.subResourceType is 'sub resource type for this bcid, stored as a URI';
COMMENT ON COLUMN bcids.created is 'timestamp of insertion';
COMMENT ON COLUMN bcids.modified is 'timestamp of last update';

DROP TABLE IF EXISTS projects;

CREATE TABLE projects (
  projectId SERIAL PRIMARY KEY NOT NULL,
  projectCode TEXT NOT NULL,
  projectTitle TEXT NULL,
  projectUrl TEXT NOT NULL,
  validationXml TEXT NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  userId INTEGER NOT NULL REFERENCES users (userId),
  public BOOLEAN NOT NULL DEFAULT '1'
);

CREATE INDEX projects_userId_idx ON projects (userId);
CREATE INDEX projects_public_idx ON projects (public);
CREATE INDEX projects_projectUrl_idx ON projects (projectUrl);

CREATE TRIGGER update_projects_modtime BEFORE UPDATE ON projects FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_projects_createdtime BEFORE INSERT ON projects FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN projects.projectCode is 'The short name for this project';
COMMENT ON COLUMN projects.projectUrl is 'Where this project is located on the web';
COMMENT ON COLUMN projects.public is 'Whether or not this is a public project?';

DROP TABLE IF EXISTS userProjects;

CREATE TABLE userProjects (
  userProjectId SERIAL PRIMARY KEY NOT NULL,
  projectId INTEGER NOT NULL REFERENCES projects (projectId),
  userId INTEGER NOT NULL REFERENCES users (userId),
  CONSTRAINT userProjects_userId_projectId_uniq UNIQUE (userId, projectId)
);

CREATE INDEX userProjects_projectId_idx ON projects (projectId);
CREATE INDEX userProjects_userId_idx ON projects (userId);

DROP TABLE IF EXISTS expeditions;

CREATE TABLE expeditions (
  expeditionId SERIAL NOT NULL PRIMARY KEY,
  projectId INTEGER NOT NULL REFERENCES projects (projectId),
  expeditionCode TEXT NOT NULL, -- change to code
  expeditionTitle TEXT NULL, -- change to title
  userId INTEGER NOT NULL REFERENCES users (userId),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  public BOOLEAN NOT NULL DEFAULT '1',
  CONSTRAINT expeditions_code_projectId_uniq UNIQUE (expeditionCode, projectId)
);

CREATE INDEX expeditions_projectId_idx ON expeditions (projectId);

CREATE TRIGGER update_expeditions_modtime BEFORE UPDATE ON expeditions FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER set_expeditions_createdtime BEFORE INSERT ON expeditions FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN expeditions.expeditionCode is 'The short name for this expedition';
COMMENT ON COLUMN expeditions.public is 'Whether or not this is a public expedition';

DROP TABLE IF EXISTS expeditionBcids;

CREATE TABLE expeditionBcids (
  expeditionBcidId SERIAL NOT NULL PRIMARY KEY,
  expeditionId INTEGER NOT NULL REFERENCES expeditions (expeditionId),
  bcidId INTEGER NOT NULL REFERENCES bcids (bcidId)
);

CREATE INDEX expeditionBcids_expeditionId_idx ON expeditionBcids (expeditionId);
CREATE INDEX expeditionBcids_bcidId_idx ON expeditionBcids (bcidId);

DROP TABLE IF EXISTS oAuthClients;

CREATE TABLE oAuthClients (
  clientId TEXT NOT NULL PRIMARY KEY,
  clientSecret TEXT NOT NULL,
  callback TEXT NOT NULL
);

COMMENT ON COLUMN oAuthClients.clientId is 'the public unique client id';
COMMENT ON COLUMN oAuthClients.clientSecret is 'the private shared secret';
COMMENT ON COLUMN oAuthClients.callback is 'The callback url of the client app';

DROP TABLE IF EXISTS oAuthNonces;

CREATE TABLE oAuthNonces (
  oAuthNonceId SERIAL NOT NULL PRIMARY KEY,
  clientId TEXT NOT NULL REFERENCES oAuthClients (clientId),
  code TEXT NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  userId INTEGER NOT NULL REFERENCES users (userId),
  redirectUri TEXT NOT NULL,
  CONSTRAINT oAuthNonces_code_clientId_uniq UNIQUE (clientId, code)
);

CREATE INDEX oAuthNonces_code_idx ON oAuthNonces (code);

CREATE TRIGGER set_oAuthNonces_createdtime BEFORE INSERT ON oAuthNonces FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN oAuthNonces.code is 'The generated code the client app can exchange for an access token';
COMMENT ON COLUMN oAuthNonces.redirectUri is 'The redirectUri associated with this code';

DROP TABLE IF EXISTS oAuthTokens;

CREATE TABLE oAuthTokens (
  oAuthTokenId SERIAL NOT NULL PRIMARY KEY,
  clientId TEXT NOT NULL REFERENCES oAuthClients (clientId),
  token TEXT NOT NULL UNIQUE,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  userId INTEGER NOT NULL REFERENCES users (userId),
  refreshToken TEXT NOT NULL
);

CREATE INDEX oAuthTokens_token_idx on oAuthTokens (token);
CREATE INDEX oAuthTokens_refreshToken_idx on oAuthTokens (refreshToken);

CREATE TRIGGER set_oAuthTokens_createdtime BEFORE INSERT ON oAuthTokens FOR EACH ROW EXECUTE PROCEDURE set_created_column();

COMMENT ON COLUMN oAuthTokens.token is 'The generated token used by the client app';
COMMENT ON COLUMN oAuthTokens.refreshToken is 'The generated token used to gain a new access_token';

CREATE OR REPLACE FUNCTION delete_expired_oAuthTokens()
  RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM oAuthTokens WHERE created < now() - INTERVAL '2 DAYS';
  RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER delete_expired_oAuthTokens AFTER INSERT ON oAuthTokens EXECUTE PROCEDURE delete_expired_oAuthTokens();

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

DROP TABLE IF EXISTS templateConfigs;

CREATE TABLE templateConfigs (
  templateConfigId SERIAL NOT NULL PRIMARY KEY,
  userId INTEGER NOT NULL REFERENCES users (userId),
  projectId INTEGER NOT NULL REFERENCES projects (projectId),
  configName TEXT NOT NULL,
  public BOOLEAN NOT NULL DEFAULT '0',
  config TEXT NOT NULL,
  CONSTRAINT templateConfigs_configName_projectId_uniq UNIQUE (configName, projectId)
);

CREATE INDEX templateConfigs_projectId_idx ON templateConfigs (projectId);

COMMENT ON COLUMN templateConfigs.configName is 'The name of the config';
COMMENT ON COLUMN templateConfigs.public is 'Whether or not this is a public template config?';
COMMENT ON COLUMN templateConfigs.config is 'The array of uris to be checked when generating a template';
