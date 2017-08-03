package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.test.MockThreadPoolService;
import net.floodlightcontroller.packet.*;
import net.floodlightcontroller.test.FloodlightTestCase;

import java.util.*;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import net.floodlightcontroller.topology.NodePortTuple;
import org.easymock.EasyMock;
import org.junit.*;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;

/**
 * Unit test for a DHCP server behaviors
 *
 * @author Qing Wang (qw@g.clemson.edu) on 6/31/17.
 */
public class DHCPServerTest extends FloodlightTestCase {

    protected FloodlightContext cntx;
    protected FloodlightModuleContext fmc;

    protected MockThreadPoolService threadPool;

    protected IOFSwitchService switchService;
    protected IPacket testPacket;
    protected OFPacketIn packetIn;
    protected IPacket dhcpPacket;
    protected IPacket dhcpPacketReply;
    protected IOFSwitch sw;
    protected byte[] testPktSerialized;

    private DHCPServer dhcpServer;
    private static String testSwitch1DPID = "00:00:00:00:00:00:00:01";
    private OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);


    /**
     * Make sure we have all DHCPServer dependency module loaded up and setup
     *
     * We need to add dhcp server attributes ourselves but not from floodlight default property file b/c mock floodlight
     * provider won't read and parse floodlight default properties file
     *
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        /* Module loader setup */
        cntx = new FloodlightContext();
        mockFloodlightProvider = getMockFloodlightProvider();
        dhcpServer = new DHCPServer();

        fmc = new FloodlightModuleContext();
        fmc.addService(IFloodlightProviderService.class, mockFloodlightProvider);
        fmc.addService(IDHCPService.class, dhcpServer);

        /* Add dhcp server attributes */
        fmc.addConfigParam(dhcpServer, "name", "myinstance");
        fmc.addConfigParam(dhcpServer, "subnet-mask", "255.255.255.0");
        fmc.addConfigParam(dhcpServer, "lower-ip-range", "192.168.56.2");
        fmc.addConfigParam(dhcpServer, "upper-ip-range", "192.168.56.10");
        fmc.addConfigParam(dhcpServer, "broadcast-address", "192.168.56.255");
        fmc.addConfigParam(dhcpServer, "router", "192.168.56.1");
        fmc.addConfigParam(dhcpServer, "domain-name", "mydomain");
        fmc.addConfigParam(dhcpServer, "default-lease-time", "3600");
        fmc.addConfigParam(dhcpServer, "hold-lease-time", "120");
        fmc.addConfigParam(dhcpServer, "lease-gc-period", "60");
        fmc.addConfigParam(dhcpServer, "ip-forwarding", "0");
        fmc.addConfigParam(dhcpServer, "controller-mac", "b8:88:e3:0e:05:50");
        fmc.addConfigParam(dhcpServer, "controller-ip", "192.168.56.128");
        fmc.addConfigParam(dhcpServer, "ntp-servers", "108.61.73.242, 108.61.73.243");
        fmc.addConfigParam(dhcpServer, "reserved-static-addresses", "192.168.56.2, 77:22:33:44:55:66; 192.168.56.3, 11:22:33:44:55:66");
        fmc.addConfigParam(dhcpServer, "node-port-tuple", "00:00:00:00:00:00:00:01, 1; 00:00:00:00:00:00:00:01, 1; 00:00:00:00:00:00:00:02, 2");
        fmc.addConfigParam(dhcpServer, "vlanvid", "100, 200, 200");


        /* Initialize dhcp module */
        dhcpServer.init(fmc);
        dhcpServer.startUp(fmc);

        /* Mock switches */
        sw = EasyMock.createMock(IOFSwitch.class);
        reset(sw);
        expect(sw.getId()).andReturn(DatapathId.of(testSwitch1DPID)).anyTimes();
        expect(sw.getOFFactory()).andReturn(OFFactories.getFactory(OFVersion.OF_13)).anyTimes();
        replay(sw);

        // Load mock switches to the switch map
        Map<DatapathId, IOFSwitch> switches = new HashMap<DatapathId, IOFSwitch>();
        switches.put(DatapathId.of(testSwitch1DPID), sw);
        mockSwitchManager.setSwitches(switches);


