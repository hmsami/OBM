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
package org.obm.sync.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.obm.sync.IntegrationTestICSUtils.assertIcsCancelFormat;
import static org.obm.sync.IntegrationTestICSUtils.assertIcsReplyFormat;
import static org.obm.sync.IntegrationTestUtils.newEvent;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.guice.GuiceModule;
import org.obm.push.arquillian.ManagedTomcatGuiceArquillianRunner;
import org.obm.push.arquillian.extension.deployment.DeployForEachTests;
import org.obm.sync.IntegrationTestICSUtils.StoreMessageReceivedListener;
import org.obm.sync.ObmSyncArchiveUtils;
import org.obm.sync.ObmSyncIntegrationTest;
import org.obm.sync.ServicesClientModule.ArquillianLocatorService;
import org.obm.sync.ServicesTestModule;
import org.obm.sync.ServicesWithSocketJMSTestModule;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.EventNotFoundException;
import org.obm.sync.calendar.ServicesClientWithJMSModule.MessageConsumerResourcesManager;
import org.obm.sync.client.calendar.CalendarClient;
import org.obm.sync.client.login.LoginClient;

import com.google.inject.Inject;

import fr.aliacom.obm.common.user.UserPassword;

@RunWith(ManagedTomcatGuiceArquillianRunner.class)
@GuiceModule(ServicesClientWithJMSModule.class)
public class RemoveEventIntegrationTest extends ObmSyncIntegrationTest {

	private static final int TIMEOUT = 5000;

	@Inject ArquillianLocatorService locatorService;
	@Inject CalendarClient calendarClient;
	@Inject LoginClient loginClient;
	@Inject MessageConsumerResourcesManager messageConsumerResourcesManager;
	
	private StoreMessageReceivedListener storeMessageReceivedListener;
	private String userCalendar;

	@Before
	public void setup() throws Exception {
		storeMessageReceivedListener = new StoreMessageReceivedListener();
		messageConsumerResourcesManager.start();
		messageConsumerResourcesManager.getConsumer().setMessageListener(storeMessageReceivedListener);
		userCalendar = "user1@domain.org";
	}
	
	@After
	public void tearDown() throws Exception {
		super.teardown();
		messageConsumerResourcesManager.close();
	}
	
