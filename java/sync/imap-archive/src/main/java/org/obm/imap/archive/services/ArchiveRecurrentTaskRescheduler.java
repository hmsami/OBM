/* ***** BEGIN LICENSE BLOCK *****
 * Copyright (C) 2014  Linagora
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

package org.obm.imap.archive.services;

import org.obm.imap.archive.ImapArchiveModule.LoggerModule;
import org.obm.imap.archive.scheduling.ArchiveDomainTask;
import org.obm.imap.archive.scheduling.ArchiveSchedulerBus;
import org.obm.imap.archive.scheduling.ArchiveSchedulerBus.Events.TaskStatusChanged;
import org.obm.imap.archive.scheduling.ArchiveSchedulingService;
import org.obm.provisioning.dao.exceptions.DaoException;
import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.linagora.scheduling.ScheduledTask.State;

import fr.aliacom.obm.common.domain.ObmDomain;

@Singleton
public class ArchiveRecurrentTaskRescheduler implements ArchiveSchedulerBus.Client {

	private final Logger logger;
	private final ArchiveSchedulingService schedulingService;

	@Inject 
	@VisibleForTesting ArchiveRecurrentTaskRescheduler(
			@Named(LoggerModule.TASK) Logger logger,
			ArchiveSchedulingService schedulingService) {
		this.logger = logger;
		this.schedulingService = schedulingService;
	}
	
	@Subscribe
	public void onTreatmentStateChange(TaskStatusChanged event) {
		ArchiveDomainTask task = event.task();
		if (stateLeadsToReschdule(event.state())) {
			if (task.getArchiveConfiguration().isRecurrent()) {
				reschedule(task);
			}
		}
	}

	private void reschedule(ArchiveDomainTask task) {
		try {
			ObmDomain domain = task.getArchiveConfiguration().getDomain();
			logger.info("A recurrent task for domain {} will be re-scheduled", domain.getName());
			schedulingService.scheduleAsRecurrent(domain);
		} catch (DaoException e) {
			logger.error("Cannot re-schedule a recurrent task", e);
		}
	}

	private boolean stateLeadsToReschdule(State state) {
		return State.TERMINATED == state
			|| State.FAILED == state;
	}
}
