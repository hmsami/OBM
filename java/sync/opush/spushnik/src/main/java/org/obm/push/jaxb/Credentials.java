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
package org.obm.push.jaxb;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

@XmlRootElement(name="Credentials")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "loginAtDomain", "password", "certificate" })
public class Credentials {

	public static Builder builder() {
		return new Builder();
	}
	
	public static Credentials createEmptyRequest() {
		return builder().build();
	}
	
	public static class Builder {
		private String loginAtDomain;
		private String password;
		private byte[] certificate;
		
		private Builder() {
			super();
		}
		
		public Builder loginAtDomain(String loginAtDomain) {
			this.loginAtDomain = loginAtDomain;
			return this;
		}
		
		public Builder password(String password) {
			this.password = password;
			return this;
		}
		
		public Builder certificate(byte[] certificate) {
			this.certificate = certificate;
			return this;
		}
		
		public Credentials build() {
			Preconditions.checkState(loginAtDomain != null, "loginAtDomain is required");
			Preconditions.checkState(password != null, "password is required");
			return new Credentials(loginAtDomain, password, certificate);
		}
	}
	
	@XmlElement
	private final String loginAtDomain;
	@XmlElement
	private final String password;
	@XmlElement
	private final byte[] certificate;

	private Credentials() {
		this(null, null, null);
	}
	
	private Credentials(String loginAtDomain, String password, byte[] certificate) {
		this.loginAtDomain = loginAtDomain;
		this.password = password;
		this.certificate = certificate;
	}
	
	public String getLoginAtDomain() {
		return loginAtDomain;
	}
	
	public String getPassword() {
		return password;
	}
	
	public byte[] getCertificate() {
		return certificate;
	}
	
	@Override
	public final int hashCode(){
		return Objects.hashCode(loginAtDomain, password, Arrays.hashCode(certificate));
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof Credentials) {
			Credentials that = (Credentials) object;
			return Objects.equal(this.loginAtDomain, that.loginAtDomain)
				&& Objects.equal(this.password, that.password)
				&& Arrays.equals(this.certificate, that.certificate);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("loginAtDomain", loginAtDomain)
			.add("password", password)
			.add("certificate", certificate)
			.toString();
	}
}
