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
import net.floodlightcontroller.util.OFMessageUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.*;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver13.OFFactoryVer13;
import org.projectfloodlight.openflow.types.*;

/**
 * Unit test for a DHCP server behaviors
 *
 * @author Qing Wang (qw@g.clemson.edu) on 6/31/17.
 */
public class DHCPServerTest extends FloodlightTestCase {

    private static FloodlightContext cntx;
    private static FloodlightModuleContext fmc;

    private static MockThreadPoolService threadPool;

    private static IOFSwitchService switchService;
    protected OFPacketIn packetIn;
    private static IPacket dhcpPacket;
    private static IPacket dhcpPacketReply;
    private static IOFSwitch sw;

    private static DHCPServer dhcpServer;
    private static String testSwitch1DPID = "00:00:00:00:00:00:00:01";
    private static OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);
    private static String broadcastMac = "ff:ff:ff:ff:ff:ff";
    private static String broadcastIp = "255.255.255.255";


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


    }

    /**
     * This class creates many types of packets for dhcp test ( i.e. packet-in/out, dhcp packet, etc )
     * @author Qing Wang (qw@g.clemson.edu) on 8/8/17.
     */
    private static class DHCPPacketFactory {

        private static DHCP getDhcpOfferMessage() {
            DHCPInstance instance = dhcpServer.getInstance("myinstance");
            MacAddress chaddr = MacAddress.of("00:11:22:33:44:55");
            IPv4Address yiaddr = IPv4Address.NONE;
            IPv4Address giaddr = IPv4Address.NONE;
            int xid = 99;

            ArrayList<Byte> requestOrder = new ArrayList<Byte>();
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_SubnetMask.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_Router.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_DomainName.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_DNS.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_Broadcast_IP.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_LeaseTime.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OPtionCode_RebindingTime.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_RenewalTime.getCode());
            requestOrder.add(DHCP.DHCPOptionCode.OptionCode_IPForwarding.getCode());

            return dhcpServer.buildDHCPOfferMessage(instance, chaddr, yiaddr, giaddr, xid, requestOrder);
        }

        private static IPacket getDHCPDiscoverMessage() {
            DHCP discoverPacket = new DHCP();

            List<DHCPOption> dhcpOptionList = new ArrayList<DHCPOption>();
            MacAddress clientMac = MacAddress.of("00:11:22:33:44:55");
            // DHCP header
            discoverPacket.setOpCode(DHCP.DHCPOpCode.OpCode_Request.getCode())
                    .setHardwareType((byte) 1)
                    .setHardwareAddressLength((byte) 6)
                    .setHops((byte) 0)
                    .setTransactionId(99)
                    .setSeconds((short) 0)
                    .setFlags((short) 0)
                    .setClientIPAddress(IPv4Address.FULL_MASK)  // full mask is all 0's
                    .setYourIPAddress(IPv4Address.FULL_MASK)
                    .setServerIPAddress(IPv4Address.FULL_MASK)
                    .setGatewayIPAddress(IPv4Address.FULL_MASK)
                    .setClientHardwareAddress(clientMac);


            // DHCP Options Setup
            DHCPOption msgTypeOption = new DHCPOption()
                    .setCode(DHCP.DHCPOptionCode.OptionCode_MessageType.getCode())
                    .setLength((byte)1)
                    .setData(DHCPServerUtils.intToBytes(1));

            DHCPOption requestIPOption = new DHCPOption()
                    .setCode(DHCP.DHCPOptionCode.OptionCode_RequestedIP.getCode())
                    .setLength((byte)4)
                    .setData(IPAddress.of("192.168.56.10").getBytes());

            byte[] requestParamValue = new byte[4];
            requestParamValue[0] = 1;   // subnet mask
            requestParamValue[1] = 3;   // router
            requestParamValue[2] = 6;   // domain name server
            requestParamValue[4] = 42;  // NTP server
            DHCPOption reqParamOption = new DHCPOption()
                    .setCode(DHCP.DHCPOptionCode.OptionCode_RequestedParameters.getCode())
                    .setLength((byte)4)
                    .setData(requestParamValue);

            // client ID traditionaly set to a "hardware type" value followed by "client hardware address"
            byte[] clientIdValue = new byte[7];
            clientIdValue[0] = 1;   // Ethernet
            System.arraycopy(clientMac.getBytes(), 0, clientIdValue, 1, 6);
            DHCPOption clientIdOption = new DHCPOption()
                    .setCode(DHCP.DHCPOptionCode.OptionCode_ClientID.getCode())
                    .setLength((byte)7)
                    .setData(clientIdValue);

            // End of dhcp options
            DHCPOption endOption = new DHCPOption()
                            .setCode(DHCP.DHCPOptionCode.OptionCode_END.getCode())
                            .setLength((byte)0)
                            .setData(null);


            dhcpOptionList.add(msgTypeOption);
            dhcpOptionList.add(requestIPOption);
            dhcpOptionList.add(reqParamOption);
            dhcpOptionList.add(clientIdOption);
            dhcpOptionList.add(endOption);

            discoverPacket.setOptions(dhcpOptionList);


            IPacket dhcpRequestPacket = new Ethernet()
                    .setSourceMACAddress("00:11:22:33:44:55")
                    .setDestinationMACAddress(broadcastMac)
                    .setVlanID((short) 24)
                    .setEtherType(EthType.IPv4)
                    .setPayload(new IPv4()
                            .setTtl((byte) 250)
                            .setSourceAddress(0)
                            .setDestinationAddress(broadcastIp)
                            .setProtocol(IpProtocol.UDP)
                            .setChecksum((short)0)
                            .setPayload(
                                    new UDP()
                                            .setSourcePort((short) 68)
                                            .setDestinationPort((short) 67)
                                            .setChecksum((short) 0)
                                            .setPayload(discoverPacket)
                            ));


            return dhcpRequestPacket;

        }

        private static OFPacketIn createPacketIn(IPacket packet) {
            byte[] testPacketSerialized = packet.serialize();

            OFPacketIn.Builder packetInBuilder = factory.buildPacketIn();
            if (factory.getVersion() == OFVersion.OF_10) {
                packetInBuilder
                        .setInPort(OFPort.of(1))
                        .setData(testPacketSerialized)
                        .setReason(OFPacketInReason.NO_MATCH);

            } else {
                packetInBuilder
                        .setMatch(factory.buildMatch().setExact(MatchField.IN_PORT, OFPort.of(1)).build())
                        .setData(testPacketSerialized)
                        .setReason(OFPacketInReason.NO_MATCH);

            }

            // Add this "packet-in" packet (has to be Ethernet packet) to the context store
            IFloodlightProviderService.bcStore.put(cntx,
                    IFloodlightProviderService.CONTEXT_PI_PAYLOAD,
                    (Ethernet)packet);

            return packetInBuilder.build();

        }



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
        assertEquals(IPv4Address.of("192.168.56.128"), instance.getServerIP());
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

        OFPacketIn testPacketIn1 = DHCPPacketFactory.createPacketIn(testPacket1);
        assertEquals(IListener.Command.CONTINUE, dhcpServer.receive(sw, testPacketIn1, cntx));

        OFPacketIn testPacketIn2 = DHCPPacketFactory.createPacketIn(testPacket2);
        assertEquals(IListener.Command.CONTINUE, dhcpServer.receive(sw, testPacketIn2, cntx));

        OFPacketIn testPacketIn3 = DHCPPacketFactory.createPacketIn(testPacket3);
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
//        IPacket dhcpDiscoverPacket = getdhcpPacket(DHCP.DHCPOpCode.OpCode_Request, 1);
//        OFPacketIn discoverPacketIn = createPacketIn(dhcpDiscoverPacket, true);


        dhcpServer.enableDHCP();
