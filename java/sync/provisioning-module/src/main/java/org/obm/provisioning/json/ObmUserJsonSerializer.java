/* ***** BEGIN LICENSE BLOCK *****
 *
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.provisioning.json;

import static org.obm.provisioning.bean.UserJsonFields.ADDRESSES;
import static org.obm.provisioning.bean.UserJsonFields.BUSINESS_ZIPCODE;
import static org.obm.provisioning.bean.UserJsonFields.COMMONNAME;
import static org.obm.provisioning.bean.UserJsonFields.COMPANY;
import static org.obm.provisioning.bean.UserJsonFields.COUNTRY;
import static org.obm.provisioning.bean.UserJsonFields.DESCRIPTION;
import static org.obm.provisioning.bean.UserJsonFields.DIRECTION;
import static org.obm.provisioning.bean.UserJsonFields.FAXES;
import static org.obm.provisioning.bean.UserJsonFields.FIRSTNAME;
import static org.obm.provisioning.bean.UserJsonFields.GROUPS;
import static org.obm.provisioning.bean.UserJsonFields.ID;
import static org.obm.provisioning.bean.UserJsonFields.KIND;
import static org.obm.provisioning.bean.UserJsonFields.LASTNAME;
import static org.obm.provisioning.bean.UserJsonFields.LOGIN;
import static org.obm.provisioning.bean.UserJsonFields.MAILS;
import static org.obm.provisioning.bean.UserJsonFields.MAIL_QUOTA;
import static org.obm.provisioning.bean.UserJsonFields.MAIL_SERVER;
import static org.obm.provisioning.bean.UserJsonFields.MOBILE;
import static org.obm.provisioning.bean.UserJsonFields.PASSWORD;
import static org.obm.provisioning.bean.UserJsonFields.PHONES;
import static org.obm.provisioning.bean.UserJsonFields.PROFILE;
import static org.obm.provisioning.bean.UserJsonFields.SERVICE;
import static org.obm.provisioning.bean.UserJsonFields.TIMECREATE;
import static org.obm.provisioning.bean.UserJsonFields.TIMEUPDATE;
import static org.obm.provisioning.bean.UserJsonFields.TITLE;
import static org.obm.provisioning.bean.UserJsonFields.TOWN;
import static org.obm.provisioning.bean.UserJsonFields.ZIPCODE;

import java.io.IOException;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import com.google.common.collect.Sets;

import fr.aliacom.obm.common.user.ObmUser;


public class ObmUserJsonSerializer extends JsonSerializer<ObmUser> {
	
	private final static String NOT_IMPLEMENTED_YET = "Not implemented yet";

	@Override
	public void serialize(ObmUser value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		Set<String> mails = Sets.newHashSet();
		mails.add(value.getEmailAtDomain());
		mails.addAll(value.getEmailAlias());
		
		jgen.writeStartObject();
		jgen.writeObjectField(ID.asSpecificationValue(), value.getExtId());
		jgen.writeStringField(LOGIN.asSpecificationValue(), value.getLogin());
		jgen.writeStringField(LASTNAME.asSpecificationValue(), value.getLastName());
		jgen.writeStringField(PROFILE.asSpecificationValue(),
				value.getProfileName() != null ? value.getProfileName().getName() : null);
		jgen.writeStringField(FIRSTNAME.asSpecificationValue(), value.getFirstName());
		jgen.writeStringField(COMMONNAME.asSpecificationValue(), value.getCommonName());
		jgen.writeStringField(PASSWORD.asSpecificationValue(), value.getPassword());
		jgen.writeStringField(KIND.asSpecificationValue(), value.getKind());
		jgen.writeStringField(TITLE.asSpecificationValue(), value.getTitle());
		jgen.writeStringField(DESCRIPTION.asSpecificationValue(), value.getDescription());
		jgen.writeStringField(COMPANY.asSpecificationValue(), value.getCompany());
		jgen.writeStringField(SERVICE.asSpecificationValue(), value.getService());
		jgen.writeStringField(DIRECTION.asSpecificationValue(), value.getDirection());
		writeStringsField(jgen, ADDRESSES.asSpecificationValue(),
				value.getAddress1(), value.getAddress2(), value.getAddress3());
		jgen.writeStringField(TOWN.asSpecificationValue(), value.getTown());
		jgen.writeStringField(ZIPCODE.asSpecificationValue(), value.getZipCode());
		jgen.writeStringField(BUSINESS_ZIPCODE.asSpecificationValue(), value.getExpresspostal());
		jgen.writeStringField(COUNTRY.asSpecificationValue(), value.getCountryCode());
		writeStringsField(jgen, PHONES.asSpecificationValue(), value.getPhone(), value.getPhone2());
		jgen.writeStringField(MOBILE.asSpecificationValue(), value.getMobile());
		writeStringsField(jgen, FAXES.asSpecificationValue(), value.getFax(), value.getFax2());
		jgen.writeStringField(MAIL_QUOTA.asSpecificationValue(), String.valueOf(value.getMailQuota()));
		jgen.writeStringField(MAIL_SERVER.asSpecificationValue(), NOT_IMPLEMENTED_YET);
		jgen.writeObjectField(MAILS.asSpecificationValue(), mails);
		jgen.writeObjectField(TIMECREATE.asSpecificationValue(), value.getTimeCreate());
		jgen.writeObjectField(TIMEUPDATE.asSpecificationValue(), value.getTimeUpdate());
		writeStringsField(jgen, GROUPS.asSpecificationValue(), NOT_IMPLEMENTED_YET);
		jgen.writeEndObject();
	}
	
	public void writeStringsField(JsonGenerator jgen, String fieldName, String... values)
			throws JsonGenerationException, IOException {
		jgen.writeFieldName(fieldName);
		jgen.writeStartArray();
		for(String value: values) {
			if (value != null) {
				jgen.writeString(value);
			}
		}
		jgen.writeEndArray();
	}
	
}