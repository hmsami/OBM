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
package org.obm.sync.server.handler;

import java.util.Map;

import org.obm.provisioning.dao.exceptions.DomainNotFoundException;
import org.obm.sync.ServerCapability;
import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.AuthFault;
import org.obm.sync.login.LoginBackend;
import org.obm.sync.login.LoginBindingImpl;
import org.obm.sync.login.TrustedLoginBindingImpl;
import org.obm.sync.server.Request;
import org.obm.sync.server.XmlResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import fr.aliacom.obm.common.ObmSyncVersionNotFoundException;
import fr.aliacom.obm.common.setting.SettingsService;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserPassword;
import fr.aliacom.obm.common.user.UserService;
import fr.aliacom.obm.common.user.UserSettings;
import fr.aliacom.obm.services.constant.ObmSyncConfigurationService;

/**
 * Responds to the following urls :
 *
 * <code>/login/doLogin?login=xx&password=yy</code>
 */
public class LoginHandler implements ISyncHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final LoginBindingImpl binding;
	private final TrustedLoginBindingImpl trustedBinding;
	private final SettingsService settingsService;
	private final UserService userService;
	private final ObmSyncConfigurationService configurationService;


	@Inject
	@VisibleForTesting
	LoginHandler(LoginBindingImpl loginBindingImpl,

			TrustedLoginBindingImpl trustedLoginBindingImpl,
			SettingsService settingsService,
			UserService userService,
			ObmSyncConfigurationService configurationService) {

		this.binding = loginBindingImpl;
		this.trustedBinding = trustedLoginBindingImpl;
		this.settingsService = settingsService;
		this.userService = userService;
		this.configurationService = configurationService;
	}



	@Override

	public void handle(Request request,XmlResponder responder) throws Exception {

		String method = request.getMethod();
		if ("doLogin".equals(method)) {
			login(request, responder);
		} else if ("authenticateGlobalAdmin".equals(method)) {
			authenticateGlobalAdmin(request, responder);
		} else if ("authenticateAdmin".equals(method)) {
			authenticateAdmin(request, responder);
		} else if ("trustedLogin".equals(method)) {
			trustedLogin(request, responder);
		} else if ("doLogout".equals(method)) {
			doLogout(request);
		} else {
			responder.sendError("unsupported method: " + method);
			return;
		}

	}



	private void authenticateGlobalAdmin(Request request, XmlResponder responder) {

		try {
			String login = getLogin(request);
			UserPassword password = getPassword(request);
			boolean isPasswordHashed = isPasswordHashed(request);
			String origin = getOrigin(request);
			boolean success = binding.authenticateGlobalAdmin(login, password, origin, isPasswordHashed);

			responder.sendBoolean(success);
		} catch (AuthFault e) {
			responder.sendError(e);

		} catch (IllegalArgumentException e) {

			responder.sendError("Authentication refused : " + e.getMessage());

		} catch (IllegalStateException e) {

			responder.sendError("Authentication refused : " + e.getMessage());

		}

	}



	private void authenticateAdmin(Request request, XmlResponder responder) throws AuthFault {

		try {
			responder.sendBoolean(binding.authenticateAdmin(
					getLogin(request), getPassword(request), getOrigin(request), getDomainName(request), isPasswordHashed(request)));
		} catch (IllegalArgumentException e) {
			responder.sendError("Authentication refused : " + e.getMessage());
		} catch (IllegalStateException e) {
			responder.sendError("Authentication refused : " + e.getMessage());
		}

	}



	private void doLogout(Request request) {
		request.destroySession();
		binding.logout(request.getParameter("sid"));
	}



	private void trustedLogin(Request request, XmlResponder responder) {
		doLogin(request, responder, trustedBinding);
	}



	private void login(Request request, XmlResponder responder) {
		doLogin(request, responder, binding);
	}



	@VisibleForTesting
	void doLogin(Request request, XmlResponder responder, LoginBackend loginBackend) {
		try {
			request.createSession();
			String origin = getOrigin(request);
			String login = getLogin(request);
			UserPassword pass = getPassword(request);
			boolean isPasswordHashed = isPasswordHashed(request);
			if (logger.isDebugEnabled()) {
				request.dumpHeaders();
			}
			AccessToken token = null;
			
			                       try {
			                               token = loginBackend.logUserIn(login, pass, origin, request.getClientIP(), request.getRemoteIP(),
			                                                       request.getLemonLdapLogin(), request.getLemonLdapDomain(), isPasswordHashed);
			                       } catch (DomainNotFoundException e) {
			                               throw new AuthFault(e);
			                       }
			if (token == null) {
				throw new AuthFault("Login failed for user '" + login + "'");
			}

			fillTokenWithUserSettings(token);
			fillTokenWithServerCapabilities(token);
			responder.sendToken(token);
		} catch (AuthFault e) {
            responder.sendError(e);
		} catch (ObmSyncVersionNotFoundException e) {
			responder.sendError("Invalid obm-sync server version");
		} catch (IllegalArgumentException e) {
			responder.sendError("Login refused : " + e.getMessage());
		}
	}



	private String getOrigin(Request request) {
		return request.getMandatoryParameter("origin");
	}



	private String getLogin(Request request) {
		return request.getMandatoryParameter("login");
	}
	
	private UserPassword getPassword(Request request) {
		String password = request.getParameter("password");

		return password != null ? UserPassword.valueOf(password) : null;
	}



	private String getDomainName(Request request) {
		return request.getParameter("domainName");
	}



	private Boolean isPasswordHashed(Request request) {
		return Boolean.valueOf(request.getParameter("isPasswordHashed"));
	}



	private void fillTokenWithUserSettings(AccessToken token) {
		ObmUser user = userService.getUserFromAccessToken(token);
		UserSettings settings = settingsService.getSettings(user);
		token.setUserSettings(settings);
	}



	@VisibleForTesting

	void fillTokenWithServerCapabilities(AccessToken token) {
		Map<ServerCapability, String> capabilities = token.getServerCapabilities();
		for (ServerCapability capability: ServerCapability.values()) {
			capabilities.put(capability, String.valueOf(isServerCapabilityEnabled(capability)));
		}
	}



	private boolean isServerCapabilityEnabled(ServerCapability capability) {
		switch (capability) {
		case CONFIDENTIAL_EVENTS:
			return configurationService.isConfidentialEventsEnabled();
		default:
			return true;

		}

	}

}
