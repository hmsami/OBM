-- ////////////////////////////////////////////////////////////////////////////
-- // Update OBM MySQL Database from 0.8.9 to 0.9.0                          //
-- ////////////////////////////////////////////////////////////////////////////
-- // $Id$
-- ////////////////////////////////////////////////////////////////////////////


-------------------------------------------------------------------------------
-- Global Information table
-------------------------------------------------------------------------------
UPDATE ObmInfo set obminfo_value='0.9.0' where obminfo_name='db_version';


-------------------------------------------------------------------------------
-- Global Preferences update
-------------------------------------------------------------------------------
DROP TABLE IF EXISTS GlobalPref;


-------------------------------------------------------------------------------
-- RepeatKind table drop 
-------------------------------------------------------------------------------
DROP TABLE IF EXISTS RepeatKind;


-------------------------------------------------------------------------------
-- Update User and Group tables
-------------------------------------------------------------------------------
-- Add column _local
ALTER TABLE UserObm ADD COLUMN userobm_local int(1) DEFAULT 1 after userobm_usercreate;
ALTER TABLE UGroup ADD COLUMN group_local int(1) DEFAULT 1 after group_usercreate;
ALTER TABLE UGroup ADD COLUMN group_privacy int(2) NULL DEFAULT 0 after group_system;

-- Add column _ext_id
ALTER TABLE UserObm ADD COLUMN userobm_ext_id varchar(16) after userobm_local;
ALTER TABLE UGroup ADD COLUMN group_ext_id varchar(16) after group_local;

-- User phone2, fax, fax2 and description
ALTER TABLE UserObm ADD COLUMN userobm_phone2 varchar(32) after userobm_phone;
ALTER TABLE UserObm ADD COLUMN userobm_fax varchar(32) after userobm_phone2;
ALTER TABLE UserObm ADD COLUMN userobm_fax2 varchar(32) after userobm_fax;
ALTER TABLE UserObm ADD COLUMN userobm_description varchar(255) after userobm_email;

-- Add column _calendar_version
ALTER TABLE UserObm ADD COLUMN userobm_calendar_version timestamp(14) after userobm_perms;


-------------------------------------------------------------------------------
-- Update Contract tables
-------------------------------------------------------------------------------
-- Update Contract table

ALTER TABLE Contract ADD COLUMN contract_datesignature date DEFAULT NULL after contract_number;
ALTER TABLE Contract ADD COLUMN contract_daterenew date DEFAULT NULL after contract_dateexp;
ALTER TABLE Contract ADD COLUMN contract_datecancel date DEFAULT NULL after contract_daterenew;
ALTER TABLE Contract ADD COLUMN contract_priority_id int(8) NOT NULL DEFAULT 0 after contract_type_id;
ALTER TABLE Contract ADD COLUMN contract_status_id int(8) NOT NULL DEFAULT 0 after contract_priority_id;
ALTER TABLE Contract ADD COLUMN contract_kind int(2) NULL DEFAULT 0 after contract_status_id;
ALTER TABLE Contract ADD COLUMN contract_format int(2) NULL DEFAULT 0 after contract_kind;
ALTER TABLE Contract ADD COLUMN contract_ticketnumber int(8) NULL DEFAULT 0 after contract_format;
ALTER TABLE Contract ADD COLUMN contract_duration float NULL DEFAULT 0 after contract_ticketnumber;
ALTER TABLE Contract ADD COLUMN contract_autorenewal int(2) NULL DEFAULT 0 after contract_duration;
ALTER TABLE Contract ADD COLUMN contract_privacy int(2) NULL DEFAULT 0 after contract_marketmanager_id;

--
-- New table 'ContractPriority'
--
CREATE TABLE ContractPriority (
  contractpriority_id          int(8) NOT NULL auto_increment,
  contractpriority_timeupdate  timestamp(14),
  contractpriority_timecreate  timestamp(14),
  contractpriority_userupdate  int(8) default NULL,
  contractpriority_usercreate  int(8) default NULL,
  contractpriority_color       varchar(6) default NULL,
  contractpriority_order       int(2) default NULL, 
  contractpriority_label       varchar(32) default NULL,
  PRIMARY KEY (contractpriority_id)
);

--
-- Dumping data for table 'ContractPriority'
--
INSERT INTO ContractPriority (contractpriority_color, contractpriority_order, contractpriority_label) VALUES ('FF0000', 1, 'Hight');
INSERT INTO ContractPriority (contractpriority_color, contractpriority_order, contractpriority_label) VALUES ('FFA0A0', 2, 'Normal');
INSERT INTO ContractPriority (contractpriority_color, contractpriority_order, contractpriority_label) VALUES ('FFF0F0', 3, 'Low');


--
-- New table 'ContractStatus'
--
CREATE TABLE ContractStatus (
  contractstatus_id     	int(8) NOT NULL auto_increment,
  contractstatus_timeupdate  	timestamp(14),
  contractstatus_timecreate  	timestamp(14),
  contractstatus_userupdate  	int(8) default NULL,
  contractstatus_usercreate  	int(8) default NULL,
  contractstatus_order  	int(2) default NULL,
  contractstatus_label  	varchar(32) default NULL,
PRIMARY KEY (contractstatus_id)
);

--
-- Dumping data for table 'ContractStatus'
--
INSERT INTO ContractStatus (contractstatus_order, contractstatus_label) VALUES (1, 'Open');
INSERT INTO ContractStatus (contractstatus_order, contractstatus_label) VALUES (2, 'Close');


-------------------------------------------------------------------------------
-- Update Incident tables
-------------------------------------------------------------------------------
ALTER TABLE Incident ADD COLUMN incident_cat1_id int(8) DEFAULT NULL after incident_status_id;
ALTER TABLE Incident CHANGE incident_duration incident_duration float DEFAULT 0;
ALTER TABLE Incident CHANGE incident_description incident_comment TEXT DEFAULT NULL;

--
-- New table 'IncidentCategory1'
--
CREATE TABLE IncidentCategory1 (
  incidentcategory1_id          int(8) NOT NULL auto_increment,
  incidentcategory1_timeupdate  timestamp(14),
  incidentcategory1_timecreate  timestamp(14),
  incidentcategory1_userupdate  int(8) default NULL,
  incidentcategory1_usercreate  int(8) default NULL,
  incidentcategory1_order       int(2),
  incidentcategory1_label       varchar(32) default NULL,
PRIMARY KEY (incidentcategory1_id)
);

--
-- Dumping data for table 'IncidentCategory1'
--
INSERT INTO IncidentCategory1 (incidentcategory1_order, incidentcategory1_label) VALUES (1, 'By email / phone');
INSERT INTO IncidentCategory1 (incidentcategory1_order, incidentcategory1_label) VALUES (2, 'On site');


-------------------------------------------------------------------------------
-- Update Display Prefs
-------------------------------------------------------------------------------
UPDATE DisplayPref SET display_fieldname='company_name' WHERE display_entity='contact' AND display_fieldname='contact_company_name';


-------------------------------------------------------------------------------
-- Update Contact table
-------------------------------------------------------------------------------
ALTER TABLE Contact ADD COLUMN contact_company varchar(64) AFTER contact_company_id;


