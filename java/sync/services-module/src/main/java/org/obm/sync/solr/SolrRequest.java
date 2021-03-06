/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.sync.solr;

import java.io.Serializable;

import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.aliacom.obm.common.domain.ObmDomain;

public abstract class SolrRequest implements Serializable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private final ObmDomain domain;
	private final SolrService solrService;
	
	public SolrRequest(ObmDomain domain, SolrService solrService) {
		this.domain = domain;
		this.solrService = solrService;
	}

	public abstract void run(CommonsHttpSolrServer server) throws Exception;

	/**
	 * Gets called after processing of this {@link SolrRequest} is done.<br />
	 * It's up to subclasses to implement any logic they need in this method (cleanup, etc.).
	 */
	public void postProcess() {
	}

	public ObmDomain getDomain() {
		return domain;
	}

	public SolrService getSolrService() {
		return solrService;
	}

	public void onError(@SuppressWarnings("unused") Throwable t) {
	}

}
