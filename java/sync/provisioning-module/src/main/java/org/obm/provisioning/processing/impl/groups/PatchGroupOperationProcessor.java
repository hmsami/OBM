/* ***** BEGIN LICENSE BLOCK *****
 *
 * Copyright (C) 2011-2013  Linagora
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version, provided you comply
 * with the Additional Terms applicable for OBM connector by Linagora
 * pursuant to Section 7 of the GNU Affero General Public License,
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain
 * the “Message sent thanks to OBM, Free Communication by Linagora”
 * signature notice appended to any and all outbound messages
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain
 * from infringing Linagora intellectual property rights over its trademarks
 * and commercial brands. Other Additional Terms apply,
 * see <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and its applicable Additional Terms for OBM along with this program. If not,
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to
 * OBM connectors.
 *
 * ***** END LICENSE BLOCK ***** */
package org.obm.provisioning.processing.impl.groups;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.obm.annotations.transactional.Transactional;
import org.obm.provisioning.Group;
import org.obm.provisioning.GroupExtId;
import org.obm.provisioning.ProvisioningService;
import org.obm.provisioning.beans.Batch;
import org.obm.provisioning.beans.HttpVerb;
import org.obm.provisioning.beans.Operation;
import org.obm.provisioning.exception.ProcessingException;
import org.obm.provisioning.json.PatchGroupJsonDeserializer;

import com.google.inject.Inject;

import fr.aliacom.obm.common.domain.ObmDomain;

public class PatchGroupOperationProcessor extends ModifyGroupOperationProcessor {
	
	@Inject
	PatchGroupOperationProcessor() {
		super(HttpVerb.PATCH);
	}
	
	@Override
	@Transactional
	public void process(Operation operation, Batch batch) throws ProcessingException {
		final ObmDomain domain = batch.getDomain();
		GroupExtId extId = getGroupExtIdFromRequest(operation);
		Group oldGroup = getGroupFromDao(extId, domain);
		Group group = getGroupFromRequestBody(operation, oldGroup);
		
		Group newGroup = modifyGroupInDao(domain, inheritDatabaseIdentifiers(group, oldGroup));
		modifyGroupInLdap(domain, newGroup, oldGroup);
	}

	private Group getGroupFromRequestBody(Operation operation, Group oldGroup) {
		String requestBody = operation.getRequest().getBody();
		ObjectMapper objectMapper = getObjectMapperForDomain(oldGroup);

		try {
			return objectMapper.readValue(requestBody, Group.class);
		}
		catch (Exception e) {
			throw new ProcessingException(String.format("Cannot parse ObmUser object from request body %s.", requestBody), e);
		}
	}

	private ObjectMapper getObjectMapperForDomain(Group oldGroup) {
		Module module = new SimpleModule("InBatch", new Version(0, 0, 0, null))
			.addDeserializer(Group.class, new PatchGroupJsonDeserializer(oldGroup));

		return ProvisioningService.createObjectMapper(module);
	}
}
