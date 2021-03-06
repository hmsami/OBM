/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
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
package org.obm.server;

import org.obm.server.jetty.JettyServerFactory;
import org.obm.sync.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class EmbeddedServerModule extends AbstractModule {

	private final ServerConfiguration configuration;

	public EmbeddedServerModule(ServerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	protected void configure() {
		Multibinder.newSetBinder(binder(), LifecycleListener.class);
		install(new LoggerModule());
		bind(LifeCycleHandler.class).to(configuration.lifeCycleHandlerClass());
	}
	
	@Provides @Singleton
	public WebServer buildWebServer(Injector injector) {
		return injector
				.getInstance(JettyServerFactory.class)
				.buildServer(configuration);
	}

	public static class LoggerModule extends AbstractModule {

		public static final String CONTAINER = "CONTAINER";
		
		@Override
		protected void configure() {
			bind(Logger.class).annotatedWith(Names.named(CONTAINER)).toInstance(LoggerFactory.getLogger(CONTAINER));
		}
	}
}