	@Test
	@RunAsClient
	public void testRemoveEventByIdWhenOwnerInInternal(@ArquillianResource @OperateOnDeployment(ARCHIVE) URL baseUrl) throws Exception {
		locatorService.configure(baseUrl);
		
		Event event = newEvent(userCalendar, "user1", "testRemoveEventByIdWhenOwnerInInternal");
		AccessToken token = loginClient.login(userCalendar, UserPassword.valueOf("user1"));
		Event storedEvent = calendarClient.storeEvent(token, userCalendar, event, false, null);

		calendarClient.removeEventById(token, userCalendar, storedEvent.getObmId(), 0, true);
		
		assertThatGetEventByExtIdTriggersEventNotFound(calendarClient, token, userCalendar, event);
		storeMessageReceivedListener.waitForMessageCount(2, TIMEOUT);
		assertIcsCancelFormat(storeMessageReceivedListener.messages().get(1))
			.contains(
				"UID:testRemoveEventByIdWhenOwnerInInternal\r\n")
			.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;PARTSTAT=DECLINED;RSVP=TRUE;CN=Firstname Lastna\r\n" +
				" me;ROLE=OPT-PARTICIPANT:mailto:user1@domain.org\r\n" +
				"DTSTART:20130601T120000Z\r\n" +
				"DURATION:PT0S\r\n" +
				"TRANSP:OPAQUE\r\n" +
				"ORGANIZER;CN=Firstname Lastname:mailto:user1@domain.org\r\n" +
				"PRIORITY:5\r\n" +
				"CLASS:PUBLIC\r\n" +
				"SUMMARY:Title_testRemoveEventByIdWhenOwnerInInternal\r\n");
	}
	
	@Test
	@RunAsClient
	public void testRemoveEventByIdWhenAttendeeInInternal(@ArquillianResource @OperateOnDeployment(ARCHIVE) URL baseUrl) throws Exception {
		locatorService.configure(baseUrl);

		String organizerCalendar = "user2@domain.org";
		Event event = newEvent(organizerCalendar, "user2", "testRemoveEventByIdWhenAttendeeInInternal");
		event.addAttendee(UserAttendee.builder()
				.asAttendee()
				.email(userCalendar)
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.REQ)
				.build());

		AccessToken organizerToken = loginClient.login(organizerCalendar, UserPassword.valueOf("user2"));
		Event storedEvent = calendarClient.storeEvent(organizerToken, organizerCalendar, event, false, null);
		
		AccessToken userToken = loginClient.login(userCalendar, UserPassword.valueOf("user1"));
		calendarClient.removeEventById(userToken, userCalendar, storedEvent.getObmId(), 0, true);

		Event eventFromServer = calendarClient.getEventFromExtId(userToken, userCalendar, event.getExtId());
		assertThat(eventFromServer.findAttendeeFromEmail(organizerCalendar).getParticipation()).isEqualTo(Participation.accepted());
		assertThat(eventFromServer.findAttendeeFromEmail(userCalendar).getParticipation()).isEqualTo(Participation.declined());
		storeMessageReceivedListener.waitForMessageCount(2, TIMEOUT);
		assertIcsReplyFormat(storeMessageReceivedListener.messages().get(1))
			.contains(
				"UID:testRemoveEventByIdWhenAttendeeInInternal\r\n")
			.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;PARTSTAT=DECLINED;RSVP=TRUE;CN=Firstname Lastna\r\n" +
				" me;ROLE=REQ-PARTICIPANT:mailto:user1@domain.org\r\n" +
				"COMMENT:\r\n" +
				"DTSTART:20130601T120000Z\r\n" +
				"DURATION:PT0S\r\n" +
				"TRANSP:OPAQUE\r\n" +
				"ORGANIZER;CN=Firstname Lastname:mailto:user2@domain.org\r\n" +
				"PRIORITY:5\r\n" +
				"CLASS:PUBLIC\r\n" +
				"SUMMARY:Title_testRemoveEventByIdWhenAttendeeInInternal\r\n");
	}
	
	@Test
	@RunAsClient
	public void testRemoveEventByIdWhenAttendeeInExternal(@ArquillianResource @OperateOnDeployment(ARCHIVE) URL baseUrl) throws Exception {
		locatorService.configure(baseUrl);

		String organizerCalendar = "organizer@external.org";
		Event event = newEvent(userCalendar, "user1", "testRemoveEventByIdWhenAttendeeInExternal");
		event.setInternalEvent(false);
		event.addOrReplaceAttendee(userCalendar, UserAttendee.builder()
				.asAttendee()
				.email(userCalendar)
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.REQ)
				.build());
		event.addAttendee(ContactAttendee.builder()
				.asOrganizer()
				.email(organizerCalendar)
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.REQ)
				.build());

		AccessToken token = loginClient.login(userCalendar, UserPassword.valueOf("user1"));
		Event storedEvent = calendarClient.storeEvent(token, userCalendar, event, false, null);
		
		calendarClient.removeEventById(token, userCalendar, storedEvent.getObmId(), 0, true);

		Event eventFromServer = calendarClient.getEventFromExtId(token, userCalendar, event.getExtId());
		assertThat(eventFromServer.findAttendeeFromEmail(organizerCalendar).getParticipation()).isEqualTo(Participation.accepted());
		assertThat(eventFromServer.findAttendeeFromEmail(userCalendar).getParticipation()).isEqualTo(Participation.declined());
		storeMessageReceivedListener.waitForMessageCount(2, TIMEOUT);
		assertIcsReplyFormat(storeMessageReceivedListener.messages().get(1))
			.contains(
				"UID:testRemoveEventByIdWhenAttendeeInExternal\r\n")
			.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;PARTSTAT=DECLINED;RSVP=TRUE;CN=Firstname Lastna\r\n" +
				" me;ROLE=REQ-PARTICIPANT:mailto:user1@domain.org\r\n" +
				"COMMENT:\r\n" +
				"DTSTART:20130601T120000Z\r\n" +
				"DURATION:PT0S\r\n" +
				"TRANSP:OPAQUE\r\n" +
				"ORGANIZER;CN=organizer@external.org:mailto:organizer@external.org\r\n" +
				"PRIORITY:5\r\n" +
				"CLASS:PUBLIC\r\n" +
				"SUMMARY:Title_testRemoveEventByIdWhenAttendeeInExternal\r\n");
	}
	
	@Test
	@RunAsClient
	public void testRemoveEventByExtIdWhenOwnerInInternal(@ArquillianResource @OperateOnDeployment(ARCHIVE) URL baseUrl) throws Exception {
		locatorService.configure(baseUrl);
		
		Event event = newEvent(userCalendar, "user1", "testRemoveEventByExtIdWhenOwnerInInternal");
		AccessToken token = loginClient.login(userCalendar, UserPassword.valueOf("user1"));
		Event storedEvent = calendarClient.storeEvent(token, userCalendar, event, false, null);

		calendarClient.removeEventByExtId(token, userCalendar, storedEvent.getExtId(), 0, true);
		
		assertThatGetEventByExtIdTriggersEventNotFound(calendarClient, token, userCalendar, event);
		storeMessageReceivedListener.waitForMessageCount(2, TIMEOUT);
		assertIcsCancelFormat(storeMessageReceivedListener.messages().get(1))
			.contains(
				"UID:testRemoveEventByExtIdWhenOwnerInInternal\r\n")
			.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;PARTSTAT=DECLINED;RSVP=TRUE;CN=Firstname Lastna\r\n" +
				" me;ROLE=OPT-PARTICIPANT:mailto:user1@domain.org\r\n" +
				"DTSTART:20130601T120000Z\r\n" +
				"DURATION:PT0S\r\n" +
				"TRANSP:OPAQUE\r\n" +
				"ORGANIZER;CN=Firstname Lastname:mailto:user1@domain.org\r\n" +
				"PRIORITY:5\r\n" +
				"CLASS:PUBLIC\r\n" +
				"SUMMARY:Title_testRemoveEventByExtIdWhenOwnerInInternal\r\n");
	}
	
	@Test
	@RunAsClient
	public void testRemoveEventByExtIdWhenAttendeeInInternal(@ArquillianResource @OperateOnDeployment(ARCHIVE) URL baseUrl) throws Exception {
		locatorService.configure(baseUrl);

		String organizerCalendar = "user2@domain.org";
		Event event = newEvent(organizerCalendar, "user2", "testRemoveEventByExtIdWhenAttendeeInInternal");
		event.addAttendee(UserAttendee.builder()
				.asAttendee()
				.email(userCalendar)
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.REQ)
				.build());

		AccessToken organizerToken = loginClient.login(organizerCalendar, UserPassword.valueOf("user2"));
		Event storedEvent = calendarClient.storeEvent(organizerToken, organizerCalendar, event, false, null);
		
		AccessToken userToken = loginClient.login(userCalendar, UserPassword.valueOf("user1"));
		calendarClient.removeEventByExtId(userToken, userCalendar, storedEvent.getExtId(), 0, true);

		Event eventFromServer = calendarClient.getEventFromExtId(userToken, userCalendar, event.getExtId());
		assertThat(eventFromServer.findAttendeeFromEmail(organizerCalendar).getParticipation()).isEqualTo(Participation.accepted());
		assertThat(eventFromServer.findAttendeeFromEmail(userCalendar).getParticipation()).isEqualTo(Participation.declined());
		storeMessageReceivedListener.waitForMessageCount(2, TIMEOUT);
		assertIcsReplyFormat(storeMessageReceivedListener.messages().get(1))
			.contains(
				"UID:testRemoveEventByExtIdWhenAttendeeInInternal\r\n")
			.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;PARTSTAT=DECLINED;RSVP=TRUE;CN=Firstname Lastna\r\n" +
				" me;ROLE=REQ-PARTICIPANT:mailto:user1@domain.org\r\n" +
				"COMMENT:\r\n" +
				"DTSTART:20130601T120000Z\r\n" +
				"DURATION:PT0S\r\n" +
				"TRANSP:OPAQUE\r\n" +
				"ORGANIZER;CN=Firstname Lastname:mailto:user2@domain.org\r\n" +
				"PRIORITY:5\r\n" +
				"CLASS:PUBLIC\r\n" +
				"SUMMARY:Title_testRemoveEventByExtIdWhenAttendeeInInternal\r\n");
	}
	
	@Test
	@RunAsClient
	public void testRemoveEventByExtIdWhenAttendeeInExternal(@ArquillianResource @OperateOnDeployment(ARCHIVE) URL baseUrl) throws Exception {
		locatorService.configure(baseUrl);

		String organizerCalendar = "organizer@external.org";
		Event event = newEvent(userCalendar, "user1", "testRemoveEventByExtIdWhenAttendeeInExternal");
		event.setInternalEvent(false);
		event.addOrReplaceAttendee(userCalendar, UserAttendee.builder()
				.asAttendee()
				.email(userCalendar)
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.REQ)
				.build());
		event.addAttendee(ContactAttendee.builder()
				.asOrganizer()
				.email(organizerCalendar)
				.participation(Participation.accepted())
				.participationRole(ParticipationRole.REQ)
				.build());

		AccessToken token = loginClient.login(userCalendar, UserPassword.valueOf("user1"));
		Event storedEvent = calendarClient.storeEvent(token, userCalendar, event, false, null);
		
		calendarClient.removeEventByExtId(token, userCalendar, storedEvent.getExtId(), 0, true);

		Event eventFromServer = calendarClient.getEventFromExtId(token, userCalendar, event.getExtId());
		assertThat(eventFromServer.findAttendeeFromEmail(organizerCalendar).getParticipation()).isEqualTo(Participation.accepted());
		assertThat(eventFromServer.findAttendeeFromEmail(userCalendar).getParticipation()).isEqualTo(Participation.declined());
		storeMessageReceivedListener.waitForMessageCount(2, TIMEOUT);
		assertIcsReplyFormat(storeMessageReceivedListener.messages().get(1))
			.contains(
				"UID:testRemoveEventByExtIdWhenAttendeeInExternal\r\n")
			.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;PARTSTAT=DECLINED;RSVP=TRUE;CN=Firstname Lastna\r\n" +
				" me;ROLE=REQ-PARTICIPANT:mailto:user1@domain.org\r\n" +
				"COMMENT:\r\n" +
				"DTSTART:20130601T120000Z\r\n" +
				"DURATION:PT0S\r\n" +
				"TRANSP:OPAQUE\r\n" +
				"ORGANIZER;CN=organizer@external.org:mailto:organizer@external.org\r\n" +
				"PRIORITY:5\r\n" +
				"CLASS:PUBLIC\r\n" +
				"SUMMARY:Title_testRemoveEventByExtIdWhenAttendeeInExternal\r\n");
	}

	private void assertThatGetEventByExtIdTriggersEventNotFound(CalendarClient calendarClient,
			AccessToken token, String calendar, Event event) {
		try {
			calendarClient.getEventFromExtId(token, calendar, event.getExtId());
			failBecauseExceptionWasNotThrown(EventNotFoundException.class);
		} catch (Exception e) {
			assertThat(e).isInstanceOf(EventNotFoundException.class);
		}
	}
	
	@DeployForEachTests
	@Deployment(managed=false, name=ARCHIVE)
	public static WebArchive createDeployment() {
		return ObmSyncArchiveUtils
				.createDeployment(ServicesWithSocketJMSTestModule.class)
				.addClass(ServicesTestModule.class);
	}
}