//        /* Build test dhcp packet */
//        this.dhcpPacket = new Ethernet()
//                .setSourceMACAddress("00:11:22:33:44:55")
//                .setDestinationMACAddress("55:66:77:88:99:00")
//                .setVlanID((short) 24)
//                .setEtherType(EthType.IPv4)
//                .setPayload(new IPv4()
//                        .setSourceAddress("192.168.1.1")
//                        .setDestinationAddress("192.168.1.100")
//                        .setPayload(new UDP()
//                            .setSourcePort((short) 68)
//                            .setDestinationPort((short) 67)
//                            .setPayload(new Data(new byte[] {0x01}))
//                        ));
//
//
//        /* Build mock packet-in */
//        packetIn = createPacketIn(dhcpPacket, true);


    }


    /**
     * Build a dhcp packet for testing purpose
     */
    private IPacket getdhcpPacket(DHCP.DHCPOpCode dhcpOpCode, int dhcpMessageType) {

        List<DHCPOption> dhcpOptions = new ArrayList<DHCPOption>();
        DHCP dhcpPacket = new DHCP();

        // DHCP header
        dhcpPacket.setHardwareType((byte) 1);
        dhcpPacket.setHardwareAddressLength((byte) 6);
        dhcpPacket.setHops((byte) 0);
        dhcpPacket.setTransactionId(99);
        dhcpPacket.setSeconds((short) 0);
        dhcpPacket.setFlags((short) 0);
        dhcpPacket.setClientIPAddress(IPv4Address.FULL_MASK); // full mask is all 0's
        dhcpPacket.setYourIPAddress(IPv4Address.FULL_MASK);
        dhcpPacket.setServerIPAddress(IPv4Address.FULL_MASK);
        dhcpPacket.setGatewayIPAddress(IPv4Address.FULL_MASK);
        dhcpPacket.setClientHardwareAddress(MacAddress.FULL_MASK);

        // DHCP OpCode
        dhcpPacket.setOpCode(dhcpOpCode.getValue());

        // DHCP Mesasge Type
        DHCPOption option = new DHCPOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getValue());
        option.setData(DHCPServerUtils.intToBytes(dhcpMessageType));


        // Other DHCP Options
        option = new DHCPOption();
        option.setCode(DHCP.DHCPOptionCode.OptionCode_SubnetMask.getValue());
        option.setData(DHCPServerUtils.intToBytes(dhcpMessageType));

        dhcpOptions.add(option);
        dhcpPacket.setOptions(dhcpOptions);

        IPacket packet = new Ethernet()
                .setSourceMACAddress("00:11:22:33:44:55")
                .setDestinationMACAddress("55:66:77:88:99:00")
                .setVlanID((short) 24)
                .setEtherType(EthType.IPv4)
                .setPayload(new IPv4()
                        .setSourceAddress("192.168.1.1")
                        .setDestinationAddress("192.168.1.100")
                        .setPayload(
                                new UDP()
                                    .setSourcePort((short) 68)
                                    .setDestinationPort((short) 67)
                                    .setPayload(dhcpPacket)
                        ));


        return packet;

    }


    private OFPacketIn createPacketIn(IPacket testPacket, boolean dhcp) {
        byte[] testPacketSerialized = testPacket.serialize();

        OFPacketIn testPacketIn;
        if (!dhcp) { // do not want dhcp packet-in
            testPacketIn = factory.buildPacketIn()
                    .setMatch(factory.buildMatch()
                            .setExact(MatchField.IN_PORT, OFPort.of(1))
                            .setExact(MatchField.ETH_SRC, MacAddress.of("00:44:33:22:11:00"))
                            .setExact(MatchField.ETH_DST, MacAddress.of("00:11:22:33:44:55"))
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_SRC, IPv4Address.of("192.168.1.1"))
                            .setExact(MatchField.IPV4_DST, IPv4Address.of("192.168.1.2"))
                            .setExact(MatchField.IP_PROTO, IpProtocol.UDP)
                            .setExact(MatchField.UDP_SRC, TransportPort.of(5000))
                            .setExact(MatchField.UDP_DST, TransportPort.of(5001))
                            .build())
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setData(testPacketSerialized)
                    .setReason(OFPacketInReason.NO_MATCH)
                    .build();

        }else { // want dhcp packet-in
            testPacketIn = factory.buildPacketIn()
                    .setMatch(factory.buildMatch()
                            .setExact(MatchField.IN_PORT, OFPort.of(1))
                            .setExact(MatchField.ETH_SRC, MacAddress.of("00:44:33:22:11:00"))
                            .setExact(MatchField.ETH_DST, MacAddress.of("00:11:22:33:44:55"))
                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                            .setExact(MatchField.IPV4_SRC, IPv4Address.of("192.168.1.1"))
                            .setExact(MatchField.IPV4_DST, IPv4Address.of("192.168.1.2"))
                            .setExact(MatchField.IP_PROTO, IpProtocol.UDP)
                            .setExact(MatchField.UDP_SRC, TransportPort.of(68))
                            .setExact(MatchField.UDP_DST, TransportPort.of(67))
                            .build())
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setData(testPacketSerialized)
                    .setReason(OFPacketInReason.NO_MATCH)
                    .build();

        }

        // Add this "packet-in" packet (has to be Ethernet packet) to the context store
        IFloodlightProviderService.bcStore.put(cntx,
                IFloodlightProviderService.CONTEXT_PI_PAYLOAD,
                (Ethernet)testPacket);
        return testPacketIn;

    }

    /**
     * Test if we get an expected instance based on instance name
     */
    @Test
    public void testGetInstanceByName() throws Exception {
        DHCPInstance instance = dhcpServer.getInstance("myinstance");
        assertNotNull(instance);

    }


    /**
     * Test if we get an expected instance based on node port tuple
     */
    @Test
    public void testGetInstanceByNpt() throws Exception {
        NodePortTuple npt1 = new NodePortTuple(DatapathId.of("00:00:00:00:00:00:00:01"), OFPort.of(1));
        DHCPInstance instance1 = dhcpServer.getInstance(npt1);
        assertNotNull(instance1);

        NodePortTuple npt2 = new NodePortTuple(DatapathId.of("00:00:00:00:00:00:00:02"), OFPort.of(2));
        DHCPInstance instance2 = dhcpServer.getInstance(npt2);
        assertNotNull(instance2);

        NodePortTuple npt3 = new NodePortTuple(DatapathId.of("00:00:00:00:00:00:00:03"), OFPort.of(3));
        DHCPInstance instance3 = dhcpServer.getInstance(npt3);
        assertNull(instance3);

        NodePortTuple npt4 = new NodePortTuple(DatapathId.of("00:00:00:00:00:00:00:03"), OFPort.of(2));
        DHCPInstance instance4 = dhcpServer.getInstance(npt4);
        assertNull(instance4);

        NodePortTuple npt5 = new NodePortTuple(DatapathId.of("00:00:00:00:00:00:00:01"), OFPort.of(3));
        DHCPInstance instance5 = dhcpServer.getInstance(npt5);
        assertNull(instance5);

        NodePortTuple npt6 = new NodePortTuple(DatapathId.of("00:00:00:00:00:00:00:01"), OFPort.of(1));
        DHCPInstance instance6 = dhcpServer.getInstance(npt6);
        assertEquals(instance1, instance6);


    }


    /**
     * Test if we get an expected instance based on vlan id
     */
    @Test
    public void testGetInstanceByVlanID() throws Exception {
            DHCPInstance instance1 = dhcpServer.getInstance(VlanVid.ofVlan(100));
            assertNotNull(instance1);

            DHCPInstance instance2 = dhcpServer.getInstance(VlanVid.ofVlan(200));
            assertNotNull(instance2);

            DHCPInstance instance3 = dhcpServer.getInstance(VlanVid.ofVlan(300));
            assertNull(instance3);

    }


    /**
     * Test if we get an expected instance based on ip address
     */


    /**
     * Test if we can get expected instances collection
     */


    /**
     * Test if we correctly create expected dhcp instance via floodlightdefault.properties
     */
    @Test
    public void testCreateInstance() throws Exception {
        DHCPInstance instance = dhcpServer.getInstance("myinstance");

        Map<MacAddress, IPv4Address> returnStaticAddresses = new HashMap<MacAddress, IPv4Address>();
        returnStaticAddresses.put(MacAddress.of("77:22:33:44:55:66"), IPv4Address.of("192.168.56.2"));
        returnStaticAddresses.put(MacAddress.of("11:22:33:44:55:66"), IPv4Address.of("192.168.56.3"));

        assertEquals("myinstance", instance.getName());
        assertEquals(IPv4Address.of("255.255.255.0"), instance.getSubnetMask());
        assertEquals(IPv4Address.of("192.168.56.2"), instance.getStartIPAddress());
        assertEquals(IPv4Address.of("192.168.56.10"), instance.getEndIPAddress());
        assertNotNull(instance.getDHCPPool());
        assertEquals(IPv4Address.of("192.168.56.255"), instance.getBroadcastIP());
        assertEquals(IPv4Address.of("192.168.56.1"), instance.getRouterIP());
        assertEquals("mydomain", instance.getDomainName());
        assertEquals(false, instance.getIpforwarding());
        assertEquals(3600, instance.getLeaseTimeSec());
        assertEquals(3150, instance.getRebindTimeSec());
        assertEquals(1800, instance.getRenewalTimeSec());
        assertEquals(MacAddress.of("b8:88:e3:0e:05:50"), instance.getServerMac());
        assertEquals(IPv4Address.of("192.168.56.1"), instance.getServerIP());
        assertEquals(Arrays.asList(IPv4Address.of("108.61.73.242"), IPv4Address.of("108.61.73.243")), instance.getNtpServers());
        assertEquals(new ArrayList<>(), instance.getDNSServers());
        assertEquals(returnStaticAddresses, instance.getStaticAddresseses());

    }


    /**
     * Test if we can correctly create expected dhcp instance via REST API
     */



    /**
     * Test if we can enable/disable dhcp service as expected
     */
    @Test
    public void testDisableDHCP() throws Exception {
        dhcpServer.disableDHCP();
        assertEquals(IListener.Command.CONTINUE, dhcpServer.receive(sw, this.packetIn, cntx));

    }


    /**
     * Test if we can correctly filter out packet-in message that is not for dhcp purpose
     */
    @Test
    public void testDHCPPacketIn() throws Exception {
        dhcpServer.enableDHCP();

        // build series of test packet that is not dhcp packet
        IPacket testPacket1 = new Ethernet()
                .setDestinationMACAddress("00:11:22:33:44:55")
                .setSourceMACAddress("00:44:33:22:11:00")
                .setEtherType(EthType.IPv4)
                .setPayload(
                        new IPv4()
                                .setTtl((byte) 128)
                                .setSourceAddress("192.168.1.1")
                                .setDestinationAddress("192.168.1.2")
                                .setPayload(new UDP()
                                        .setSourcePort((short) 5000)
                                        .setDestinationPort((short) 5001)
                                        .setPayload(new Data(new byte[] {0x01}))));

        IPacket testPacket2 = new Ethernet()
                .setDestinationMACAddress("00:11:22:33:44:55")
                .setSourceMACAddress("00:44:33:22:11:00")
                .setEtherType(EthType.IPv6);


        IPacket testPacket3 = new Ethernet()
                .setDestinationMACAddress("00:11:22:33:44:55")
                .setSourceMACAddress("00:44:33:22:11:00")
                .setEtherType(EthType.IPv4)
                .setPayload(
                        new IPv4()
                                .setTtl((byte) 128)
                                .setSourceAddress("192.168.1.1")
                                .setDestinationAddress("192.168.1.2")
                                .setPayload(new TCP()
                                        .setSourcePort((short) 5000)
                                        .setDestinationPort((short) 5001)
                                        .setPayload(new Data(new byte[] {0x01}))));

        OFPacketIn testPacketIn1 = createPacketIn(testPacket1, false);
        assertEquals(IListener.Command.CONTINUE, dhcpServer.receive(sw, testPacketIn1, cntx));

        OFPacketIn testPacketIn2 = createPacketIn(testPacket2, false);
        assertEquals(IListener.Command.CONTINUE, dhcpServer.receive(sw, testPacketIn2, cntx));

        OFPacketIn testPacketIn3 = createPacketIn(testPacket3, false);
        assertEquals(IListener.Command.CONTINUE, dhcpServer.receive(sw, testPacketIn3, cntx));


    }


    /**
     * Test if synchronized dhcp pool is working ? ?
     */




    /**
     * Test if dhcp server can correctly handle dhcp discover message
     */
    @Test
    public void testHandleDhcpDiscover() throws Exception {

        /* create a dhcp discover packet-in message w/ options */
        IPacket dhcpDiscoverPacket = getdhcpPacket(DHCP.DHCPOpCode.OpCode_Request, 1);
        OFPacketIn discoverPacketIn = createPacketIn(dhcpDiscoverPacket, true);


        dhcpServer.enableDHCP();
        dhcpServer.receive(sw, discoverPacketIn, cntx);


        /* handle dhcp discover message */



    }


    /**
     * Test if we can build expected dhcp packet
     */
    @Test
    public void testBuildDHCPOfferMessage() throws Exception {

        /* Expected dhcp offer message */


        /* mock a dhcp packet */
        DHCPInstance instance = dhcpServer.getInstance("myinstance");
        MacAddress chaddr = MacAddress.of("00:11:22:33:44:55");
        IPv4Address dstIPAddr = IPv4Address.of("1.1.1.1");
        IPv4Address yiaddr = IPv4Address.NONE;
        IPv4Address giaddr = IPv4Address.NONE;
        int xid = 99;

        ArrayList<Byte> requestOrder = new ArrayList<Byte>();
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_SubnetMask.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_Router.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_DomainName.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_DNS.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_Broadcast_IP.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_LeaseTime.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_NTP_IP.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OPtionCode_RebindingTime.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_RenewalTime.getValue());
        requestOrder.add(DHCP.DHCPOptionCode.OptionCode_IPForwarding.getValue());



        DHCP dhcpOffermessage = dhcpServer.buildDHCPOfferMessage(instance, chaddr, yiaddr, giaddr, xid, requestOrder);
        assertNotNull(dhcpOffermessage);

        System.out.println(dhcpOffermessage.toString());


    }


}