//        dhcpServer.receive(sw, discoverPacketIn, cntx);


        /* handle dhcp discover message */



    }

    /**
     * Test if we can build expected output action in dhcp offer packet-out
     */
    @Test
    public void testBuildDHCPOfferPacketOut() throws Exception {

        DHCPInstance instance = dhcpServer.getInstance("myinstance");
        DHCP dhcpOfferMessage = DHCPPacketFactory.getDhcpOfferMessage();
        IPv4Address dstIPAddr = IPv4Address.of("1.1.1.1");
        OFPort inPort = OFPort.of(2);

        OFPacketOut dhcpOfferPacketOut = dhcpServer.buildDHCPOfferPacketOut(instance, sw, inPort,
                                        dstIPAddr, dhcpOfferMessage);

        OFActionOutput output = sw.getOFFactory().actions().buildOutput()
                .setMaxLen(0xffFFffFF)
                .setPort(inPort)
                .build();

        assertEquals(output, dhcpOfferPacketOut.getActions().get(0));

    }


    /**
     * Test if we can build expected dhcp offer message
     */
    @Test
    public void testBuildDHCPOfferMessage() throws Exception {

        /* mock a dhcp offer packet */
        DHCPInstance instance = dhcpServer.getInstance("myinstance");
        DHCP dhcpOfferMessage = DHCPPacketFactory.getDhcpOfferMessage();

        assertNotNull(dhcpOfferMessage);
        // check if we get expected header info in dhcp offer
        assertEquals((byte) 2, dhcpOfferMessage.getOpCode());
        assertEquals((byte) 1, dhcpOfferMessage.getHardwareType());
        assertEquals((byte) 6, dhcpOfferMessage.getHardwareAddressLength());
        assertEquals((byte) 0, dhcpOfferMessage.getHops());
        assertEquals(99, dhcpOfferMessage.getTransactionId());
        assertEquals((short) 0, dhcpOfferMessage.getSeconds());
        assertEquals(IPv4Address.FULL_MASK, dhcpOfferMessage.getClientIPAddress());
        assertEquals(MacAddress.of("00:11:22:33:44:55"), dhcpOfferMessage.getClientHardwareAddress());
        assertEquals(IPv4Address.of("192.168.56.128"), dhcpOfferMessage.getServerIPAddress());
        assertEquals(IPv4Address.NONE, dhcpOfferMessage.getYourIPAddress());
        assertEquals(IPv4Address.NONE, dhcpOfferMessage.getGatewayIPAddress());
        assertEquals(null, dhcpOfferMessage.getServerName());

        // check if we get expected options value in dhcp offer
        assertArrayEquals(instance.getSubnetMask().getBytes(), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_SubnetMask).getData());
        assertArrayEquals(instance.getRouterIP().getBytes(), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_Router).getData());
        assertArrayEquals(instance.getServerIP().getBytes(), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_DHCPServerIp).getData());
        assertArrayEquals(instance.getDomainName().getBytes(), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_DomainName).getData());
        assertArrayEquals(instance.getBroadcastIP().getBytes(), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_Broadcast_IP).getData());
        assertArrayEquals(DHCPServerUtils.intToBytes(instance.getLeaseTimeSec()), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_LeaseTime).getData());
        assertArrayEquals(DHCPServerUtils.intToBytes(instance.getRebindTimeSec()), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OPtionCode_RebindingTime).getData());
        assertArrayEquals(DHCPServerUtils.intToBytes(instance.getRenewalTimeSec()), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_RenewalTime).getData());
        assertArrayEquals(DHCPServerUtils.intToBytes(instance.getIpforwarding()?1:0), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_IPForwarding).getData());
        assertArrayEquals(DHCPServerUtils.IPv4ListToByteArr(instance.getDNSServers()), dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_DNS).getData());

        // check un-configured option fields
        assertNull(dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_NTP_IP));
        assertNull(dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_ClientID));
        assertNull(dhcpOfferMessage.getOption(DHCP.DHCPOptionCode.OptionCode_RequestedParameters));

    }


}
