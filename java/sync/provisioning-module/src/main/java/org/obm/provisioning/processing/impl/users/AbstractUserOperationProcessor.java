/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2011-2013  Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for OBM
 * software by Linagora pursuant to Section 7 of the GNU Affero General Public
 * License, subsections (b), (c), and (e), pursuant to which you must notably (i)
 * retain the displaying by the interactive user interfaces of the “OBM, Free
 * Communication by Linagora” Logo with the “You are using the Open Source and
 * free version of OBM developed and supported by Linagora. Contribute to OBM R&D
 * by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
 * links between OBM and obm.org, between Linagora and linagora.com, as well as
 * between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks and
 * commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for OBM along with this program. If not, see
 * <http://www.gnu.org/licenses/> for the GNU Affero General   Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to the OBM software.
 * ***** END LICENSE BLOCK ***** */
package org.obm.provisioning.processing.impl.users;

import static fr.aliacom.obm.common.system.ObmSystemUser.CYRUS;

import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.obm.cyrus.imap.admin.CyrusImapService;
import org.obm.cyrus.imap.admin.CyrusManager;
import org.obm.domain.dao.UserDao;
import org.obm.domain.dao.UserSystemDao;
import org.obm.provisioning.ProvisioningService;
import org.obm.provisioning.beans.Batch;
import org.obm.provisioning.beans.BatchEntityType;
import org.obm.provisioning.beans.HttpVerb;
import org.obm.provisioning.beans.Operation;
import org.obm.provisioning.beans.Request;
import org.obm.provisioning.dao.exceptions.DaoException;
import org.obm.provisioning.dao.exceptions.SystemUserNotFoundException;
import org.obm.provisioning.exception.ProcessingException;
import org.obm.provisioning.json.ObmUserJsonDeserializer;
import org.obm.provisioning.ldap.client.LdapManager;
import org.obm.provisioning.ldap.client.LdapService;
import org.obm.provisioning.processing.impl.HttpVerbBasedOperationProcessor;
import org.obm.push.mail.IMAPException;
import org.obm.sync.host.ObmHost;
import org.obm.sync.serviceproperty.ServiceProperty;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.util.Providers;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.system.ObmSystemUser;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserExtId;

public abstract class AbstractUserOperationProcessor extends HttpVerbBasedOperationProcessor {

	@Inject
	protected UserDao userDao;
	@Inject
	protected CyrusImapService cyrusService;
	@Inject
	protected LdapService ldapService;
	@Inject
	protected UserSystemDao userSystemDao;

	protected AbstractUserOperationProcessor(HttpVerb verb) {
		super(BatchEntityType.USER, verb);
	}

	protected CyrusManager buildCyrusManager(ObmUser user) throws DaoException, SystemUserNotFoundException, IMAPException {
		ObmSystemUser cyrusUserSystem = userSystemDao.getByLogin(CYRUS);

		return cyrusService.buildManager(user.getMailHost().getIp(), cyrusUserSystem.getLogin(), cyrusUserSystem.getPassword());
	}

	protected ObmUser inheritDatabaseIdentifiers(ObmUser user, ObmUser existingUser) {
		return ObmUser.builder().from(user).uid(existingUser.getUid()).entityId(existingUser.getEntityId()).build();
	}

	protected UserExtId getExtIdFromRequestParams(Operation operation) {
		final Request request = operation.getRequest();
		final String itemId = request.getParams().get(Request.ITEM_ID_KEY);

		if (Strings.isNullOrEmpty(itemId)) {
			throw new ProcessingException(String.format("Cannot get extId parameter from request url %s.", request.getResourcePath()));
		}

		return UserExtId.valueOf(itemId);
	}

	protected ObmUser getUserFromDao(UserExtId extId, ObmDomain domain) {
		try {
			return userDao.getByExtId(extId, domain);
		}
		catch (Exception e) {
			throw new ProcessingException(String.format("Cannot fetch existing user %s from database.", extId), e);
		}
	}

	protected ObmUser getUserFromRequestBody(Operation operation, Batch batch) {
		String requestBody = operation.getRequest().getBody();
		ObjectMapper objectMapper = getObjectMapperForDomain(batch.getDomain());

		try {
			return objectMapper.readValue(requestBody, ObmUser.class);
		}
		catch (Exception e) {
			throw new ProcessingException(String.format("Cannot parse ObmUser object from request body %s.", requestBody), e);
		}
	}

	private ObjectMapper getObjectMapperForDomain(ObmDomain domain) {
		Module module = new SimpleModule("InBatch", new Version(0, 0, 0, null)).addDeserializer(ObmUser.class, new ObmUserJsonDeserializer(Providers.of(domain)));

		return ProvisioningService.createObjectMapper(module);
	}

	protected LdapManager buildLdapManager(ObmUser user) {
		ObmDomain domain = user.getDomain();
		LdapConnectionConfig connectionConfig = new LdapConnectionConfig();
		ObmHost ldapHost = Iterables.getFirst(domain.getHosts().get(ServiceProperty.LDAP), null);

		if (ldapHost == null) {
			throw new ProcessingException(String.format("Domain %s for user %s (%s) has no linked %s host.", domain.getName(), user.getLogin(), user.getExtId(), ServiceProperty.LDAP));
		}

		connectionConfig.setLdapHost(ldapHost.getIp());
		connectionConfig.setLdapPort(LdapConnectionConfig.DEFAULT_LDAP_PORT);

		return ldapService.buildManager(connectionConfig);
	}

}