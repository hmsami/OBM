/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.configuration;



import org.obm.configuration.utils.IniFile;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

@Singleton
public class EmailConfigurationImpl implements EmailConfiguration {
	
	private static final int IMAP_PORT = 143;
	private static final int BACKEND_IMAP_TIMEOUT_DEFAULT = 5000;
	@VisibleForTesting
	static final MailboxNameCheckPolicy MAILBOX_NAME_CHECK_POLICY_DEFAULT = MailboxNameCheckPolicy.ALWAYS;
	
	private static final String BACKEND_IMAP_LOGIN_WITH_DOMAIN = "imap.loginWithDomain";
	private static final String BACKEND_IMAP_ACTIVATE_TLS = "imap.activateTLS";
	protected static final String BACKEND_IMAP_TIMEOUT_VALUE = "imap.timeoutInMs";
	
	private static final String BACKEND_IMAP_DRAFTS_PATH = "imap.mailbox.draft";
	private static final String BACKEND_IMAP_SENT_PATH = "imap.mailbox.sent";
	private static final String BACKEND_IMAP_TRASH_PATH = "imap.mailbox.trash";
	@VisibleForTesting
	static final String BACKEND_IMAP_MAILBOX_NAME_CHECK_POLICY = "imap.mailbox.nameCheckPolicy";

	
	public static class Factory {
		
		protected IniFile.Factory iniFileFactory;

		public Factory() {
			iniFileFactory = new IniFile.Factory();
		}
		
		public EmailConfigurationImpl create(String iniFile) {
			return new EmailConfigurationImpl(iniFileFactory.build(iniFile));
		}
	}
	
	protected final IniFile iniFile;
	
	protected EmailConfigurationImpl(IniFile iniFile) {
		this.iniFile = iniFile;
	}	
	
	private boolean isOptionEnabled(String option) {
		String entryContent = iniFile.getStringValue(option);
		return !"false".equals(entryContent);
	}
	
	@Override
	public boolean activateTls() {
		return isOptionEnabled(BACKEND_IMAP_ACTIVATE_TLS);
	}
	
	@Override
	public boolean loginWithDomain() {
		return isOptionEnabled(BACKEND_IMAP_LOGIN_WITH_DOMAIN);
	}
	
	@Override
	public int imapPort() {
		return IMAP_PORT;
	}

	@Override
	public int imapTimeoutInMilliseconds() {
		return iniFile.getIntValue(BACKEND_IMAP_TIMEOUT_VALUE, BACKEND_IMAP_TIMEOUT_DEFAULT);
	}

	@Override
	public String imapMailboxDraft() {
		return iniFile.getStringValue(BACKEND_IMAP_DRAFTS_PATH, IMAP_DRAFTS_NAME);
	}

	@Override
	public String imapMailboxSent() {
		return iniFile.getStringValue(BACKEND_IMAP_SENT_PATH, IMAP_SENT_NAME);
	}

	@Override
	public String imapMailboxTrash() {
		return iniFile.getStringValue(BACKEND_IMAP_TRASH_PATH, IMAP_TRASH_NAME);
	}

	@Override
	public MailboxNameCheckPolicy mailboxNameCheckPolicy() {
		String strValue = iniFile.getStringValue(BACKEND_IMAP_MAILBOX_NAME_CHECK_POLICY);

		if (strValue == null) {
			return MAILBOX_NAME_CHECK_POLICY_DEFAULT;
		}

		return MailboxNameCheckPolicy.valueOf(strValue.toUpperCase());
	}

}
