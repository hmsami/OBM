/* ***** BEGIN LICENSE BLOCK *****
 * Version: GPL 2.0
 *
 * The contents of this file are subject to the GNU General Public
 * License Version 2 or later (the "GPL").
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Initial Developer of the Original Code is
 *   MiniG.org project members
 *
 * ***** END LICENSE BLOCK ***** */

package org.obm.push.mail;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.columba.ristretto.message.Address;
import org.minig.imap.FastFetch;
import org.minig.imap.Flag;
import org.minig.imap.FlagsList;
import org.minig.imap.IMAPException;
import org.minig.imap.ListInfo;
import org.minig.imap.ListResult;
import org.minig.imap.SearchQuery;
import org.minig.imap.StoreClient;
import org.obm.configuration.ConfigurationService;
import org.obm.locator.LocatorClient;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.Email;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.MSEmail;
import org.obm.push.bean.SyncState;
import org.obm.push.exception.DaoException;
import org.obm.push.exception.ProcessingEmailException;
import org.obm.push.exception.SendEmailException;
import org.obm.push.exception.ServerErrorException;
import org.obm.push.exception.SmtpInvalidRcptException;
import org.obm.push.exception.StoreEmailException;
import org.obm.push.mail.smtp.SmtpSender;
import org.obm.push.store.EmailDao;
import org.obm.sync.client.calendar.AbstractEventSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EmailManager implements IEmailManager {

	private static final Logger logger = LoggerFactory
			.getLogger(EmailManager.class);
	
	private final EmailDao emailDao;
	private final SmtpSender smtpProvider;
	private final LocatorClient locatorClient;
	private final EmailSync emailSyncCache;
	
	private final boolean loginWithDomain;
	private final boolean activateTLS;
	
	@Inject
	private EmailManager(EmailDao emailDao,
			EmailConfiguration emailConfiguration,
			ConfigurationService configurationService,
			SmtpSender smtpSender, EmailSync emailSyncCache) throws ConfigurationException {
		
		this.emailSyncCache = emailSyncCache;
		this.locatorClient = new LocatorClient(configurationService.getLocatorUrl());
		this.smtpProvider = smtpSender;
		this.emailDao = emailDao;
		this.loginWithDomain = emailConfiguration.loginWithDomain();
		this.activateTLS = emailConfiguration.activateTls();
	}

	@Override
	public String locateImap(BackendSession bs) {
		String locateImap = locatorClient.
				getServiceLocation("mail/imap_frontend", bs.getLoginAtDomain());
		logger.info("Using {} as imap host.", locateImap);
		return locateImap;
	}

	private StoreClient getImapClient(BackendSession bs) {
		final String imapHost = locateImap(bs);
		final String login = getLogin(bs);
		StoreClient storeClient = new StoreClient(imapHost, 143, login, bs.getPassword()); 
		
		logger.info("Creating storeClient with login {} : " +
				"loginWithDomain = {} | activateTLS = {}", 
				new Object[]{login, loginWithDomain, activateTLS});
		
		return storeClient; 
	}

	private String getLogin(BackendSession bs) {
		String login = bs.getLoginAtDomain();
		if (!loginWithDomain) {
			int at = login.indexOf("@");
			if (at > 0) {
				login = login.substring(0, at);
			}
		}
		return login;
	}	

	@Override
	public MailChanges getSync(BackendSession bs, SyncState state,
			Integer devId, Integer collectionId, String collectionName,
			FilterType filter) throws ServerErrorException {
		
		final StoreClient store = getImapClient(bs);
		try {
			login(store);
			String mailBox = parseMailBoxName(store, collectionName);
			store.select(mailBox);
			return emailSyncCache.getSync(store, devId, bs, state,
					collectionId, filter);
		} catch (Throwable e) {
			throw new ServerErrorException(e);
		} finally {
			store.logout();
		}
	}

	@Override
	public List<MSEmail> fetchMails(BackendSession bs, AbstractEventSyncClient calendarClient, Integer collectionId, 
			String collectionName, Collection<Long> uids) throws IOException, IMAPException {
		final List<MSEmail> mails = new LinkedList<MSEmail>();
		final StoreClient store = getImapClient(bs);
		try {
			login(store);
			store.select(parseMailBoxName(store, collectionName));
			
			final MailMessageLoader mailLoader = new MailMessageLoader(store, calendarClient);
			for (final Long uid: uids) {
				final MSEmail email = mailLoader.fetch(collectionId, uid, bs);
				if (email != null) {
					mails.add(email);
				}
			}
			
		} finally {
			store.logout();
		}
		return mails;
	}

	private ListResult listAllFolder(StoreClient store) throws IMAPException {
		try {
			return store.listAll();
		} catch (Throwable e) {
			logger.error("Can't read list of folder.", e);
			throw new IMAPException("Can't read list of folder");
		}

	}

	@Override
	public void updateReadFlag(BackendSession bs, String collectionName,
			Long uid, boolean read) throws IMAPException {
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			String mailBoxName = parseMailBoxName(store, collectionName);
			store.select(mailBoxName);
			FlagsList fl = new FlagsList();
			fl.add(Flag.SEEN);
			store.uidStore(Arrays.asList(uid), fl, read);
			logger.info("flag  change: " + (read ? "+" : "-") + " SEEN"
					+ " on mail " + uid + " in " + mailBoxName);
		} finally {
			store.logout();
		}
	}

	@Override
	public String parseMailBoxName(BackendSession bs, String collectionName)
			throws IMAPException {
		// parse obm:\\adrien@test.tlse.lng\email\INBOX\Sent
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			return parseMailBoxName(store, collectionName);
		} finally {
			store.logout();
		}
	}

	private String parseMailBoxName(StoreClient store, String collectionName) throws IMAPException {
		if (collectionName.toLowerCase().endsWith(EmailConfiguration.IMAP_INBOX_NAME.toLowerCase())) {
			return EmailConfiguration.IMAP_INBOX_NAME;
		}
		
		int slash = collectionName.lastIndexOf("email\\");
		final String boxName = collectionName.substring(slash + "email\\".length());
		final ListResult lr = listAllFolder(store);
		for (final ListInfo i: lr) {
			if (i.getName().toLowerCase().contains(boxName.toLowerCase())) {
				return i.getName();
			}
		}
		throw new IMAPException("Cannot find IMAP folder for collection [ " + collectionName + " ]");
	}
	 
	@Override
	public void delete(BackendSession bs, Integer devId, String collectionPath,
			Integer collectionId, Long uid) throws IMAPException {
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			String mailBoxName = parseMailBoxName(store, collectionPath);
			store.select(mailBoxName);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			logger.info("delete conv id : " + uid);
			store.uidStore(Arrays.asList(uid), fl, true);
			store.expunge();
			deleteMessageInCache(devId, collectionId, Arrays.asList(uid));
		} finally {
			store.logout();
		}
	}

	@Override
	public Long moveItem(BackendSession bs, Integer devId, String srcFolder,
			Integer srcFolderId, String dstFolder, Integer dstFolderId, Long uid)
			throws IMAPException {
		StoreClient store = getImapClient(bs);
		Collection<Long> newUid = null;
		try {
			login(store);
			String srcMailBox = parseMailBoxName(store, srcFolder);
			String dstMailBox = parseMailBoxName(store, dstFolder);
			store.select(srcMailBox);
			List<Long> uids = Arrays.asList(uid);
			newUid = store.uidCopy(uids, dstMailBox);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			logger.info("delete conv id : " + uid);
			store.uidStore(uids, fl, true);
			store.expunge();
			deleteMessageInCache(devId, srcFolderId, Arrays.asList(uid));
			addMessageInCache(store, devId, dstFolderId, uid);
		} finally {
			store.logout();
		}
		if (newUid == null || newUid.isEmpty()) {
			return null;
		}
		return newUid.iterator().next();
	}

	@Override
	public List<InputStream> fetchMIMEMails(BackendSession bs,
			AbstractEventSyncClient calendarClient, String collectionName, Set<Long> uids)
			throws IOException, IMAPException {
		List<InputStream> mails = new LinkedList<InputStream>();
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			store.select(parseMailBoxName(store, collectionName));
			for (Long uid : uids) {
				mails.add(store.uidFetchMessage(uid));
			}
		} finally {
			store.logout();
		}
		return mails;
	}

	private void login(StoreClient store) throws IMAPException {
		if (!store.login(activateTLS)) {
			throw new IMAPException("Cannot log into imap server");
		}
	}

	@Override
	public void setAnsweredFlag(BackendSession bs, String collectionName,
			Long uid) throws IMAPException {
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			String mailBoxName = parseMailBoxName(store, collectionName);
			store.select(mailBoxName);
			FlagsList fl = new FlagsList();
			fl.add(Flag.ANSWERED);
			store.uidStore(Arrays.asList(uid), fl, true);
			logger.info("flag  change: "
					+ ("+ ANSWERED" + " on mail " + uid + " in " + mailBoxName));
		} finally {
			store.logout();
		}
	}

	@Override
	public void sendEmail(BackendSession bs, Address from, Set<Address> setTo,
			Set<Address> setCc, Set<Address> setCci, InputStream mimeMail,
			Boolean saveInSent) throws ProcessingEmailException, SendEmailException, SmtpInvalidRcptException {
		SmtpInvalidRcptException invalidRctp = null;
		InputStream streamMail = new BufferedInputStream(mimeMail);
		try {
			try{
				streamMail.mark(streamMail.available());
			} catch (IOException e) {
				throw new ProcessingEmailException();
			} 
			
			try {
				smtpProvider.sendEmail(bs, from, setTo, setCc, setCci, streamMail);
			} catch (SmtpInvalidRcptException e1) {
				invalidRctp = e1;
			}
			
			if (saveInSent) {
				try {
					streamMail.reset();
					storeInSent(bs, streamMail);
				} catch (Throwable e) {
					logger.error("Error during store mail in Sent folder", e);
				}
			}
		} finally {
			closeStream(streamMail);
		}
		if(invalidRctp != null){
			throw invalidRctp;
		}
	}	
	
	private void closeStream(InputStream mimeMail) {
		if (mimeMail != null) {
			try {
				mimeMail.close();
			} catch (IOException t) {
				logger.error(t.getMessage(), t);
			}
		}
	}
	
	@Override
	public InputStream findAttachment(BackendSession bs, String collectionName,
			Long mailUid, String mimePartAddress) throws IMAPException {

		StoreClient store = getImapClient(bs);
		try {
			login(store);
			String mailBoxName = parseMailBoxName(store, collectionName);
			store.select(mailBoxName);
			return store.uidFetchPart(mailUid, mimePartAddress);
		} finally {
			store.logout();
		}
	}

	@Override
	public void purgeFolder(BackendSession bs, Integer devId,
			String collectionPath, Integer collectionId) throws IMAPException {
		long time = System.currentTimeMillis();
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			String mailBoxName = parseMailBoxName(store, collectionPath);
			store.select(mailBoxName);
			logger.info("Mailbox folder[" + collectionPath
					+ "] will be purged...");
			Collection<Long> uids = store.uidSearch(new SearchQuery());
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			store.uidStore(uids, fl, true);
			store.expunge();
			deleteMessageInCache(devId, collectionId, uids);
			time = System.currentTimeMillis() - time;
			logger.info("Mailbox folder[" + collectionPath + "] was purged in "
					+ time + " millisec. " + uids.size()
					+ " messages have been deleted");
		} finally {
			store.logout();
		}

	}

	@Override
	public Long storeInInbox(BackendSession bs, InputStream mailContent, boolean isRead)
			throws StoreEmailException {
		logger.info("Store mail in folder[Inbox]");
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			return storeMail(store, EmailConfiguration.IMAP_INBOX_NAME, isRead, mailContent, false);
		} catch (Throwable e) {
			throw new StoreEmailException(
					"Error during store mail in Inbox folder", e);
		} finally {
			store.logout();
		}
	}

	/**
	 * Store the mail in the Sent folder storeInSent reset the mimeMail will be
	 * if storeInSent read it
	 * 
	 * @param bs
	 *            the BackendSession
	 * @param mail
	 *            the mail that will be stored
	 * @return the imap uid of the mail
	 * @throws StoreEmailException
	 */
	private long storeInSent(BackendSession bs, InputStream mail)
			throws StoreEmailException {
		logger.info("Store mail in folder[Sent]");
		StoreClient store = getImapClient(bs);
		try {
			login(store);
			String sentFolderName = null;
			ListResult lr = listAllFolder(store);
			for (ListInfo i : lr) {
				if (i.getName().toLowerCase().endsWith("sent")) {
					sentFolderName = i.getName();
				}
			}
			return storeMail(store, sentFolderName,true, mail, true);
		} catch (Throwable e) {
			throw new StoreEmailException(
					"Error during store mail in Sent folder", e);
		} finally {
			store.logout();
		}
	}

	/**
	 * 
	 * @param store
	 *            the StoreClient
	 * @param folderName
	 *            the folder name where the mail will be stored
	 * @param isRead
	 *            if true the message will be stored with SEEN Flag
	 * @param reset
	 *            if true mailContent will be reseted
	 * @return the imap uid of the mail
	 */
	private Long storeMail(StoreClient store, String folderName,
			boolean isRead, InputStream mailContent, boolean reset) {
		Long ret = null;
		if (folderName != null) {
			if (reset && mailContent.markSupported()) {
				mailContent.mark(0);
			}
			FlagsList fl = new FlagsList();
			if(isRead){
				fl.add(Flag.SEEN);
			}
			ret = store.append(folderName, mailContent, fl);
			store.expunge();
		}
		return ret;
	}

	private void deleteMessageInCache(Integer devId, Integer collectionId,
			Collection<Long> mailUids) {
		try {
			emailDao.removeMessages(devId, collectionId, mailUids);
		} catch (DaoException e) {
			logger.error("Error while deleting messages in db", e);
		}
	}

	private void addMessageInCache(StoreClient store, Integer devId,
			Integer collectionId, Long mailUids) {
		Collection<FastFetch> fetch = store.uidFetchFast(ImmutableSet
				.of(mailUids));
		Collection<Email> emails = Collections2.transform(fetch,
				new Function<FastFetch, Email>() {
					@Override
					public Email apply(FastFetch input) {
						return new Email(input.getUid(), input.isRead());
					}
				});
		try {
			emailDao.addMessages(devId, collectionId, emails);
		} catch (DaoException e) {
			logger.error("Error while adding messages in db", e);
		}
	}

	@Override
	public boolean getLoginWithDomain() {
		return loginWithDomain;
	}

	@Override
	public boolean getActivateTLS() {
		return activateTLS;
	}

}
