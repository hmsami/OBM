-- /////////////////////////////////////////////////////////////////////////////
-- OBM - File : update-2.2.14-2.2.15.mysql.sql
-- 2009-10-14 Mehdi Rande
-- /////////////////////////////////////////////////////////////////////////////
-- $Id: $
-- /////////////////////////////////////////////////////////////////////////////

UPDATE ObmInfo SET obminfo_value = '2.2.16-pre' WHERE obminfo_name = 'db_version';
--UPDATE ObmInfo SET obminfo_value = '2.2.16' WHERE obminfo_name = 'db_version';

ALTER TABLE UGroup DROP KEY group_gid;
ALTER TABLE UGroup ADD UNIQUE KEY group_gid (group_domain_id, group_gid);
ALTER TABLE P_UGroup DROP KEY group_gid;
ALTER TABLE P_UGroup ADD UNIQUE KEY group_gid (group_domain_id, group_gid);
