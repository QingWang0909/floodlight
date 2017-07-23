package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.test.FloodlightTestCase;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

/**
 * @author Qing Wang (qw@g.clemson.edu) on 7/12/17.
 */
public class DHCPBindingTest extends FloodlightTestCase {

    private IPv4Address ip;
    private MacAddress mac;
    private DHCPBinding binding;

    private boolean expectedLeaseExpired;
    private boolean expectedLeaseAvailable;

    @Before
    @Override
    public void setUp() throws Exception {
        ip = IPv4Address.of("192.168.1.1");
        mac = MacAddress.of("00:A0:CC:23:AF:AA");
        binding = new DHCPBinding(ip, mac);

    }


    /**
     * Test if we can track the state that dhcp lease not expire
     */
    @Test
    public void testLeaseNotExpired() throws Exception {
        binding.setLeaseStartTimeSeconds();
        binding.setLeaseDurationSeconds(1000);

        expectedLeaseExpired = false;
        assertEquals(expectedLeaseExpired, binding.isLeaseExpired());

    }


    /**
     * Test if we can track the state that dhcp lease does expire
     */
    @Test
    public void testLeaseDoesExpired() throws Exception {
        binding.setLeaseStartTimeSeconds();
        // set lease duration state
        binding.setLeaseDurationSeconds(1L);
        // set lease duration state back to 0 to mock the case that lease expire
        binding.setLeaseDurationSeconds(0L);

        expectedLeaseExpired = true;
        assertEquals(expectedLeaseExpired, binding.isLeaseExpired());

    }


    /**
     * Test if we can track the state when trying to cancel a dhcp lease
     */
    @Test
    public void testCancelLease() throws Exception {
        binding.setLeaseStartTimeSeconds();
        binding.setLeaseDurationSeconds(100);
        binding.cancelLease();

        expectedLeaseExpired = true;
        expectedLeaseAvailable = true;
        assertEquals(expectedLeaseExpired, binding.isLeaseExpired());
        assertEquals(expectedLeaseAvailable, binding.isLeaseAvailable());

    }


}
