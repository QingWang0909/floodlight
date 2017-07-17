package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.*;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.debugcounter.MockDebugCounterService;
import net.floodlightcontroller.test.FloodlightTestCase;


import java.util.*;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import org.easymock.EasyMock;
import org.junit.*;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFActionPopMpls;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * @author Qing Wang (qw@g.clemson.edu) on 6/31/17.
 */
public class DHCPServerTest extends FloodlightTestCase {

    protected FloodlightContext cntx;
    protected IOFSwitchService switchService;
    protected IPacket testPacket;
    protected OFPacketIn packetIn;
    protected IPacket dhcpPacketRequest;
    protected IPacket dhcpPacketReply;
    protected IOFSwitch sw;

    private DHCPServer dhcpServer;
    private MockDebugCounterService mockDebugCounterService;
    private static String testSwitch1DPID = "00:00:00:00:00:00:00:01";

    @Override
    @Before
    public void setUp() throws Exception {
//        super.setUp();

        // Module loader setup
        cntx = new FloodlightContext();
        mockFloodlightProvider = getMockFloodlightProvider();
        mockSwitchManager = getMockSwitchService();
        mockDebugCounterService = new MockDebugCounterService();
        dhcpServer = new DHCPServer();

        FloodlightModuleContext fmc = new FloodlightModuleContext();
        fmc.addService(IFloodlightProviderService.class, mockFloodlightProvider);
        fmc.addService(IDebugCounterService.class, mockDebugCounterService);
        fmc.addService(IOFSwitchService.class, mockSwitchManager);

        mockDebugCounterService.init(fmc);
        mockDebugCounterService.startUp(fmc);
        dhcpServer.init(fmc);
        dhcpServer.startUp(fmc);

        // Mock switches
        sw = EasyMock.createMock(IOFSwitch.class);
        reset(sw);
        expect(sw.getId()).andReturn(DatapathId.of(testSwitch1DPID)).anyTimes();
        expect(sw.getOFFactory()).andReturn(OFFactories.getFactory(OFVersion.OF_13)).anyTimes();
        replay(sw);

        // Load mock switches to the switch map
        Map<DatapathId, IOFSwitch> switches = new HashMap<DatapathId, IOFSwitch>();
        switches.put(DatapathId.of(testSwitch1DPID), sw);
        mockSwitchManager.setSwitches(switches);

        // Build test dhcp request packet
        this.dhcpPacketRequest = new Ethernet()
                .setSourceMACAddress("00:11:22:33:44:55")
                .setDestinationMACAddress("55:66:77:88:99:00")
                .setVlanID((short) 24)
                .setEtherType(EthType.IPv4)
                .setPayload(new IPv4()
                        .setSourceAddress("192.168.1.1")
                        .setDestinationAddress("192.168.1.100")
                        .setPayload(new UDP()
                            .setSourcePort((short) 68)
                            .setDestinationPort((short) 67)
                            .setPayload(new Data(new byte[] {0x01}))
                        )
                );

    }

    protected void setPacketIn(IPacket packet){
        byte[] serializedPacket = packet.serialize();
        this.packetIn = OFFactories.getFactory(OFVersion.OF_13).buildPacketIn()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setMatch(OFFactories.getFactory(OFVersion.OF_13).buildMatch()
                    .setExact(MatchField.IN_PORT, OFPort.of(1))
                    .build()
                    )
                .setData(serializedPacket)
                .build();

        // Add this "packet-in" packet(has to be Ethernet packet) to the context store
        IFloodlightProviderService.bcStore.put(cntx,
                IFloodlightProviderService.CONTEXT_PI_PAYLOAD,
                (Ethernet)packet);

    }


    /* Build a DHCP packet to feed into receive(), test code logic of DHCP enable/disable */
    @Test
    public void testEnableDHCP() throws Exception {
        System.out.println("Run here");
        dhcpServer.enableDHCP();
        this.setPacketIn(dhcpPacketRequest);
        dhcpServer.receive(sw, this.packetIn, cntx);

        // Just test enableDHCP logic
//        assertTrue();

    }

    @Test
    @Ignore
    public void testDisableDHCP() throws Exception {


    }

    @Test
    @Ignore
    public void testIsDHCPPacket() throws Exception {


    }

    @Test
    @Ignore
    public void testSetDHCPOfferMessage() throws Exception {
        dhcpServer.enableDHCP();
        this.setPacketIn(dhcpPacketRequest);
        dhcpServer.receive(sw, this.packetIn, cntx);

//        dhcpServer.sendDHCPOfferMsg();

    }

    @Test
    @Ignore
    public void testSetDHCPAckMessage() throws Exception {


    }

    @Test
    @Ignore
    public void testSetDHCPNAckMessage() throws Exception {


    }


}
