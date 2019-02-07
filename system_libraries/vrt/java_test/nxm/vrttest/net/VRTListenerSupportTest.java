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

package nxm.vrttest.net;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nxm.vrt.lib.BasicContextPacket;
import nxm.vrt.lib.BasicDataPacket;
import nxm.vrt.lib.DataPacket;
import nxm.vrt.lib.Utilities;
import nxm.vrt.lib.VRTPacket;
import nxm.vrt.net.VRTEventListener;
import nxm.vrt.net.VRTListenerSupport;
import nxm.vrttest.inc.TestSet;
import nxm.vrttest.inc.Testable;
import nxm.vrttest.net.TestListener.Mode;

import static nxm.vrttest.inc.TestRunner.assertEquals;

/** Tests for the {@link VRTListenerSupport} class. */
public class VRTListenerSupportTest implements Testable {
  // Test ID's
  private final Long ID_0 = Utilities.fromStringClassID("12-34-56:ABDC.0000");
  private final Long ID_1 = Utilities.fromStringClassID("12-34-56:ABDC.0001");
  private final Long ID_2 = Utilities.fromStringClassID("12-34-56:ABDC.0002");

  // The various test listeners used
  private final List<TestListener> rcvdListenersNone = new ArrayList<TestListener>();
  private final List<TestListener> rcvdListenersOne  = new ArrayList<TestListener>();
  private final List<TestListener> rcvdListenersTwo  = new ArrayList<TestListener>();
  private final List<TestListener> sentListenersNone = new ArrayList<TestListener>();
  private final List<TestListener> sentListenersOne  = new ArrayList<TestListener>();
  private final List<TestListener> sentListenersTwo  = new ArrayList<TestListener>();
  private final List<TestListener> warnListenersNone = new ArrayList<TestListener>();

  // Test objects
  private final VRTListenerSupport listenerSupport   = new VRTListenerSupport(this);
  private final String             testMsg           = "Test Message";
  private final Throwable          testError         = new Exception();



  @Override
  public Class<?> getTestedClass () {
    return VRTListenerSupport.class;
  }

