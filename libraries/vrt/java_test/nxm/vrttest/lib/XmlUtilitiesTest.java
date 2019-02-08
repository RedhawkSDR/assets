/* ===================== COPYRIGHT NOTICE =====================
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK.
 *
 * REDHAWK is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * ============================================================
 */

package nxm.vrttest.lib;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathExpression;
import nxm.vrt.lib.XmlUtilities;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link XmlUtilities} class. This assumes that 'pkt_src/AppendixA.xml'
 *  and has not been modified recently. Modifications to 'pkt_src/AppendixA.xml'
 *  may require the test cases to be updated.
 */
public class XmlUtilitiesTest implements Testable {
  private File fname;

  @Override
  public Class<?> getTestedClass () {
    return XmlUtilities.class;
  }

  @Override
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    fname = new File("pkt_src/AppendixA.xml");

    if (fname.exists()) {
      tests.add(new TestSet("readDocumentXML(..)",  this, "testXML"));
      tests.add(new TestSet("xpath(..)",            this, "+testXML"));
      tests.add(new TestSet("evalXPath(..)",        this, "+testXML"));
    }
    else {
      tests.add(new TestSet("readDocumentXML(..)",  this, "-testXML"));
      tests.add(new TestSet("xpath(..)",            this, "-testXML"));
      tests.add(new TestSet("evalXPath(..)",        this, "-testXML"));
    }

    return tests;
  }

  @Override
  public void done () throws Exception {
    fname = null;
  }

  @Override
  public String toString () {
    return "Tests for XmlUtilities";
  }

  public void testXML () throws Exception {
    Document xmlDocument = XmlUtilities.readDocumentXML(fname);
    NodeList packetTypes = XmlUtilities.evalXPath("/XML/PACKET_TYPES/PACKET_TYPE",         xmlDocument, NodeList.class);
    Node     packetType0 = XmlUtilities.evalXPath("/XML/PACKET_TYPES/PACKET_TYPE[1]",      xmlDocument, Node.class);
    String   nameViaDoc  = XmlUtilities.evalXPath("/XML/PACKET_TYPES/PACKET_TYPE[1]/NAME", xmlDocument, String.class);
    String   nameViaPT0  = XmlUtilities.evalXPath("./NAME",                                packetType0, String.class);
    Node     junkNode    = XmlUtilities.evalXPath("./JUNK_ITEM",                           packetType0, Node.class);
    String   junkString  = XmlUtilities.evalXPath("./JUNK_ITEM",                           packetType0, "oops");

    assertEquals("Got String",         "StandardDataPacket", nameViaDoc);
    assertEquals("Got Node",           "StandardDataPacket", nameViaPT0);
    assertEquals("Got NodeList",       packetType0,          packetTypes.item(0));
    assertEquals("Got null Node",      null,                junkNode);
    assertEquals("Got default String", "oops",               junkString);

    XPathExpression expr = XmlUtilities.xpath("/XML/PACKET_TYPES/PACKET_TYPE[1]/NAME");
    assertEquals("Expression compiled with xpath(..)", "StandardDataPacket",  expr.evaluate(xmlDocument));
  }

}
