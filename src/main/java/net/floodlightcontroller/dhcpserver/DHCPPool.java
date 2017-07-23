package net.floodlightcontroller.dhcpserver;

import java.util.ArrayList;
import java.util.Objects;

import net.floodlightcontroller.packet.DHCP;
import net.floodlightcontroller.packet.IPv4;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

/**
 * The class representing a DHCP Pool.
 * This class is essentially a list of DHCPBinding objects containing IP, MAC, and lease status information.
 *
 * @author Ryan Izard (rizard@g.clemson.edu)
 * Modified by Qing Wang (qw@g.clemson.edu) on 6/24/17
 *
 */
public class DHCPPool {

	protected static final Logger log = LoggerFactory.getLogger(DHCPPool.class);
	private volatile ArrayList<DHCPBinding> DHCP_POOL = new ArrayList<DHCPBinding>();
	private volatile int POOL_SIZE;
	private volatile int POOL_AVAILABILITY;
	private volatile boolean POOL_FULL;
	private volatile IPv4Address STARTING_ADDRESS;
	private final MacAddress UNASSIGNED_MAC = MacAddress.NONE;

	// This assumes startingIPv4Address can handle size addresses
	/**
	 * Constructor for a DHCPPool of DHCPBinding's. Each DHCPBinding object is initialized with a
	 * null MAC address and the lease is set to inactive (i.e. false).
	 * @param {@code byte[]} startingIPv4Address: The lowest IP address to lease.
	 * @param {@code integer} size: (startingIPv4Address + size) is the highest IP address to lease.
	 * @return none
	 */
	public DHCPPool(IPv4Address startingIPv4Address, int size) {
		int IPv4AsInt = startingIPv4Address.getInt();
		this.setPoolSize(size);
		this.setPoolAvailability(size);
		STARTING_ADDRESS = startingIPv4Address;
		for (int i = 0; i < size; i++) {
			DHCP_POOL.add(new DHCPBinding(IPv4Address.of(IPv4AsInt + i), UNASSIGNED_MAC));
		}

	}

	private void setPoolFull(boolean full) { POOL_FULL = full; }
	private void setPoolSize(int size) { POOL_SIZE = size; }
	private void setPoolAvailability(int size) { POOL_AVAILABILITY = size; }

	public int getPoolSize() { return POOL_SIZE; }
	public int getPoolAvailability() { return POOL_AVAILABILITY; }
	public IPv4Address getStartIP() { return STARTING_ADDRESS; }
	public boolean isPoolFull() { return POOL_FULL; }