  @Override
  @SuppressWarnings("deprecation")
  public List<TestSet> init () throws Exception {
    List<TestSet> tests = new ArrayList<TestSet>();

    tests.add(new TestSet("shutdown()",                         this,  "testShutdown"));

    tests.add(new TestSet("reset()",                            this, "+testFireReceivedPacket"));
    tests.add(new TestSet("fireReceivedPacket(..)",             this,  "testFireReceivedPacket"));
    tests.add(new TestSet("fireReceivedDataPacket(..)",         this, "+testFireReceivedInitialContext"));
    tests.add(new TestSet("fireReceivedContextPacket(..)",      this, "+testFireReceivedInitialContext"));
    tests.add(new TestSet("fireReceivedInitialContext(..)",     this,  "testFireReceivedInitialContext"));
    tests.add(new TestSet("fireSentPacket(..)",                 this,  "testFireSentPacket"));
    tests.add(new TestSet("fireErrorOccurred(..)",              this,  "testFireErrorOccurred"));
    tests.add(new TestSet("fireWarningOccurred(..)",            this,  "testFireWarningOccurred"));
    tests.add(new TestSet("addReceivedPacketListener(..)",      this,  "testAddReceivedPacketListener"));
    tests.add(new TestSet("removeReceivedPacketListener(..)",   this, "+testAddReceivedPacketListener"));
    tests.add(new TestSet("removeAllReceivedPacketListeners()", this, "+testAddReceivedPacketListener"));
    tests.add(new TestSet("getReceivedPacketListeners(..)",     this, "+testAddReceivedPacketListener"));
    tests.add(new TestSet("getAllReceivedPacketListeners()",    this, "+testAddReceivedPacketListener"));
    tests.add(new TestSet("addSentPacketListener(..)",          this,  "testAddSentPacketListener"));
    tests.add(new TestSet("removeSentPacketListener(..)",       this, "+testAddSentPacketListener"));
    tests.add(new TestSet("removeAllSentPacketListeners()",     this, "+testAddSentPacketListener"));
    tests.add(new TestSet("getSentPacketListeners(..)",         this, "+testAddSentPacketListener"));
    tests.add(new TestSet("getAllSentPacketListeners()",        this, "+testAddSentPacketListener"));
    tests.add(new TestSet("addErrorWarningListener(..)",        this,  "testAddErrorWarningListener"));
    tests.add(new TestSet("removeErrorWarningListener(..)",     this, "+testAddErrorWarningListener"));
    tests.add(new TestSet("removeAllErrorWarningListeners()",   this, "+testAddErrorWarningListener"));
    tests.add(new TestSet("getAllErrorWarningListeners()",      this, "+testAddErrorWarningListener"));

    // Deprecated...
    tests.add(new TestSet("addReveivedPacketListener(..)",      this, "+testFireReceivedPacket"));
    tests.add(new TestSet("getAllReveivedPacketListeners()",    this, "+testFireReceivedPacket"));
    tests.add(new TestSet("getReveivedPacketListeners(..)",     this, "+testFireReceivedPacket"));
    tests.add(new TestSet("removeReveivedPacketListener(..)",   this, "+testFireReceivedPacket"));
    tests.add(new TestSet("removeAllReveivedPacketListeners()", this, "+testFireReceivedPacket"));

    // Init test objects
    for (int i = 0; i < 8; i++) {
      TestListener rcvd  = new TestListener();
      TestListener rcvd1 = new TestListener();
      TestListener rcvd2 = new TestListener();
      TestListener sent  = new TestListener();
      TestListener sent1 = new TestListener();
      TestListener sent2 = new TestListener();
      TestListener warn  = new TestListener();

      listenerSupport.addReceivedPacketListener(rcvd);
      listenerSupport.addReceivedPacketListener(rcvd1, ID_1);
      listenerSupport.addReceivedPacketListener(rcvd2, ID_2);
      listenerSupport.addSentPacketListener(sent);
      listenerSupport.addSentPacketListener(sent1, ID_1);
      listenerSupport.addSentPacketListener(sent2, ID_2);
      listenerSupport.addErrorWarningListener(warn);

      rcvdListenersNone.add(rcvd);
      rcvdListenersOne.add(rcvd1);
      rcvdListenersTwo.add(rcvd2);
      sentListenersNone.add(sent);
      sentListenersOne.add(sent1);
      sentListenersTwo.add(sent2);
      warnListenersNone.add(warn);
    }
    return tests;
  }

  @Override
  public void done () throws Exception {
    listenerSupport.shutdown();
    rcvdListenersNone.clear();
    rcvdListenersOne.clear();
    rcvdListenersTwo.clear();
    sentListenersNone.clear();
    sentListenersOne.clear();
    sentListenersTwo.clear();
    warnListenersNone.clear();
  }

  @Override
  public String toString () {
    return "Tests for "+getTestedClass();
  }

  public void testShutdown () {
    VRTListenerSupport ls = new VRTListenerSupport(this);

    for (int i = 0; i < 16; i++) {
      ls.addReceivedPacketListener(new TestListener());
      ls.addSentPacketListener(new TestListener());
      ls.addErrorWarningListener(new TestListener());
    }

    ls.shutdown();
    assertEquals("shutdown() -> ReceivedPacketListeners", 0, ls.getAllReceivedPacketListeners().size());
    assertEquals("shutdown() -> SentPacketListeners",     0, ls.getAllSentPacketListeners().size());
    assertEquals("shutdown() -> ErrorWarningListeners",   0, ls.getAllErrorWarningListeners().size());
  }

  /** Resets the listeners to null values. */
  private void resetListeners () {
    for (TestListener l : rcvdListenersNone) l.reset();
    for (TestListener l : rcvdListenersOne ) l.reset();
    for (TestListener l : rcvdListenersTwo ) l.reset();
    for (TestListener l : sentListenersNone) l.reset();
    for (TestListener l : sentListenersOne ) l.reset();
    for (TestListener l : sentListenersTwo ) l.reset();
    for (TestListener l : warnListenersNone) l.reset();
  }

