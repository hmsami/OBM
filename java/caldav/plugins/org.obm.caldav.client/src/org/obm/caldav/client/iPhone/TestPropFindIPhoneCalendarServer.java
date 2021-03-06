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
 *   obm.org project members
 *
 * ***** END LICENSE BLOCK ***** */

package org.obm.caldav.client.iPhone;

import java.io.InputStream;

import org.obm.caldav.client.CalendarServerPushTest;
import org.obm.caldav.utils.DOMUtils;
import org.w3c.dom.Document;

public class TestPropFindIPhoneCalendarServer extends CalendarServerPushTest {
	
	public void testCalSync() throws Exception {
		InputStream in = loadDataFile("iphone/iPhonePropFind1.xml");
		Document doc = DOMUtils.parse(in);
		Document ret = propFindQuery(doc);
		assertNotNull(ret);

		DOMUtils.logDom(ret);
	}
	
	public void testOption() throws Exception {
		optionQuery();
	}
}