	/**
	 * Determines if there are available leases in this DHCPPool.
	 * @return {@code boolean}: true if there are addresses available, false if the DHCPPool is full
	 */
	public boolean hasAvailableSpace() {
		if (isPoolFull() || getPoolAvailability() == 0) {
			log.info("DHCP pool is full and has no space available, considering to increase the pool space");
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Display each DHCPBinding inside DHCP Pool
	 */
	public void displayDHCPPool() {
		if(getPoolSize() > 0) {
			for (DHCPBinding binding : DHCP_POOL) {
				System.out.println(binding.toString());
			}
		} else {
			log.error("Failed to display DHCP pool as its size is not allocated correctly");
		}
	}

	/**
	 * Gets the DHCPBinding object from the DHCPPool containing {@code byte[]} ip
	 * @param {@code byte[]} ip: The IPv4 address to match in a DHCPBinding
	 * @return {@code DHCPBinding}: The matching DHCPBinding object or null if ip is not found
	 */
	public DHCPBinding getDHCPbindingFromIPv4(IPv4Address ip) {
		if (ip == null) return null;

		for (DHCPBinding binding : DHCP_POOL) {
			if (binding.getIPv4Address().equals(ip)) {
				return binding;
			}
		}
		return null;
	}

	/**
	 * Gets the DHCPBinding object from the DHCPPool containing {@code byte[]} mac
	 * @param {@code byte[]} mac: The MAC address to match in in a DHCPBinding
	 * @return {@code DHCPBinding}: The matching DHCPBinding object or null if mac is not found
	 */
	public DHCPBinding getDHCPbindingFromMAC(MacAddress mac) {
		if (mac == null) return null;

		for (DHCPBinding binding : DHCP_POOL) {
			if (binding.getMACAddress().equals(mac)) {
				return binding;
			}
		}
		return null;
	}

	/**
	 * Gets the lease status of a particular IPv4 address, {@code byte[]} ip
	 * @param {@code byte[]} ip: The IPv4 address of which to check the lease status
	 * @return {@code boolean}: true if lease is active, false if lease is inactive/expired
	 */
	public boolean isIPv4Leased(IPv4Address ip) {
		if (ip == null) return false;

		DHCPBinding binding = this.getDHCPbindingFromIPv4(ip);
		if (binding != null && !binding.isLeaseAvailable()) return true;
		else return false;
	}

	/**
	 * Check whether a IPv4 address is belongs to this DHCP Pool or not
	 * @param ip
	 * @return
	 */
	public boolean isIPv4BelongPool(IPv4Address ip) {
		if (ip == null) return false;

		for (DHCPBinding binding : DHCP_POOL) {
			if (binding.getIPv4Address().equals(ip)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Completely removes the dhcp binding based on ip address
	 * After removal, that ip address will not be available for lease after removal
	 * After removal, the pool size = pool size - 1;
	 *
	 * @param {@code byte[]} ip: The address to be removed from the pool.
	 * @return none
	 */
	public void removeIPv4FromDHCPPool(IPv4Address ip) {
		if (ip == null) {
			throw new IllegalArgumentException("Failed to remove IPv4 from dhcp pool : IPv4 address can not be null");
		}

		if (this.getDHCPbindingFromIPv4(ip) == null) return;

		// Re-locate the lowest address(other than original starting IP) in the pool as new starting address
		if (ip.equals(STARTING_ADDRESS)) {
			DHCP_POOL.remove(this.getDHCPbindingFromIPv4(ip));

			DHCPBinding lowest = null;
			for (DHCPBinding binding : DHCP_POOL) {
				if (lowest == null) {
					lowest = binding;
				}
				else if ( (binding.getIPv4Address().compareTo(lowest.getIPv4Address()) < 0) ) {
					lowest = binding;
				}
			}
			System.out.println("Now the lowest IP is: " + lowest.getIPv4Address().toString());
			// lowest is new starting address
			STARTING_ADDRESS = lowest.getIPv4Address();

		} else {
			DHCP_POOL.remove(this.getDHCPbindingFromIPv4(ip));
		}

		this.setPoolSize(this.getPoolSize() - 1);
		this.setPoolAvailability(this.getPoolAvailability() - 1);

		if (this.getPoolAvailability() == 0) this.setPoolFull(true);

	}

	/**
	 * Adds an IP address to the DHCPPool if the address is not already present. If present, nothing is added to the DHCPPool.
	 * After adds, the pool size = pool size + 1;
	 *
	 * @param {@code byte[]} ip: The IP address to attempt to add to the DHCPPool
	 * @return {@code DHCPBinding}: Reference to the DHCPBinding object if successful, null if unsuccessful
	 */
	public DHCPBinding addIPv4ToDHCPPool(IPv4Address ip) {
		if (ip == null) {
			throw new IllegalArgumentException("Failed to add IPv4 from dhcp pool : IPv4 address can not be null");
		}

		DHCPBinding binding = null;
		if (this.getDHCPbindingFromIPv4(ip) == null) {
			if (ip.getInt() < STARTING_ADDRESS.getInt()) {
				STARTING_ADDRESS = ip;
			}

			binding = new DHCPBinding(ip, UNASSIGNED_MAC);
			DHCP_POOL.add(binding);
			this.setPoolSize(this.getPoolSize() + 1);
			this.setPoolAvailability(this.getPoolAvailability() + 1);
			this.setPoolFull(false);

		}

		return binding;

	}

	/**
	 * Find and returns an lease DHCP binding based on client mac address.
	 * This method only responsible for find a DHCP binding but doesn't handle DHCP binding status
	 *
	 * If this client Mac already registered, the same dhcp binding will return at priority
	 * If this is a new client Mac, then the lowest currently inactive address will be returned.
	 *
	 * @param {@code byte[]): MAC address of the device requesting the lease
	 * @return {@code DHCPBinding}: Reference to the chosen lease bhcp binding  if successful, null if unsuccessful
	 */
	public DHCPBinding findLeaseBinding(MacAddress mac) {
		if (mac == null) {
			throw new IllegalArgumentException("Failed to get dhcp lease : Mac address can not be null");
		}

		if (!this.hasAvailableSpace()) return null;

		DHCPBinding leaseBinding = this.getDHCPbindingFromMAC(mac);

		/* Client Mac registered already */
		if (leaseBinding != null) {
			log.debug("Found MAC {} registered in DHCP pool, returning that DHCP binding for lease.", mac);
			return leaseBinding;
		}

		/* New Client Mac that never registered */
		for (DHCPBinding binding : DHCP_POOL) {
			if (binding.isLeaseAvailable() && binding.getMACAddress().equals(UNASSIGNED_MAC)) {
				leaseBinding = binding;
				break;
			}
		}

		log.debug("Register Mac {} in DHCP pool, returning an available DHCP binding to lease with IP {}", mac, leaseBinding.getIPv4Address());
		return leaseBinding;

	}

	/**
	 * Find and returns a specific lease binding based on request desired IP address and client Mac address
	 * This method only responsible for find a DHCP binding but doesn't handle DHCP binding status
	 *
	 *   (1) If the MAC is found in an available, reserved fixed binding, and that binding is not for the provided IP,
	 *       the fixed binding associated with the MAC will be returned.
	 *
	 *   (2) If the IP is found in an available, fixed binding, and that binding also contains the MAC address provided,
	 *       then the binding will be returned -- this is true only if the IP and MAC result in the same available, fixed binding.
	 *
	 *   (3) If the IP is found in the pool and it is available and not fixed, then its binding will be returned.
	 *
	 *   (4) If the IP provided does not match any available entries or is invalid, null will be returned.
	 *       If this is the case, run findLeaseBinding(mac) to resolve.
	 *
	 *
	 * @param {@code byte[]}: The IP address try to obtain for a lease
	 * @param {@code byte[]}: The Client MAC address
	 * @return {@code DHCPBinding}: Reference to the chosen lease bhcp binding if successful, null if unsuccessful
	 */
	public DHCPBinding findLeaseBindingOfDesiredIP(IPv4Address desiredIp, MacAddress mac) {
		if (desiredIp == null) {
			throw new IllegalArgumentException("Failed to get a specific binding from dhcp pool : IPv4 address can not be null");
		}
		if (mac == null) {
			throw new IllegalArgumentException("Failed to get a specific binding from dhcp pool : Mac address can not be null");
		}

		if (!this.hasAvailableSpace()) return null;

		DHCPBinding binding1 = this.getDHCPbindingFromIPv4(desiredIp);
		DHCPBinding binding2 = this.getDHCPbindingFromMAC(mac);
		// If configured, must return a reserved fixed binding for a MAC address even if it's requesting another IP
		if (binding2 != null && binding2.isLeaseAvailable() && binding2.isPermanentLease() && binding1 != binding2) {
			log.info("Reserved Fixed DHCP entry for MAC trumps requested IP {}. Returning binding for MAC {}", desiredIp, mac);
			return binding2;

			// If configured, we must return a fixed binding for an IP if the binding is fixed to the provided MAC (ideal static request case)
		} else if (binding1 != null && binding1.isLeaseAvailable() && binding1.isPermanentLease() && mac.equals(binding1.getMACAddress())) {
			log.info("Found matching fixed DHCP entry for IP with MAC. Returning binding for IP {} with MAC {}", desiredIp, mac);
			return binding1;

			// The IP and MAC are not a part of a reserved fixed binding, so return the binding of the requested IP.
		} else if (binding1 != null && binding1.isLeaseAvailable() && !binding1.isPermanentLease()) {
			log.info("No fixed DHCP entry for IP or MAC found. Returning dynamic binding for IP {}.", desiredIp);
			return binding1;

			// Otherwise, the binding is fixed for both MAC and IP and this MAC does not match either, so we can't return it as available
		} else {
			log.debug("Invalid IP address request or IP is actively leased...check for any available lease to resolve");
			return null;

		}

	}

	/**
	 * Configure a particular dhcp binding as a reserved fixed/permanent IP lease
	 * When an IP is permenant, DHCP server can only re-allocate it after client explicitly release it
	 *
	 * This configure method does NOT set the lease as active, but instead reserves that IP for only the MAC provided.
	 * To set the lease as active, use findLeaseBindingOfDesiredIP()/findLeaseBinding() to return the binding
	 *
	 * @param {@code byte[]}: The IP address to set as static/fixed.
	 * @param {@code byte[]}: The MAC address to match to the IP address ip when an address is requested from the MAC mac
	 * @return {@code boolean}: True upon success; false upon failure (e.g. no IP found)
	 *
	 */
	public boolean configureFixedIPLease(IPv4Address ip, MacAddress mac) {
		if (ip == null) {
			throw new IllegalArgumentException("Failed to configure a permanent DHCP binding : IPv4 address can not be null");
		}
		if (mac == null) {
			throw new IllegalArgumentException("Failed to configure a permanent DHCP binding : Mac address can not be null");
		}

		DHCPBinding binding = this.getDHCPbindingFromIPv4(ip);
		if (binding != null) {
			binding.setMACAddress(mac);
			binding.setLeasePermanent(true);
			binding.setLeaseStatus(false);

			log.info("A permanent DHCP binding is successfully configured with IP address {} and Mac address {}", ip, mac);
			return true;

		} else {
			log.info("Failed to configure permanent DHCP binding with IP address {}, check if it is is a valid static address", ip, ip);
			return false;

		}
	}

	/**
	 * Initial attributes for the regular lease-available dhcp binding
	 * This method also sets the lease to active (i.e. true) when the assignment is made.
	 * This method should not be called if trying to renew a lease, should call renewLease()
	 *
	 * @param {@code DHCPBinding} binding : lease-available dhcp binding
	 * @param {@code byte[]} mac : mac address of a client that request a dhcp lease
	 * @param {@code long} time :  valid leasing time
	 * @return none
	 */
	public void setLeaseBinding(DHCPBinding binding, MacAddress mac, long time) {
		if (mac == null) {
			throw new IllegalArgumentException("Failed to set lease binding : Mac address can not be null");
		}

		if (binding.isPermanentLease()) {
			throw new UnsupportedOperationException("Should not use setLeaseBinding() for a permanent lease, use setFixedLeaseBinding()");
		}

		binding.setMACAddress(mac);
		binding.setLeaseStartTimeSeconds();
		binding.setLeaseDurationSeconds(time);
		binding.setLeaseStatus(true);

		this.setPoolAvailability(this.getPoolAvailability() - 1);
		if (this.getPoolAvailability() == 0) {
			setPoolFull(true);
			log.info("DHCP pool is full right now");
		}

	}

	/**
	 * Initial attributes for the fixed/permanent lease-available dhcp binding
	 * This method also sets the lease to active (i.e. true) when the assignment is made.
	 * This method should not be called if trying to renew a lease, should call renewLease()
	 *
	 * @param {@code DHCPBinding} binding : lease-available dhcp binding
	 * @param {@code byte[]} mac : mac address of a client that request a dhcp lease
	 * @param {@code long} time :  valid leasing time
	 * @return none
	 */
	public void setFixedLeaseBinding(DHCPBinding binding, MacAddress mac) {
		if (mac == null) {
			throw new IllegalArgumentException("Failed to set lease binding : Mac address can not be null");
		}

		if (!binding.isPermanentLease()) {
			throw new UnsupportedOperationException("Should not use setFixedLeaseBinding() for a regular lease, use setLeaseBinding()");
		}

		binding.setMACAddress(mac);
		binding.setLeaseStatus(true);

		this.setPoolAvailability(this.getPoolAvailability() - 1);
		if (this.getPoolAvailability() == 0) {
			setPoolFull(true);
			log.info("DHCP pool is full right now");
		}

	}

	/**
	 * Tries to renew an IP lease.
	 *
	 * @param {@code byte[]}: The IP address on which to try and renew a lease
	 * @param {@code long}: The time in seconds for which the lease will be valid
	 * @return {@code DHCPBinding}: True on success, false if unknown IP address
	 */
	public boolean renewLease(IPv4Address ip, int time) {
		if (ip == null) {
			throw new IllegalArgumentException("Failed to renew lease : IPv4 address can not be null");
		}

		DHCPBinding binding = this.getDHCPbindingFromIPv4(ip);
		if (binding != null) {
			if (binding.isPermanentLease()) {
				log.debug("Failed to renew lease : IP address {} is configured as a permanent dhcp lease", ip);
				return false;
			}

			binding.setLeaseStartTimeSeconds();
			binding.setLeaseDurationSeconds(time);
			binding.setLeaseStatus(true);

			log.info("IP address {} has successfully renewed", ip);
			return true;
		}
		return false;
	}

	/**
	 * Cancel an IP lease based on IP address
	 *
	 * @param {@code byte[]}: The IP address on which to try and cancel a lease
	 * @return {@code boolean}: True on success, false if unknown IP address
	 */
	public boolean cancelLeaseOfIPv4(IPv4Address ip) {
		if (ip == null) {
			throw new IllegalArgumentException("Failed to cancel lease : IPv4 address can not be null");
		}

		DHCPBinding binding = this.getDHCPbindingFromIPv4(ip);
		if (binding != null && !binding.isLeaseAvailable()) {
			if (binding.isPermanentLease()){
				log.debug("Failed to cancel lease : IP address {} is configured as a permanent dhcp lease", ip);
				return false;
			}

			binding.cancelLease();
			this.setPoolAvailability(this.getPoolAvailability() + 1);
			this.setPoolFull(false);

			log.info("DHCP lease of Mac address {} with IP {} is successfully canceled", binding.getMACAddress(), ip);
			return true;
		}
		return false;
	}

	/**
	 * Cancel an IP lease based on Mac Address
	 *
	 * @param {@code byte[]}: The MAC address on which to try and cancel a lease
	 * @return {@code boolean}: True on success, false if unknown IP address
	 */
	public boolean cancelLeaseOfMAC(MacAddress mac) {
		if (mac == null) {
			throw new IllegalArgumentException("Failed to cancel a lease : Mac address can not be null");
		}

		DHCPBinding binding = getDHCPbindingFromMAC(mac);
		if (binding != null && !binding.isLeaseAvailable()) {
			if (binding.isPermanentLease()) {
				log.debug("Failed to cancel lease : Mac address {} is configured as a permanent dhcp lease", mac);
				return false;
			}

			binding.cancelLease();
			this.setPoolAvailability(this.getPoolAvailability() + 1);
			this.setPoolFull(false);

			log.info("DHCP lease of Mac address {} with IP {} is successfully canceled", mac, binding.getIPv4Address());
			return true;
		}

		return false;
	}

	/**
	 * Cancel an permanant DHCP lease
	 *
	 * @param ip {@code byte[]}: The IP address on which to try and cancel a permanent lease
	 * @return {@code boolean}: True on success, false on not success
     */
	public boolean cancelPermanentLeaseOfIP(IPv4Address ip) {
		if (ip == null) {
			throw new IllegalArgumentException("Failed to cancel lease : IPv4 address can not be null");
		}

		DHCPBinding binding = this.getDHCPbindingFromIPv4(ip);
		if (binding != null && binding.isPermanentLease()) {
			binding.setLeaseStatus(false);
			binding.setLeasePermanent(false);
			binding.setMACAddress(MacAddress.NONE);

			this.setPoolAvailability(this.getPoolAvailability() + 1);
			this.setPoolFull(false);

			log.info("Permanent DHCP lease of Mac address {} with IP {} is successfully canceled", binding.getMACAddress(), ip);
			return true;
		}

		return false;
	}


	/**
	 * Make expired leases available and reset the lease times.
	 *
	 * @return {@code ArrayList<DHCPBinding>}: A list of the bindings that are now available
	 */
	public ArrayList<DHCPBinding> cleanExpiredLeases() {
		for (DHCPBinding binding : DHCP_POOL) {
			if (!binding.isPermanentLease() && binding.isLeaseExpired() && !binding.isLeaseAvailable()) {
				System.out.println("run here " + binding.getIPv4Address());
				this.cancelLeaseOfIPv4(binding.getIPv4Address());
			}

		}

		return DHCP_POOL;
	}


}

