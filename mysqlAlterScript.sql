SET FOREIGN_KEY_CHECKS=0;

alter table users drop column enabled;
alter table users drop column admin;
ALTER TABLE `users` CHANGE `userId` `id` INT(11)  UNSIGNED  NOT NULL  AUTO_INCREMENT;
ALTER TABLE `users` CHANGE `hasSetPassword` `has_set_password` TINYINT(1)  NOT NULL  DEFAULT '0';
ALTER TABLE `users` CHANGE `firstName` `first_name` VARCHAR(60)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
ALTER TABLE `users` CHANGE `lastName` `last_name` VARCHAR(60)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
ALTER TABLE `users` CHANGE `passwordResetToken` `password_reset_token` CHAR(20)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
ALTER TABLE `users` CHANGE `passwordResetExpiration` `password_reset_expiration` DATETIME  NULL  DEFAULT NULL;

alter table expeditionBcids drop column expeditionBcidId;
ALTER TABLE `expeditionBcids` CHANGE `expeditionId` `expedition_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `expeditionBcids` CHANGE `bcidId` `bcid_id` INT(11)  UNSIGNED  NOT NULL;
RENAME TABLE `expeditionBcids` TO `expedition_bcids`;

alter table bcids drop column internalId;
alter table bcids drop column finalCopy;
ALTER TABLE `bcids` CHANGE `ts` `ts` TIMESTAMP  NOT NULL  DEFAULT CURRENT_TIMESTAMP  COMMENT 'timestamp of insertion';
alter table bcids change column ts created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `bcids` CHANGE `bcidId` `id` INT(11)  UNSIGNED  NOT NULL  AUTO_INCREMENT;
ALTER TABLE `bcids` CHANGE `ezidMade` `ezid_made` TINYINT(1)  NOT NULL  DEFAULT '0';
ALTER TABLE `bcids` CHANGE `ezidRequest` `ezid_request` TINYINT(1)  NOT NULL  DEFAULT '1';
ALTER TABLE `bcids` CHANGE `userId` `user_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `bcids` CHANGE `webAddress` `web_address` VARCHAR(2083)  CHARACTER SET utf8  BINARY  NULL  DEFAULT NULL;
ALTER TABLE `bcids` CHANGE `resourceType` `resource_type` VARCHAR(2083)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
ALTER TABLE `bcids` CHANGE `sourceFile` `source_file` VARCHAR(255)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
ALTER TABLE `bcids` CHANGE `subResourceType` `sub_resource_type` VARCHAR(20)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
RENAME TABLE `bcids` TO `bcids_tmp`;

alter table expeditions drop internalId;
alter table expeditions change column ts created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `expeditions` CHANGE `expeditionId` `id` INT(11)  UNSIGNED  NOT NULL  AUTO_INCREMENT;
ALTER TABLE `expeditions` CHANGE `expeditionCode` `expedition_code` VARCHAR(58)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
ALTER TABLE `expeditions` CHANGE `expeditionTitle` `expedition_title` VARCHAR(128)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
ALTER TABLE `expeditions` CHANGE `userId` `user_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `expeditions` CHANGE `projectId` `project_id` INT(11)  UNSIGNED  NOT NULL;

alter table projects drop column abstract;
alter table projects change column ts created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `projects` CHANGE `projectId` `id` INT(11)  UNSIGNED  NOT NULL  AUTO_INCREMENT;
ALTER TABLE `projects` CHANGE `projectCode` `project_code` VARCHAR(6)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
ALTER TABLE `projects` CHANGE `projectTitle` `project_title` VARCHAR(128)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
ALTER TABLE `projects` CHANGE `validationXml` `validation_xml` VARCHAR(2083)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
ALTER TABLE `projects` CHANGE `userId` `user_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `projects` CHANGE `projectUrl` `project_url` VARCHAR(2083)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';

alter table userProjects drop column userProjectId;
ALTER TABLE `userProjects` CHANGE `projectId` `project_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `userProjects` CHANGE `userId` `user_id` INT(11)  UNSIGNED  NOT NULL;
RENAME TABLE `userProjects` TO `user_projects`;

ALTER TABLE `templateConfigs` drop column `public`;
ALTER TABLE `templateConfigs` CHANGE `templateConfigId` `id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `templateConfigs` CHANGE `userId` `user_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `templateConfigs` CHANGE `projectId` `project_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `templateConfigs` CHANGE `configName` `name` VARCHAR(100)  CHARACTER SET utf8  COLLATE utf8_general_ci  NULL  DEFAULT NULL;
ALTER TABLE `templateConfigs` CHANGE `config` `attribute_uris` MEDIUMTEXT  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  COMMENT 'The array of uris to be checked when generating a template';
RENAME TABLE `templateConfigs` TO `project_templates`;

alter table oAuthClients drop oAuthClientId;
ALTER TABLE `oAuthClients` CHANGE `clientId` `id` CHAR(20)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
ALTER TABLE `oAuthClients` CHANGE `clientSecret` `client_secret` CHAR(75)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
RENAME TABLE `oAuthClients` TO `oauth_clients`;

alter table `oAuthNonces` change column ts created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `oAuthNonces` CHANGE `oAuthNonceId` `id` INT(11)  UNSIGNED  NOT NULL  AUTO_INCREMENT;
ALTER TABLE `oAuthNonces` CHANGE `clientId` `client_id` CHAR(20)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
ALTER TABLE `oAuthNonces` CHANGE `userId` `user_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `oAuthNonces` CHANGE `redirectUri` `redirect_uri` VARCHAR(2083)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
RENAME TABLE `oAuthNonces` TO `oauth_nonces`;

alter table oAuthTokens change column ts created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `oAuthTokens` CHANGE `oAuthTokenId` `id` INT(11)  UNSIGNED  NOT NULL  AUTO_INCREMENT;
ALTER TABLE `oAuthTokens` CHANGE `clientId` `client_id` CHAR(20)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
ALTER TABLE `oAuthTokens` CHANGE `userId` `user_id` INT(11)  UNSIGNED  NOT NULL;
ALTER TABLE `oAuthTokens` CHANGE `refreshToken` `refresh_token` CHAR(20)  CHARACTER SET utf8  COLLATE utf8_general_ci  NOT NULL  DEFAULT '';
RENAME TABLE `oAuthTokens` TO `oauth_tokens`;

SET FOREIGN_KEY_CHECKS=1;
