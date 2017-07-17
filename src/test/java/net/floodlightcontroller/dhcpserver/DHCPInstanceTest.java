package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.test.FloodlightTestCase;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

/**
 * @author Qing Wang (qw@g.clemson.edu) on 7/16/17.
 */
public class DHCPInstanceTest extends FloodlightTestCase {

    private DHCPInstance instance;

    @Before
    @Test
    public void setUp() { }

    @Test
    public void testBuildDHCPInstance() throws Exception {
        // build test instance
        instance = DHCPInstance.createBuilder().setName("testInstance")
                .setServerIP(IPv4Address.of("192.168.1.2"))
                .setServerMac(MacAddress.of("aa:bb:cc:dd:ee:ff"))
                .setBroadcastIP(IPv4Address.of("192.168.1.255"))
                .setRouterIP(IPv4Address.of("192.168.1.1"))
                .setSubnetMask(IPv4Address.of("255.255.255.0"))
                .setStartIP(IPv4Address.of("192.168.1.3"))
                .setEndIP(IPv4Address.of("192.168.1.10"))
                .setLeaseTimeSec(10)
                .build();

        System.out.println(instance.toString());
        instance.getDHCPPool().displayDHCPPool();

    }


}
