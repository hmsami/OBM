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
package com.linagora.obm.ui.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.google.inject.Inject;
import com.linagora.obm.ui.service.Services.Logout;
import com.linagora.obm.ui.url.ServiceUrlMapping;

public class HomePage implements Page {
	
	@Inject ServiceUrlMapping mapping;
	@Inject PageFactory pageFactory;
	
	private final WebDriver driver;
	private WebElement information;
	private WebElement informationUser;
	private WebElement informationProfile;
	@FindBy(id="bannerLogoutLink")
	private WebElement logoutLink;
	
	public HomePage(WebDriver driver) {
		this.driver = driver;
	}
	
	@Override
	public void open() {
		driver.get(mapping.lookup(HomePage.class).toExternalForm());
	}
	
	@Override
	public String currentTitle() {
		return driver.getTitle();
	}

	public LoginPage logout() {
		elLogoutLink().click();
		return pageFactory.create(driver, LoginPage.class);
	}

	public LoginPage logoutByUrl() {
		driver.get(mapping.lookup(Logout.class).toExternalForm());
		return pageFactory.create(driver, LoginPage.class);
	}

	public WebElement elInformationPanel() {
		return information;
	}

	public WebElement elInformationUser() {
		return informationUser;
	}

	public WebElement elInformationProfile() {
		return informationProfile;
	}

	public WebElement elLogoutLink() {
		return logoutLink;
	}
}
