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

package org.obm.push.mail.mime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;


public class LeafPartsFinder {

	private Collection<IMimePart> leaves;
	private final boolean filterNested;
	private final IMimePart root;
	
	public LeafPartsFinder(IMimePart root, boolean depthFirst, boolean filterNested) {
		this.root = root;
		this.filterNested = filterNested;
		if (depthFirst) {
			leaves = new ArrayList<IMimePart>();
		} else {
			leaves = new TreeSet<IMimePart>(new Comparator<IMimePart>() {
				@Override
				public int compare(IMimePart o1, IMimePart o2) {
					MimeAddress firstAddr = o1.getAddress();
					MimeAddress secondAddr = o2.getAddress();
					int diffLevel = firstAddr.compareNestLevel(secondAddr);
					if (diffLevel != 0) {
						return diffLevel;
					}
					return firstAddr.getLastIndex() - secondAddr.getLastIndex();
				}
			});
		}
		buildLeafList(root);
	}

	
	private void buildLeafList(IMimePart mp) {
		if (mp.getChildren().isEmpty()) {
			leaves.add(mp);
		} else {
			if (mp != root && mp.isNested() && filterNested) {
				leaves.add(mp);
				return;
			}
			for (IMimePart m : mp.getChildren()) {
				buildLeafList(m);
			}
		}
	}

	public Collection<IMimePart> getLeaves() {
		return leaves;
	}
	
}
