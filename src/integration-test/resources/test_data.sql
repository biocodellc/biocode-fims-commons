-- noinspection SqlNoDataSourceInspectionForFile

-- password = "d"
INSERT INTO users (userid, username, password, email, firstname, lastname, institution, hassetpassword, passwordresettoken, passwordresetexpiration, enabled, admin)
  VALUES (1, 'demo', '1000:4457f0de972588f0f4cc7b4c1cf3585bf2568091414f4432:3edd3bcd1074c73cb672c517f2807301988ec6f6b91b5bc1', 'demo@example.com', 'demo', 'user', 'higher learning', 0, NULL, NULL, TRUE, FALSE);
-- password = "d2"
INSERT INTO users (userid, username, password, email, firstname, lastname, institution, hassetpassword, passwordresettoken, passwordresetexpiration, enabled, admin)
  VALUES (2, 'demo2', '1000:3573e8b45b89d80d2cc7f350158e87ea097357ced0947945:c8efc9b61d017cf0a2aa8056f2d32034771786db2a7c69fe', 'demo2@example.com', 'demo2', 'user', 'higher learning', 0, NULL, NULL, TRUE, FALSE);
-- password = "d3"
INSERT INTO users (userid, username, password, email, firstname, lastname, institution, hassetpassword, passwordresettoken, passwordresetexpiration, enabled, admin)
VALUES (3, 'demo3', '1000:2a2a3a70259045a617052771d3fe378184e247762232c291:e14e983df237edb5066b2412ac949c9284cd0bc1b8001907', 'demo3@example.com', 'demo2', 'user', 'higher learning', 0, NULL, NULL, TRUE, FALSE);

INSERT INTO projects (projectId, abstract, projectCode, projectTitle, projectUrl, validationXml, ts, userid, public)
  VALUES (1, '', 'PROJ1', 'project 1', 'http://example.com', '', '2017-04-26 15:08:14', 1, TRUE);
INSERT INTO projects (projectId, abstract, projectCode, projectTitle, projectUrl, validationXml, ts, userid, public)
  VALUES (2, '', 'PROJ2', 'project 2', 'http://example.com', '', '2017-04-26 15:08:14', 2, FALSE );
INSERT INTO projects (projectId, abstract, projectCode, projectTitle, projectUrl, validationXml, ts, userid, public)
  VALUES (3, '', 'PROJ3', 'project 3', 'http://example.com', '', '2017-04-26 15:08:14', 3, TRUE );

INSERT INTO userprojects (userid, projectid) VALUES (1, 1);
INSERT INTO userprojects (userid, projectid) VALUES (1, 2);
INSERT INTO userprojects (userid, projectid) VALUES (2, 1);
INSERT INTO userprojects (userid, projectid) VALUES (2, 2);
INSERT INTO userprojects (userid, projectid) VALUES (3, 2);
INSERT INTO userprojects (userid, projectid) VALUES (3, 3);

INSERT INTO expeditions (expeditionId, projectid, expeditionCode, expeditionTitle, userid, ts, public)
  VALUES (1, 1, 'PROJ1_EXP1', 'project 1 expedition 1', 1, '2017-04-26 15:08:14',TRUE);
INSERT INTO expeditions (expeditionId, projectid, expeditionCode, expeditionTitle, userid, ts, public)
  VALUES (2, 1, 'PROJ1_EXP2', 'project 1 expedition 2', 2, '2017-04-26 15:08:14',TRUE);
INSERT INTO expeditions (expeditionId, projectid, expeditionCode, expeditionTitle, userid, ts, public)
  VALUES (3, 2, 'PROJ2_EXP1', 'project 2 expedition 1', 2, '2017-04-26 15:08:14',TRUE);

INSERT INTO bcids (bcidId, ezidMade, ezidRequest, finalCopy, identifier, resourceType, subResourceType, title, ts, webAddress, userId)
    VALUES (1, TRUE, TRUE, FALSE, 'ark:/99999/a2', 'http://purl.org/dc/dcmitype/Collection', NULL, '', '2017-04-26 15:08:14', NULL, 1);
INSERT INTO expeditionBcids (expeditionId, bcidId) VALUES (1, 1);
INSERT INTO bcids (bcidId, ezidMade, ezidRequest, finalCopy, identifier, resourceType, subResourceType, title, ts, webAddress, userId)
  VALUES (2, TRUE, TRUE, FALSE, 'ark:/99999/b2', 'http://purl.org/dc/dcmitype/Event', NULL, '', '2017-04-26 15:08:14', NULL, 1);
INSERT INTO expeditionBcids (expeditionId, bcidId) VALUES (1, 2);
INSERT INTO bcids (bcidId, ezidMade, ezidRequest, finalCopy, identifier, resourceType, subResourceType, title, ts, webAddress, userId)
  VALUES (3, TRUE, TRUE, FALSE, 'ark:/99999/c2', 'http://purl.org/dc/dcmitype/Collection', NULL, '', '2017-04-26 15:08:14', NULL,2);
INSERT INTO expeditionBcids (expeditionId, bcidId) VALUES (2, 3);
INSERT INTO bcids (bcidId, ezidMade, ezidRequest, finalCopy, identifier, resourceType, subResourceType, title, ts, webAddress, userId)
  VALUES (4, TRUE, TRUE, FALSE, 'ark:/99999/d2', 'http://purl.org/dc/dcmitype/Event', NULL, '', '2017-04-26 15:08:14', NULL, 2);
INSERT INTO expeditionBcids (expeditionId, bcidId) VALUES (2, 4);
INSERT INTO bcids (bcidId, ezidMade, ezidRequest, finalCopy, identifier, resourceType, subResourceType, title, ts, webAddress, userId)
  VALUES (5, TRUE, TRUE, FALSE, 'ark:/99999/e2', 'http://purl.org/dc/dcmitype/Dataset', 'Fims Metadata', '', '2017-04-26 15:08:14', NULL, 2);
INSERT INTO expeditionBcids (expeditionId, bcidId) VALUES (3, 5);
INSERT INTO bcids (bcidId, ezidMade, ezidRequest, finalCopy, identifier, resourceType, subResourceType, title, ts, webAddress, userId)
  VALUES (6, TRUE, TRUE, FALSE, 'ark:/99999/A2', 'http://purl.org/dc/dcmitype/Collection', NULL, '', '2017-04-26 15:08:14', NULL, 2);
INSERT INTO expeditionBcids (expeditionId, bcidId) VALUES (3, 6);


