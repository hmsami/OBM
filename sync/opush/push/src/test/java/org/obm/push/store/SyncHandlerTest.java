package org.obm.push.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.matchers.StringContains;
import org.obm.push.backend.IBackend;
import org.obm.push.backend.IContentsExporter;
import org.obm.push.backend.IContinuation;
import org.obm.push.bean.BackendSession;
import org.obm.push.bean.CalendarBusyStatus;
import org.obm.push.bean.CalendarSensitivity;
import org.obm.push.bean.ItemChange;
import org.obm.push.bean.MSEvent;
import org.obm.push.bean.PIMDataType;
import org.obm.push.bean.SyncCollection;
import org.obm.push.bean.SyncCollectionChange;
import org.obm.push.bean.SyncState;
import org.obm.push.exception.ActiveSyncException;
import org.obm.push.exception.DaoException;
import org.obm.push.impl.Responder;
import org.obm.push.protocol.data.CalendarEncoder;
import org.obm.push.protocol.data.EncoderFactory;
import org.obm.push.state.StateMachine;
import org.obm.push.utils.DOMUtils;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;

public class SyncHandlerTest {
	
	@Ignore("I'm wait for the task of 'backendsession stateless' is completed !")
	@Test
	public void testProcessResponseWithAccents() throws IOException, TransformerException, ActiveSyncException, DaoException{

		String expectedString = "éàâ";
		int collectionId = 0;
		String syncKey = "1";
		SyncState syncState = new SyncState("");
		SyncCollection syncCollection = new SyncCollection();
		BackendSession bs = EasyMock.createMock(BackendSession.class);
		
		MSEvent event = new MSEvent();
		event.setStartTime(new Date());
		event.setSubject(expectedString);
		event.setUID("A");
		event.setLocation("quelque part");
		event.setEndTime(new Date());
		event.setSensitivity(CalendarSensitivity.NORMAL);
		event.setBusyStatus(CalendarBusyStatus.BUSY);
		event.setAllDayEvent(false);
		
		bs.checkHint("hint.loadAttendees", true);
		EasyMock.expectLastCall().andReturn(false).anyTimes();
		
		EasyMock.expect(bs.getProtocolVersion()).andReturn(BigDecimal.valueOf(5)).anyTimes();
		

		syncCollection.setSyncKey(syncKey);
		syncCollection.addChange(new SyncCollectionChange("serverId", null, "modType", event, PIMDataType.CALENDAR));
		Collection<SyncCollection> syncCollectionsList = ImmutableList.of(syncCollection);
		
		IContentsExporter exporter = EasyMock.createMock(IContentsExporter.class);
		
		Set<ItemChange> itemChanges = new HashSet<ItemChange>();
		ItemChange itemChange = new ItemChange();
		itemChange.setServerId("serverId");
		itemChange.setData(event);
		itemChanges.add(itemChange);
		EasyMock.expectLastCall().andReturn(itemChanges).anyTimes();

		StateMachine stateMachine = EasyMock.createMock(StateMachine.class); 
		stateMachine.getSyncState(collectionId, syncKey);
		EasyMock.expectLastCall().andReturn(syncState);
		stateMachine.allocateNewSyncKey(bs, collectionId, null);
		EasyMock.expectLastCall().andReturn("newSyncKey");
		
		IBackend backend = EasyMock.createMock(IBackend.class);
		
		EncoderFactory encoderFactory = EasyMock.createMock(EncoderFactory.class);
		encoderFactory.getEncoder(event);
		EasyMock.expectLastCall().andReturn(new CalendarEncoder());
		
		UnsynchronizedItemDao synchronizedItemCache = EasyMock.createMock(UnsynchronizedItemDao.class);
		MonitoredCollectionDao monitoredCollectionStoreService = EasyMock.createMock(MonitoredCollectionDao.class);
		SyncHandler syncHandler = new SyncHandler(backend, encoderFactory, null, exporter, 
				stateMachine, synchronizedItemCache, monitoredCollectionStoreService, null, null);
				
		Responder responder = EasyMock.createMock(Responder.class);
		Capture<Document> document = new Capture<Document>();
		responder.sendResponse(EasyMock.eq("AirSync"), EasyMock.capture(document));
		
		
		IContinuation continuation = EasyMock.createMock(IContinuation.class);
		EasyMock.expect(continuation.getReqId()).andReturn(123);
		
		EasyMock.replay(bs, responder, continuation, stateMachine, backend, encoderFactory);
		
		syncHandler.doTheJob(bs, syncCollectionsList,new HashMap<String, String>(), continuation);
		
		EasyMock.verify(bs, responder, continuation, stateMachine, backend, encoderFactory);
		
		Document sentResponse = document.getValue();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DOMUtils.serialise(sentResponse, out);
		Assert.assertThat(new String(out.toByteArray(), "UTF-8"), 
				StringContains.containsString(expectedString));
	}
}
