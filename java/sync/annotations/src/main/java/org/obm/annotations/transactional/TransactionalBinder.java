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
package org.obm.annotations.transactional;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TransactionalBinder implements ITransactionAttributeBinder {
	private final TransactionManager transactionManager;
	private Map<Transaction, Transactional> transactionAttributeCache;

	@Inject
	public TransactionalBinder(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		WeakHashMap<Transaction, Transactional> weakHashMap = new WeakHashMap<Transaction, Transactional>();
		this.transactionAttributeCache = Collections.synchronizedMap(weakHashMap);
	}

	@Override
	public Transactional getTransactionalInCurrentTransaction() throws TransactionException {
		Transaction transaction = getCurrentTransaction();
		Transactional transactional = transactionAttributeCache.get(transaction);
		if(transactional == null){
			throw new TransactionException(
					"Nothing is linked to the current transaction");
		}
		return transactional;
	}

	@Override
	public void bindTransactionalToCurrentTransaction(Transactional transactional)
			throws TransactionException {
		Transaction transaction = getCurrentTransaction();
		transactionAttributeCache.put(transaction, transactional);
	}

	@Override
	public void invalidateTransactionalInCurrentTransaction() throws TransactionException {
		Transaction transaction = getCurrentTransaction();
		transactionAttributeCache.remove(transaction);
	}

	private Transaction getCurrentTransaction() throws TransactionException {
		try {
			Transaction transaction = transactionManager.getTransaction();
			if (transaction == null) {
				throw new TransactionException(
						"No active transaction have been found");
			}
			return transaction;
		} catch (SystemException e) {
			throw new TransactionException(e);
		}

	}
}
