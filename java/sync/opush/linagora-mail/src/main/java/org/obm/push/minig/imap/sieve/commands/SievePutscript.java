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

package org.obm.push.minig.imap.sieve.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.obm.push.minig.imap.sieve.SieveArg;
import org.obm.push.minig.imap.sieve.SieveCommand;
import org.obm.push.minig.imap.sieve.SieveResponse;
import org.obm.push.utils.FileUtils;

public class SievePutscript extends SieveCommand<Boolean> {

	private String name;
	private byte[] data;

	public SievePutscript(String name, InputStream scriptContent) {
		retVal = false;
		this.name = name;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileUtils.transfer(scriptContent, out, true);
			this.data = out.toByteArray();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	protected List<SieveArg> buildCommand() {
		List<SieveArg> args = new ArrayList<SieveArg>(1);
		args
				.add(new SieveArg(("PUTSCRIPT \"" + name + "\"").getBytes(),
						false));
		args.add(new SieveArg(data, true));
		return args;
	}

	@Override
	public void responseReceived(List<SieveResponse> rs) {
		logger.info("putscript response received.");
		if (commandSucceeded(rs)) {
			retVal = true;
		} else {
			for (SieveResponse sr : rs) {
				logger.error(sr.getData());
			}
		}
	}

}