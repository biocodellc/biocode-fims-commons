create table bcids (
  bcidId integer generated by default as identity (start with 1),
  doi varchar(255),
  ezidMade bit,
  ezidRequest bit not null,
  finalCopy bit not null,
  graph varchar(255),
  identifier varchar(255),
  resourceType varchar(255) not null,
  sourceFile varchar(255),
  subResourceType varchar(255),
  title varchar(255),
  ts timestamp,
  webAddress varchar(255),
  userId integer not null,
  primary key (bcidId)
);
create table expeditionBcids (
  expeditionId integer,
  bcidId integer not null,
  primary key (bcidId)
);
create table expeditions (
  expeditionId integer generated by default as identity (start with 1),
  expeditionCode varchar(255) not null,
  expeditionTitle varchar(255),
  public boolean not null,
  ts timestamp,
  projectId integer not null,
  userId integer,
  primary key (expeditionId)
);
create table oAuthClients (
  clientId varchar(255) not null,
  callback varchar(255),
  clientSecret varchar(255),
  primary key (clientId)
);
create table oAuthNonces (
  oAuthNonceId integer generated by default as identity (start with 1),
  code varchar(255) not null,
  redirectUri varchar(255) not null,
  ts timestamp,
  clientId varchar(255) not null,
  userId integer,
  primary key (oAuthNonceId)
);
create table oAuthTokens (
  oAuthTokenId integer generated by default as identity (start with 1),
  refreshToken varchar(255) not null,
  token varchar(255) not null,
  ts timestamp,
  clientId varchar(255) not null,
  userId integer,
  primary key (oAuthTokenId)
);
create table projects (
  projectId integer generated by default as identity (start with 1),
  abstract varchar(255) null,
  projectCode varchar(255),
  projectTitle varchar(255),
  projectUrl varchar(255) not null,
  public boolean not null,
  ts timestamp,
  validationXml varchar(255) not null,
  userId integer not null,
  primary key (projectId)
);
create table templateConfigs (
  templateConfigId integer generated by default as identity (start with 1),
  config varchar(255) not null,
  configName varchar(255) not null,
  public boolean,
  projectId integer not null,
  userId integer not null,
  primary key (templateConfigId)
);
create table userProjects (
  userId integer not null,
  projectId integer not null
);
create table users (
  userId integer generated by default as identity (start with 1),
  admin boolean not null,
  email varchar(255) not null,
  enabled boolean not null,
  firstName varchar(255) not null,
  hasSetPassword boolean not null,
  institution varchar(255) not null,
  lastName varchar(255) not null,
  password varchar(255) not null,
  passwordResetExpiration timestamp,
  passwordResetToken char(20) null,
  username varchar(255) not null,
  primary key (userId)
);
