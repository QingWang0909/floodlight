package net.floodlightcontroller.dhcpserver;

import com.google.common.collect.Sets;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.test.FloodlightTestCase;
import net.floodlightcontroller.topology.NodePortTuple;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.types.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Qing Wang (qw@g.clemson.edu) on 7/16/17.
 */
public class DHCPInstanceTest extends FloodlightTestCase {

    @Before
    @Test
    public void setUp() { }


    /**
     * Test if dhcp instance build by the builder has correct attributes, as requested
     */
    @Test
    public void testBuildDHCPInstance() throws Exception {

        Map<MacAddress, IPv4Address> returnStaticAddresses = new HashMap<MacAddress, IPv4Address>();
        returnStaticAddresses.put(MacAddress.of("44:55:66:77:88:99"), IPv4Address.of("192.168.1.3"));
        returnStaticAddresses.put(MacAddress.of("99:88:77:66:55:44"), IPv4Address.of("192.168.1.5"));

        DHCPPool returnDHCPPool = new DHCPPool(IPv4Address.of("192.168.1.3"), 8);

        /* Create DHCP Instance */
        DHCPInstance instance = DHCPInstance.createBuilder().setName("dhcpTestInstance")
                .setServerIP(IPv4Address.of("192.168.1.2"))
                .setServerMac(MacAddress.of("aa:bb:cc:dd:ee:ff"))
                .setBroadcastIP(IPv4Address.of("192.168.1.255"))
                .setRouterIP(IPv4Address.of("192.168.1.1"))
                .setSubnetMask(IPv4Address.of("255.255.255.0"))
                .setStartIP(IPv4Address.of("192.168.1.3"))
                .setEndIP(IPv4Address.of("192.168.1.10"))
                .setLeaseTimeSec(10)
                .setDNSServers(Arrays.asList(IPv4Address.of("10.0.0.1"), IPv4Address.of("10.0.0.2")))
                .setNTPServers(Arrays.asList(IPv4Address.of("10.0.0.3"), IPv4Address.of("10.0.0.4")))
                .setIPforwarding(true)
                .setDomainName("testDomainName")
                .setStaticAddresses(MacAddress.of("44:55:66:77:88:99"), IPv4Address.of("192.168.1.3"))
                .setStaticAddresses(MacAddress.of("99:88:77:66:55:44"), IPv4Address.of("192.168.1.5"))
                .setClientMembers(Sets.newHashSet(MacAddress.of("00:11:22:33:44:55"), MacAddress.of("55:44:33:22:11:00")))
                .setVlanMembers(Sets.newHashSet(VlanVid.ofVlan(100), VlanVid.ofVlan(200)))
                .setNptMembers(Sets.newHashSet(new NodePortTuple(DatapathId.of(1L), OFPort.of(1)), new NodePortTuple(DatapathId.of(2L), OFPort.of(2))))
                .build();


        assertNotNull(instance);
        assertNotNull(instance.getName());
        assertNotNull(instance.getDHCPPool());
        assertEquals("dhcpTestInstance", instance.getName());
        assertEquals(IPv4Address.of("192.168.1.2"), instance.getServerIP());
        assertEquals(MacAddress.of("aa:bb:cc:dd:ee:ff"), instance.getServerMac());
        assertEquals(IPv4Address.of("192.168.1.255"), instance.getBroadcastIP());
        assertEquals(IPv4Address.of("192.168.1.1"), instance.getRouterIP());
        assertEquals(IPv4Address.of("255.255.255.0"), instance.getSubnetMask());
        assertEquals(IPv4Address.of("192.168.1.3"), instance.getStartIPAddress());
        assertEquals(IPv4Address.of("192.168.1.10"), instance.getEndIPAddress());
        assertEquals(10, instance.getLeaseTimeSec());
        assertEquals((int)(10*0.875), instance.getRebindTimeSec());
        assertEquals((int)(10*0.5), instance.getRenewalTimeSec());
        assertEquals(Arrays.asList(IPv4Address.of("10.0.0.1"), IPv4Address.of("10.0.0.2")), instance.getDNSServers());
        assertEquals(Arrays.asList(IPv4Address.of("10.0.0.3"), IPv4Address.of("10.0.0.4")), instance.getNtpServers());
        assertEquals(true, instance.getIpforwarding());
        assertEquals("testDomainName", instance.getDomainName());
        assertEquals(returnStaticAddresses, instance.getStaticAddresseses());
        assertEquals(Sets.newHashSet(MacAddress.of("00:11:22:33:44:55"), MacAddress.of("55:44:33:22:11:00")), instance.getClientMembers());
        assertEquals(Sets.newHashSet(VlanVid.ofVlan(100), VlanVid.ofVlan(200)), instance.getVlanMembers());
        assertEquals(Sets.newHashSet(new NodePortTuple(DatapathId.of(1L), OFPort.of(1)), new NodePortTuple(DatapathId.of(2L), OFPort.of(2))), instance.getNptMembers());

    }


