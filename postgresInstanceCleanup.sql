-- This script will remove all data associated with a different fims instance. Need to update the project_id for each database this is run against

drop table expedition_bcids;
drop table bcids_tmp;

delete from entity_identifiers where id in (select e.id from expeditions e join projects p on p.id = e.project_id where p.id != 25);
delete from expeditions where project_id != 25;
delete from project_templates where project_id != 25;
delete from project_config_history where project_id != 25;
delete from user_projects where project_id != 25;
delete from projects where id != 25;
delete from users u where id not in (select user_id from user_projects where project_id = 25 union select user_id from projects where project_id = 25);

alter table projects drop column project_url;