  private void _testFirePacket (Mode mode, Long id, VRTPacket p) {
    _testFirePacket(mode, id, p, null, null, null);
  }

  private void _testFirePacket (Mode mode, Long id, VRTPacket p,
                                DataPacket data, VRTPacket ctx,
                                Map<Integer,VRTPacket> context) {
    // ==== INIT VARIABLES =====================================================
    String                 txt = "fire"+mode.toString().toUpperCase().charAt(0)
                                       +mode.toString().substring(1)+"(..)";
    List<TestListener>     none;
    List<TestListener>     one;
    List<TestListener>     two;
    String                 msg = null;
    Throwable              err = null;

    switch (mode) {
      case receivedInitialContext: // FALLTHROUGH
      case receivedDataPacket:     // FALLTHROUGH
      case receivedContextPacket:  // FALLTHROUGH
      case receivedPacket:
        none = rcvdListenersNone;
        one  = rcvdListenersOne;
        two  = rcvdListenersTwo;
        break;
      case sentPacket:
        none = sentListenersNone;
        one  = sentListenersOne;
        two  = sentListenersTwo;
        break;
      case errorOccurred:          // FALLTHROUGH
      case warningOccurred:
        msg  = testMsg;
        err  = testError;
        none = warnListenersNone;
        one  = null;
        two  = null;
        break;
      default:
        throw new AssertionError("Unsupported mode "+mode);
    }

    // ==== RESET & CALL FUNCTION ==============================================
    resetListeners();
    if (p != null) p.setClassIdentifier(id);

    switch (mode) {
      case receivedInitialContext: listenerSupport.fireReceivedInitialContext(msg, data, ctx, context); break;
      case receivedDataPacket:     listenerSupport.fireReceivedDataPacket((DataPacket)p); break;
      case receivedContextPacket:  listenerSupport.fireReceivedContextPacket(p);          break;
      case receivedPacket:         listenerSupport.fireReceivedPacket(p);                 break;
      case sentPacket:             listenerSupport.fireSentPacket(p);                     break;
      case errorOccurred:          listenerSupport.fireErrorOccurred(msg, err, p);        break;
      case warningOccurred:        listenerSupport.fireWarningOccurred(msg, err, p);      break;
    }

    // ==== TEST RESULTS =======================================================
    for (TestListener l : none) {
      assertEquals(txt+" -> mode",    mode,    l.mode);
      assertEquals(txt+" -> source",  this,    l.event.getSource());
      assertEquals(txt+" -> packet",  p,       l.packet);
      assertEquals(txt+" -> message", msg,     l.message);
      assertEquals(txt+" -> data",    data,    l.data);
      assertEquals(txt+" -> ctx",     ctx,     l.ctx);
      assertEquals(txt+" -> context", context, l.context);
      assertEquals(txt+" -> error",   err,     l.error);
    }

    if (one == null) {
      // PASS
    }
    else if (id == ID_1) {
      for (TestListener l : one) {
        assertEquals(txt+" -> mode",    mode,    l.mode);
        assertEquals(txt+" -> source",  this,    l.event.getSource());
        assertEquals(txt+" -> packet",  p,       l.packet);
        assertEquals(txt+" -> message", msg,     l.message);
        assertEquals(txt+" -> data",    data,    l.data);
        assertEquals(txt+" -> ctx",     ctx,     l.ctx);
        assertEquals(txt+" -> context", context, l.context);
        assertEquals(txt+" -> error",   err,     l.error);
      }
    }
    else {
      for (TestListener l : one) {
        assertEquals(txt+" -> source", Mode.none, l.mode);
      }
    }

    if (two == null) {
      // PASS
    }
    else if (id == ID_2) {
      for (TestListener l : two) {
        assertEquals(txt+" -> mode",    mode,    l.mode);
        assertEquals(txt+" -> source",  this,    l.event.getSource());
        assertEquals(txt+" -> packet",  p,       l.packet);
        assertEquals(txt+" -> message", msg,     l.message);
        assertEquals(txt+" -> data",    data,    l.data);
        assertEquals(txt+" -> ctx",     ctx,     l.ctx);
        assertEquals(txt+" -> context", context, l.context);
        assertEquals(txt+" -> error",   err,     l.error);
      }
    }
    else {
      for (TestListener l : two) {
        assertEquals(txt+" -> source", Mode.none, l.mode);
      }
    }
  }