    /**
     * Test if we get expected static addresses configured in DHCP instance
     */
    @Test
    public void testSetStaticAddresses() throws Exception {

        /* Expected valid static address */
        Map<MacAddress, IPv4Address> expectStaticAddresses = new HashMap<MacAddress, IPv4Address>();
        expectStaticAddresses.put(MacAddress.of("44:55:66:77:88:99"), IPv4Address.of("192.168.1.3"));
        expectStaticAddresses.put(MacAddress.of("99:88:77:66:55:44"), IPv4Address.of("192.168.1.5"));

        /* valid static IP address as request */
        DHCPInstance instance1 = DHCPInstance.createBuilder().setName("dhcpTestInstance")
                .setServerIP(IPv4Address.of("192.168.1.2"))
                .setServerMac(MacAddress.of("aa:bb:cc:dd:ee:ff"))
                .setStartIP(IPv4Address.of("192.168.1.3"))
                .setEndIP(IPv4Address.of("192.168.1.10"))
                .setLeaseTimeSec(10)
                .setStaticAddresses(MacAddress.of("44:55:66:77:88:99"), IPv4Address.of("192.168.1.3"))
                .setStaticAddresses(MacAddress.of("99:88:77:66:55:44"), IPv4Address.of("192.168.1.5"))
                .build();
        assertEquals(expectStaticAddresses, instance1.getStaticAddresseses());


        /* invalid static IP address request should return null */
        DHCPInstance instance2 = DHCPInstance.createBuilder()
                .setName("dhcpTestInstance")
                .setServerIP(IPv4Address.of("192.168.1.2"))
                .setServerMac(MacAddress.of("aa:bb:cc:dd:ee:ff"))
                .setStartIP(IPv4Address.of("192.168.1.3"))
                .setEndIP(IPv4Address.of("192.168.1.10"))
                .setLeaseTimeSec(10)
                .setStaticAddresses(MacAddress.of("44:55:66:77:88:99"), IPv4Address.of("20.20.20.20"))
                .setStaticAddresses(MacAddress.of("99:88:77:66:55:44"), IPv4Address.of("30.30.30.30"))
                .build();
        assertTrue(instance2.getStaticAddresseses().isEmpty());

    }


    /**
     * Test if we get expected exception when trying to create a dhcp instance without name
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMissingName() throws Exception {
        DHCPInstance instance = DHCPInstance.createBuilder()
                .setServerIP(IPv4Address.of("192.168.1.2"))
                .setServerMac(MacAddress.of("aa:bb:cc:dd:ee:ff"))
                .setBroadcastIP(IPv4Address.of("192.168.1.255"))
                .setRouterIP(IPv4Address.of("192.168.1.1"))
                .setSubnetMask(IPv4Address.of("255.255.255.0"))
                .setStartIP(IPv4Address.of("192.168.1.3"))
                .setEndIP(IPv4Address.of("192.168.1.10"))
                .setLeaseTimeSec(10)
                .build();

    }


    /**
     * Test if we get expected exception when trying to create a dhcp instance without dhcp pool
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMissingDHCPPool() throws Exception {
        DHCPInstance instance = DHCPInstance.createBuilder()
                .setName("dhcpTestInstance")
                .setServerIP(IPv4Address.of("192.168.1.2"))
                .setServerMac(MacAddress.of("aa:bb:cc:dd:ee:ff"))
                .setBroadcastIP(IPv4Address.of("192.168.1.255"))
                .setRouterIP(IPv4Address.of("192.168.1.1"))
                .setSubnetMask(IPv4Address.of("255.255.255.0"))
                .setLeaseTimeSec(10)
                .build();

    }


    /**
     * Test if we get expected exception when trying to create a dhcp instance with incorrect dhcp pool parameter
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectDHCPPoolParameters() throws Exception {
        DHCPInstance instance = DHCPInstance.createBuilder()
                .setName("dhcpTestInstance")
                .setServerIP(IPv4Address.of("192.168.1.2"))
                .setServerMac(MacAddress.of("aa:bb:cc:dd:ee:ff"))
                .setBroadcastIP(IPv4Address.of("192.168.1.255"))
                .setRouterIP(IPv4Address.of("192.168.1.1"))
                .setSubnetMask(IPv4Address.of("255.255.255.0"))
                .setStartIP(IPv4Address.of("192.168.1.3"))
                .setEndIP(IPv4Address.of("192.168.1.1"))
                .setLeaseTimeSec(10)
                .build();

    }






}
