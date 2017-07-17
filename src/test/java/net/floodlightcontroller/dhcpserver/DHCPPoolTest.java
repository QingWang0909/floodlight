package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.test.FloodlightTestCase;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Qing Wang (qw@g.clemson.edu) on 7/12/17.
 */
public class DHCPPoolTest extends FloodlightTestCase {

    private DHCPPool dhcpPool;

    @Before
    @Override
    public void setUp() throws Exception {
        //dhcpPool.displayDHCPPool();
    }

    private DHCPPool initDHCPPool(IPv4Address startIP, int PoolSize) {
        return new DHCPPool(startIP, PoolSize);
    }

    @Test
    public void testDHCPPoolStatusMonitoring() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 3);
        DHCPBinding lease;

        /* test dhcp pool initial state */
        assertEquals(IPv4Address.of("192.168.1.1"), dhcpPool.getStartIP());
        assertEquals(3, dhcpPool.getPoolSize());
        assertEquals(3, dhcpPool.getPoolAvailability());
        assertEquals(false, dhcpPool.isPoolFull());
        assertEquals(true, dhcpPool.hasAvailableSpace());

        /* test dhcp pool status after one lease */
        DHCPBinding lease1 = dhcpPool.findLeaseBinding(MacAddress.of("00:11:22:33:44:55"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("00:11:22:33:44:55"), 100L);
        assertEquals(3, dhcpPool.getPoolSize());
        assertEquals(2, dhcpPool.getPoolAvailability());
        assertEquals(false, dhcpPool.isPoolFull());
        assertEquals(true, dhcpPool.hasAvailableSpace());

        /* test dhcp pool status once pool is full */
        MacAddress clientMac2 = MacAddress.of("AA:BB:CC:DD:EE:FF");
        DHCPBinding lease2 = dhcpPool.findLeaseBinding(clientMac2);
        dhcpPool.setLeaseBinding(lease2, clientMac2, 200L);

        MacAddress clientMac3 = MacAddress.of("FF:EE:DD:CC:BB:AA");
        DHCPBinding lease3 = dhcpPool.findLeaseBinding(clientMac3);
        dhcpPool.setLeaseBinding(lease3, clientMac3, 200L);

        assertEquals(3, dhcpPool.getPoolSize());
        assertEquals(0, dhcpPool.getPoolAvailability());
        assertEquals(true, dhcpPool.isPoolFull());
        assertEquals(false, dhcpPool.hasAvailableSpace());

        /* test dhcp pool status after pool is full */
        MacAddress clientMac4 = MacAddress.of("44:55:66:77:88:99");
        DHCPBinding lease4 = dhcpPool.findLeaseBinding(clientMac4);
        assertNull(lease4);
        assertEquals(3, dhcpPool.getPoolSize());
        assertEquals(0, dhcpPool.getPoolAvailability());
        assertEquals(true, dhcpPool.isPoolFull());
        assertEquals(false, dhcpPool.hasAvailableSpace());

    }


    @Test
    public void testDHCPPoolNormalOperation() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 5);
        dhcpPool.displayDHCPPool();

        /* remove starter IP */
        dhcpPool.removeIPv4FromDHCPPool(IPv4Address.of("192.168.1.1"));
        assertEquals(IPv4Address.of("192.168.1.2"), dhcpPool.getStartIP());
        assertEquals(4, dhcpPool.getPoolSize());

        /* add random IP */
        dhcpPool.addIPv4ToDHCPPool(IPv4Address.of("192.168.1.6"));
        assertEquals(IPv4Address.of("192.168.1.2"), dhcpPool.getStartIP());
        assertEquals(5, dhcpPool.getPoolSize());

        /* add IP as new new starter IP */
        dhcpPool.addIPv4ToDHCPPool(IPv4Address.of("192.168.1.1"));
        assertEquals(IPv4Address.of("192.168.1.1"), dhcpPool.getStartIP());
        assertEquals(6, dhcpPool.getPoolSize());
        assertEquals(6, dhcpPool.getPoolAvailability());

        /* remove random IP */
        dhcpPool.removeIPv4FromDHCPPool(IPv4Address.of("192.168.1.3"));
        assertEquals(5, dhcpPool.getPoolSize());
        assertEquals(5, dhcpPool.getPoolAvailability());

        /* add IP that already exists in the pool */
        dhcpPool.addIPv4ToDHCPPool(IPv4Address.of("192.168.1.1"));
        assertEquals(5, dhcpPool.getPoolSize());
        assertEquals(5, dhcpPool.getPoolAvailability());

        /* remove IP that does not exists in the pool */
        dhcpPool.removeIPv4FromDHCPPool(IPv4Address.of("192.168.1.3"));
        assertEquals(5, dhcpPool.getPoolSize());
        assertEquals(5, dhcpPool.getPoolAvailability());

    }


    @Test
    public void testGetLeaseBinding() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 2);

        /* Same Mac should return same dhcp binding */
        DHCPBinding lease1 = dhcpPool.findLeaseBinding(MacAddress.of("00:11:22:33:44:55"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("00:11:22:33:44:55"), 10L);
        DHCPBinding lease2 = dhcpPool.findLeaseBinding(MacAddress.of("00:11:22:33:44:55"));
        assertSame(lease1, lease2);

        /* if DHCP pool is full then return null */
        assertTrue(dhcpPool.hasAvailableSpace());
        DHCPBinding lease3 = dhcpPool.findLeaseBinding(MacAddress.of("55:44:33:22:11:00"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("55:44:33:22:11:00"), 10L);
        DHCPBinding lease4 = dhcpPool.findLeaseBinding(MacAddress.of("AA:BB:CC:DD:EE:FF"));
        assertNull(lease4);

    }


    @Test
    public void testGetLeaseBindingOfDesiredIP() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 3);

        /* Either Client Mac or Desired IP already registered as permanent lease */
        dhcpPool.configureFixedIPLease(IPv4Address.of("192.168.1.2"), MacAddress.of("22:33:44:55:66:77"));
        DHCPBinding lease1 = dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.1"), MacAddress.of("22:33:44:55:66:77"));
        assertSame(lease1, dhcpPool.getDHCPbindingFromMAC(MacAddress.of("22:33:44:55:66:77")));
        assertSame(lease1, dhcpPool.getDHCPbindingFromIPv4(IPv4Address.of("192.168.1.2")));

        /* None of them registered as a permanent lease, return an available binding with desired IP */
        DHCPBinding lease2 = dhcpPool.findLeaseBinding(MacAddress.of("00:11:22:33:44:55"));
        DHCPBinding lease3 = dhcpPool.findLeaseBindingOfDesiredIP(lease2.getIPv4Address(), MacAddress.of("00:11:22:33:44:55"));
        dhcpPool.setLeaseBinding(lease3, MacAddress.of("00:11:22:33:44:55"), 100L);
        assertSame(lease2, lease3);
        DHCPBinding lease4 = dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.3"), MacAddress.of("55:44:33:22:11:00"));
        assertNotNull(lease4);

        /* All other case should return null */
        DHCPBinding lease5 = dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.5"), MacAddress.of("00:11:22:33:44:55"));
        assertNull(lease5);
        DHCPBinding lease6 = dhcpPool.findLeaseBindingOfDesiredIP(lease3.getIPv4Address(), MacAddress.of("AA:BB:CC:DD:EE:FF"));
        assertNull(lease6);

    }


    @Test(expected = UnsupportedOperationException.class)
    public void testSetLeaseBindingException() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 3);

        DHCPBinding lease1 = dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.2"), MacAddress.of("00:11:22:33:44:55"));
        dhcpPool.setFixedLeaseBinding(lease1, MacAddress.of("00:11:22:33:44:55"));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetFixLeaseBinding() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 3);

        dhcpPool.configureFixedIPLease(IPv4Address.of("192.168.1.2"), MacAddress.of("22:33:44:55:66:77"));
        DHCPBinding lease1 = dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.2"), MacAddress.of("22:33:44:55:66:77"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("22:33:44:55:66:77"), 100L);

    }


    @Test
    public void testCancelLeaseOfMac() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 3);

        /* Client MAC never registered */
        assertFalse(dhcpPool.cancelLeaseOfMAC(MacAddress.of("22:33:44:55:66:77")));

        /* Cient MAC associated w/ permanent lease */
        dhcpPool.configureFixedIPLease(IPv4Address.of("192.168.1.2"), MacAddress.of("22:33:44:55:66:77"));
        DHCPBinding lease = dhcpPool.findLeaseBinding(MacAddress.of("22:33:44:55:66:77"));
        dhcpPool.setFixedLeaseBinding(lease, MacAddress.of("22:33:44:55:66:77"));
        assertFalse(dhcpPool.cancelLeaseOfMAC(MacAddress.of("22:33:44:55:66:77")));
        assertEquals(2, dhcpPool.getPoolAvailability());

        /* Client MAC associated w/ normal lease */
        DHCPBinding lease1= dhcpPool.findLeaseBinding(MacAddress.of("00:11:22:33:44:55"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("00:11:22:33:44:55"), 100L);
        assertEquals(1, dhcpPool.getPoolAvailability());
        assertTrue(dhcpPool.cancelLeaseOfMAC(MacAddress.of("00:11:22:33:44:55")));
        assertEquals(2, dhcpPool.getPoolAvailability());
        assertFalse(dhcpPool.cancelLeaseOfMAC(MacAddress.of("00:11:22:33:44:55")));
        assertEquals(2, dhcpPool.getPoolAvailability());

    }


    @Test
    public void testCancelLeaseOfIP() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 3);

        /* IP is invalid or not actively leased yet */
        assertFalse(dhcpPool.cancelLeaseOfIPv4(IPv4Address.of("192.168.1.10")));
        assertFalse(dhcpPool.cancelLeaseOfIPv4(IPv4Address.of("192.168.1.1")));

        /* IP address is associated w/ permanent lease */
        dhcpPool.configureFixedIPLease(IPv4Address.of("192.168.1.2"), MacAddress.of("22:33:44:55:66:77"));
        DHCPBinding lease = dhcpPool.findLeaseBinding(MacAddress.of("22:33:44:55:66:77"));
        dhcpPool.setFixedLeaseBinding(lease, MacAddress.of("22:33:44:55:66:77"));
        assertFalse(dhcpPool.cancelLeaseOfIPv4(IPv4Address.of("192.168.1.2")));
        assertEquals(2, dhcpPool.getPoolAvailability());

        /* IP address associated with normal lease */
        DHCPBinding lease1= dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.3"), MacAddress.of("00:11:22:33:44:55"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("00:11:22:33:44:55"), 100L);
        assertEquals(1, dhcpPool.getPoolAvailability());
        assertTrue(dhcpPool.cancelLeaseOfIPv4(IPv4Address.of("192.168.1.3")));
        assertEquals(2, dhcpPool.getPoolAvailability());
        assertFalse(dhcpPool.cancelLeaseOfIPv4(IPv4Address.of("192.168.1.3")));
        assertEquals(2, dhcpPool.getPoolAvailability());

    }


    @Test
    public void testClearExpiredLeases() throws Exception {
        dhcpPool = initDHCPPool(IPv4Address.of("192.168.1.1"), 3);
//        dhcpPool.displayDHCPPool();

        /* Permanent lease should not be cleared */
        dhcpPool.configureFixedIPLease(IPv4Address.of("192.168.1.2"), MacAddress.of("22:33:44:55:66:77"));
        DHCPBinding lease = dhcpPool.findLeaseBinding(MacAddress.of("22:33:44:55:66:77"));
        dhcpPool.setFixedLeaseBinding(lease, MacAddress.of("22:33:44:55:66:77"));
        dhcpPool.cleanExpiredLeases();
//        dhcpPool.displayDHCPPool();

        /* Active-still lease should not be cleared */
        DHCPBinding lease1= dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.3"), MacAddress.of("00:11:22:33:44:55"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("00:11:22:33:44:55"), 100L);
        dhcpPool.cleanExpiredLeases();
//        dhcpPool.displayDHCPPool();

        /* Expired leases should be cleared */
        DHCPBinding lease2= dhcpPool.findLeaseBindingOfDesiredIP(IPv4Address.of("192.168.1.1"), MacAddress.of("55:44:33:22:11:00"));
        dhcpPool.setLeaseBinding(lease1, MacAddress.of("55:44:33:22:11:00"), 3L);
//        dhcpPool.displayDHCPPool();
        TimeUnit.SECONDS.sleep(5L);
        dhcpPool.cleanExpiredLeases();
//        dhcpPool.displayDHCPPool();

    }


}