  public void testFireReceivedInitialContext () {
    listenerSupport.reset();
    DataPacket             data    = new BasicDataPacket();
    VRTPacket              ctx     = new BasicContextPacket();
    Map<Integer,VRTPacket> context = new LinkedHashMap<Integer,VRTPacket>();

    _testFirePacket(Mode.receivedInitialContext, null, null, data, ctx, context);

    // fireReceivedDataPacket(..)
    _testFirePacket(Mode.receivedDataPacket, null, new BasicDataPacket());
    _testFirePacket(Mode.receivedDataPacket, ID_0, new BasicDataPacket());
    _testFirePacket(Mode.receivedDataPacket, ID_1, new BasicDataPacket());
    _testFirePacket(Mode.receivedDataPacket, ID_2, new BasicDataPacket());

    // fireReceivedContextPacket(..)
    _testFirePacket(Mode.receivedContextPacket, ID_0, new BasicContextPacket());
    _testFirePacket(Mode.receivedContextPacket, ID_1, new BasicContextPacket());
    _testFirePacket(Mode.receivedContextPacket, ID_2, new BasicContextPacket());
  }

  public void testFireReceivedPacket () {
    listenerSupport.reset();
    _testFirePacket(Mode.receivedPacket, null, new BasicDataPacket());
    _testFirePacket(Mode.receivedPacket, ID_0, new BasicDataPacket());
    _testFirePacket(Mode.receivedPacket, ID_0, new BasicContextPacket());
    _testFirePacket(Mode.receivedPacket, ID_1, new BasicDataPacket());
    _testFirePacket(Mode.receivedPacket, ID_1, new BasicContextPacket());
    _testFirePacket(Mode.receivedPacket, ID_2, new BasicDataPacket());
    _testFirePacket(Mode.receivedPacket, ID_2, new BasicContextPacket());
  }

  public void testFireSentPacket () {
    listenerSupport.reset();
    _testFirePacket(Mode.sentPacket, null, new BasicDataPacket());
    _testFirePacket(Mode.sentPacket, ID_0, new BasicDataPacket());
    _testFirePacket(Mode.sentPacket, ID_0, new BasicContextPacket());
    _testFirePacket(Mode.sentPacket, ID_1, new BasicDataPacket());
    _testFirePacket(Mode.sentPacket, ID_1, new BasicContextPacket());
    _testFirePacket(Mode.sentPacket, ID_2, new BasicDataPacket());
    _testFirePacket(Mode.sentPacket, ID_2, new BasicContextPacket());
  }

  public void testFireErrorOccurred () {
    listenerSupport.reset();
    _testFirePacket(Mode.errorOccurred, null, null);
  }

  public void testFireWarningOccurred () {
    listenerSupport.reset();
    _testFirePacket(Mode.warningOccurred, null, null);
  }

  /** Checks the listeners after an add/remove. */
  private void checkListeners (String msg, List<VRTEventListener> expAll, List<VRTEventListener> actAll) {
    assertEquals(msg+" -> number of listeners", expAll.size(), actAll.size());
    assertEquals(msg+" -> listeners",           true,          actAll.containsAll(expAll));
  }

