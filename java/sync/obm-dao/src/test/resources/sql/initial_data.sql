INSERT INTO entity (entity_mailing)
    VALUES
        (true),
        (true);
INSERT INTO domain (domain_name, domain_uuid, domain_label) VALUES ('test.tlse.lng', 'ac21bc0c-f816-4c52-8bb9-e50cfbfec5b6', 'test.tlse.lng');
INSERT INTO domainentity (domainentity_entity_id, domainentity_domain_id) VALUES (1, 1);
INSERT INTO domain (domain_name, domain_uuid, domain_label) VALUES ('test2.tlse.lng', '3a2ba641-4ae0-4b40-aa5e-c3fd3acb78bf', 'test2.tlse.lng');
INSERT INTO domainentity (domainentity_entity_id, domainentity_domain_id) VALUES (2, 2);

INSERT INTO host (host_domain_id, host_name, host_ip, host_fqdn)
    VALUES
        (1, 'mail', '1.2.3.4', 'mail.tlse.lng'),
        (1, 'sync', '1.2.3.5', 'sync.tlse.lng');
INSERT INTO serviceproperty (serviceproperty_service, serviceproperty_property, serviceproperty_entity_id, serviceproperty_value)
    VALUES
        ('mail', 'smtp_in', 1, 1),
        ('mail', 'imap', 1, 1),
        ('sync', 'obm_sync', 1, 2),
        ('mail', 'smtp_in', 2, 1);

INSERT INTO profile (profile_domain_id, profile_name) VALUES (1, 'admin');
INSERT INTO profile (profile_domain_id, profile_name) VALUES (1, 'user');
INSERT INTO profile (profile_domain_id, profile_name) VALUES (2, 'editor');