  /** Checks the listeners after an add/remove. */
  private void checkListeners (String msg, List<VRTEventListener> expAll, List<VRTEventListener> expNone,
                                           List<VRTEventListener> expOne, List<VRTEventListener> expTwo,
                                           List<VRTEventListener> actAll, List<VRTEventListener> actNone,
                                           List<VRTEventListener> actOne, List<VRTEventListener> actTwo) {
    checkListeners(msg+" -> ALL",               expAll,  actAll );
    checkListeners(msg+" -> null (deprecated)", expNone, actNone);
    checkListeners(msg+" -> ID_1 (deprecated)", expOne,  actOne );
    checkListeners(msg+" -> ID_2 (deprecated)", expTwo,  actTwo );
  }

  @SuppressWarnings("deprecation")
  public void testAddReceivedPacketListener () {
    VRTListenerSupport     ls   = new VRTListenerSupport(this);
    List<VRTEventListener> all  = new ArrayList<VRTEventListener>(); // all listeners
    List<VRTEventListener> none = new ArrayList<VRTEventListener>(); // deprecated ClassID=null
    List<VRTEventListener> one  = new ArrayList<VRTEventListener>(); // deprecated ClassID=ID_1
    List<VRTEventListener> two  = new ArrayList<VRTEventListener>(); // deprecated ClassID=ID_2

    // ==== ADD/GET LISTENERS ==================================================
    for (int i = 0; i < 16; i++) {
      TestListener l = new TestListener();
      ls.addReceivedPacketListener(l);
      all.add(l);
      none.add(l);
      checkListeners("addReceivedPacketListener(l)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }
    for (int i = 0; i < 16; i++) {
      TestListener l = new TestListener();
      ls.addReceivedPacketListener(l, null);
      all.add(l);
      none.add(l);
      checkListeners("addReceivedPacketListener(l,null)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }
    for (int i = 0; i < 8; i++) {
      TestListener l = new TestListener();
      ls.addReceivedPacketListener(l, ID_1);
      all.add(l);
      one.add(l);
      checkListeners("addReceivedPacketListener(l,ID_1)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }
    for (int i = 0; i < 8; i++) {
      TestListener l = new TestListener();
      ls.addReceivedPacketListener(l, ID_2);
      all.add(l);
      two.add(l);
      checkListeners("addReceivedPacketListener(l,ID_2)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }

    // ==== REMOVE LISTENERS ===================================================
    for (int i = 0; i < 4; i++) {
      VRTEventListener l = all.remove(0);
      none.remove(l);
      ls.removeReceivedPacketListener(l);

      checkListeners("removeReceivedPacketListener(l)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }
    int n = none.size();
    for (int i = n-1; i >= n-5; i--) {
      VRTEventListener l = none.remove(i);
      all.remove(l);
      ls.removeReceivedPacketListener(l, null);

      checkListeners("removeReceivedPacketListener(l,null)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }
    for (int i = 7; i >= 0; i-=2) {
      VRTEventListener l = one.remove(i);
      all.remove(l);
      ls.removeReceivedPacketListener(l, ID_1);

      checkListeners("removeReceivedPacketListener(l,ID_1)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }
    for (int i = 7; i >= 0; i-=2) {
      VRTEventListener l = two.remove(i);
      all.remove(l);
      ls.removeReceivedPacketListener(l, ID_2);

      checkListeners("removeReceivedPacketListener(l,ID_2)", all, none, one, two,
                     ls.getAllReceivedPacketListeners(),
                     ls.getReceivedPacketListeners(null),
                     ls.getReceivedPacketListeners(ID_1),
                     ls.getReceivedPacketListeners(ID_2));
    }

    ls.removeAllReceivedPacketListeners();
    all.clear();
    none.clear();
    one.clear();
    two.clear();

    checkListeners("removeAllSentPacketListeners()", all, none, one, two,
                   ls.getAllReceivedPacketListeners(),
                   ls.getReceivedPacketListeners(null),
                   ls.getReceivedPacketListeners(ID_1),
                   ls.getReceivedPacketListeners(ID_2));
  }

  @SuppressWarnings("deprecation")
  public void testAddSentPacketListener () {
    VRTListenerSupport     ls   = new VRTListenerSupport(this);
    List<VRTEventListener> all  = new ArrayList<VRTEventListener>(); // all listeners
    List<VRTEventListener> none = new ArrayList<VRTEventListener>(); // deprecated ClassID=null
    List<VRTEventListener> one  = new ArrayList<VRTEventListener>(); // deprecated ClassID=ID_1
    List<VRTEventListener> two  = new ArrayList<VRTEventListener>(); // deprecated ClassID=ID_2

    // ==== ADD/GET LISTENERS ==================================================
    for (int i = 0; i < 16; i++) {
      TestListener l = new TestListener();
      ls.addSentPacketListener(l);
      all.add(l);
      none.add(l);
      checkListeners("addSentPacketListener(l)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }
    for (int i = 0; i < 16; i++) {
      TestListener l = new TestListener();
      ls.addSentPacketListener(l, null);
      all.add(l);
      none.add(l);
      checkListeners("addSentPacketListener(l,null)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }
    for (int i = 0; i < 8; i++) {
      TestListener l = new TestListener();
      ls.addSentPacketListener(l, ID_1);
      all.add(l);
      one.add(l);
      checkListeners("addSentPacketListener(l,ID_1)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }
    for (int i = 0; i < 8; i++) {
      TestListener l = new TestListener();
      ls.addSentPacketListener(l, ID_2);
      all.add(l);
      two.add(l);
      checkListeners("addSentPacketListener(l,ID_2)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }

    // ==== REMOVE LISTENERS ===================================================
    for (int i = 0; i < 4; i++) {
      VRTEventListener l = all.remove(0);
      none.remove(l);
      ls.removeSentPacketListener(l);

      checkListeners("removeSentPacketListener(l)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }
    int n = none.size();
    for (int i = n-1; i >= n-5; i--) {
      VRTEventListener l = none.remove(i);
      all.remove(l);
      ls.removeSentPacketListener(l, null);

      checkListeners("removeSentPacketListener(l,null)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }
    for (int i = 7; i >= 0; i-=2) {
      VRTEventListener l = one.remove(i);
      all.remove(l);
      ls.removeSentPacketListener(l, ID_1);

      checkListeners("removeSentPacketListener(l,ID_1)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }
    for (int i = 7; i >= 0; i-=2) {
      VRTEventListener l = two.remove(i);
      all.remove(l);
      ls.removeSentPacketListener(l, ID_2);

      checkListeners("removeSentPacketListener(l,ID_2)", all, none, one, two,
                     ls.getAllSentPacketListeners(),
                     ls.getSentPacketListeners(null),
                     ls.getSentPacketListeners(ID_1),
                     ls.getSentPacketListeners(ID_2));
    }

    ls.removeAllSentPacketListeners();
    all.clear();
    none.clear();
    one.clear();
    two.clear();

    checkListeners("removeAllSentPacketListeners()", all, none, one, two,
                   ls.getAllSentPacketListeners(),
                   ls.getSentPacketListeners(null),
                   ls.getSentPacketListeners(ID_1),
                   ls.getSentPacketListeners(ID_2));
  }

  public void testAddErrorWarningListener () {
    VRTListenerSupport     ls  = new VRTListenerSupport(this);
    List<VRTEventListener> all = new ArrayList<VRTEventListener>();

    // ==== ADD/GET LISTENERS ==================================================
    for (int i = 0; i < 16; i++) {
      TestListener l = new TestListener();
      ls.addErrorWarningListener(l);
      all.add(l);
      checkListeners("addErrorWarningListener(l)", all,
                     ls.getAllErrorWarningListeners());
    }

    // ==== REMOVE LISTENERS ===================================================
    for (int i = 15; i >= 0; i-=2) {
      VRTEventListener l = all.remove(i);
      ls.removeErrorWarningListener(l);

      checkListeners("removeErrorWarningListener(l)", all,
                     ls.getAllErrorWarningListeners());
    }

    ls.removeAllErrorWarningListeners();
    all.clear();

    checkListeners("removeAllErrorWarningListeners()", all,
                   ls.getAllErrorWarningListeners());
  }
}
