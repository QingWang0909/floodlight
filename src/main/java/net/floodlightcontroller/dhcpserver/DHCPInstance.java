package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.VlanVid;

import javax.crypto.Mac;
import java.util.*;

/**
 * The class representing a DHCP instance. One DHCP instance is responsible for managing one subnet.
 * DHCP instance contains a DHCP pool,
 *
 * @author Qing Wang (qw@g.clemson.edu) on 6/24/17.
 *
 */
public class DHCPInstance {

    private String name = null;
    private volatile DHCPPool dhcpPool = null;

    private IPv4Address serverIP = IPv4Address.NONE;
    private MacAddress serverMac = MacAddress.NONE;
    private IPv4Address broadcastIP = IPv4Address.NONE;
    private IPv4Address routerIP = IPv4Address.NONE;
    private IPv4Address subnetMask = IPv4Address.NONE;
    private IPv4Address startIPAddress = IPv4Address.NONE;
    private IPv4Address endIPAddress = IPv4Address.NONE;
    private int leaseTimeSec = 0;
    private int rebindTimeSec = 0;
    private int renewalTimeSec = 0;

    private List<IPv4Address> dnsServers = null;
    private List<IPv4Address> ntpServers = null;
    private boolean ipforwarding = false;
    private String domainName = null;

    private Map<MacAddress, IPv4Address> staticAddresseses;
    private Set<MacAddress> clientMembers = null;
    private Set<VlanVid> vlanMembers = null;
    private Set<NodePortTuple> nptMembers = null;

    public String getName() { return name; }
    public DHCPPool getDHCPPool() { return dhcpPool; }
    public IPv4Address getServerIP() { return serverIP; }
    public MacAddress getServerMac() { return serverMac; }
    public IPv4Address getBroadcastIP() { return broadcastIP; }
    public IPv4Address getRouterIP() { return routerIP; }
    public IPv4Address getSubnetMask() { return subnetMask; }
    public IPv4Address getStartIPAddress() { return startIPAddress; }
    public IPv4Address getEndIPAddress() { return endIPAddress; }
    public int getLeaseTimeSec() { return leaseTimeSec; }
    public int getRebindTimeSec() { return rebindTimeSec; }
    public int getRenewalTimeSec() { return renewalTimeSec; }

    public List<IPv4Address> getDNSServers() { return dnsServers; }
    public List<IPv4Address> getNtpServers() { return ntpServers; }
    public boolean getIpforwarding() { return ipforwarding; }
    public String getDomainName() { return domainName; }

    public Map<MacAddress, IPv4Address> getStaticAddresseses() { return staticAddresseses; };
    public Set<NodePortTuple> getNptMembers() { return nptMembers; }
    public Set<VlanVid> getVlanMembers() { return vlanMembers; }
    public Set<MacAddress> getClientMembers() { return clientMembers; }

    public boolean isIPv4BelongsInstance(IPv4Address ip) {
        return this.getDHCPPool().isIPv4BelongPool(ip);
    }

    private DHCPInstance(DHCPInstanceBuilder builder) {
        this.name = builder.name;
        this.dhcpPool = builder.dhcpPool;
        this.serverIP = builder.serverIP;
        this.serverMac = builder.serverMac;
        this.broadcastIP = builder.broadcastIP;
        this.routerIP = builder.routerIP;
        this.subnetMask = builder.subnetMask;
        this.startIPAddress = builder.startIPAddress;
        this.endIPAddress = builder.endIPAddress;
        this.leaseTimeSec = builder.leaseTimeSec;
        this.rebindTimeSec = builder.rebindTimeSec;
        this.renewalTimeSec = builder.renewalTimeSec;

        this.dnsServers = builder.dnsServers;
        this.ntpServers = builder.ntpServers;
        this.ipforwarding = builder.ipforwarding;
        this.domainName = builder.domainName;

        this.staticAddresseses = builder.staticAddresseses;
        this.vlanMembers = builder.vlanMembers;
        this.nptMembers = builder.nptMembers;
        this.clientMembers = builder.clientMembers;

    }

    public static DHCPInstanceBuilder createBuilder(){
        return new DHCPInstanceBuilder();
    }

    public static class DHCPInstanceBuilder {
        private String name;
        private DHCPPool dhcpPool;
        private IPv4Address serverIP;
        private MacAddress serverMac;
        private IPv4Address broadcastIP;
        private IPv4Address routerIP;
        private IPv4Address subnetMask;
        private IPv4Address startIPAddress;
        private IPv4Address endIPAddress;
        private int leaseTimeSec;
        private int rebindTimeSec;
        private int renewalTimeSec;

        private List<IPv4Address> dnsServers;
        private List<IPv4Address> ntpServers;
        private boolean ipforwarding;
        private String domainName;

        private Map<MacAddress, IPv4Address> staticAddresseses;
        private Set<MacAddress> clientMembers;
        private Set<VlanVid> vlanMembers;
        private Set<NodePortTuple> nptMembers;

        public DHCPInstanceBuilder setName(String name) {
            if(name == null || name.isEmpty()){
                throw new IllegalArgumentException("Build DHCP instance failed : DHCP server name can not be null or empty");
            }

            this.name = name;
            return this;
        }

        public DHCPInstanceBuilder setServerIP(IPv4Address serverIP) {
            if(serverIP == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server IP address can not be null");
            }
            if(serverIP == IPv4Address.NONE){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server IP address can not be empty");
            }

            this.serverIP = serverIP;
            return this;
        }

        public DHCPInstanceBuilder setServerMac(MacAddress serverMac) {
            if(serverMac == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server Mac address can not be null");
            }
            if(serverMac == MacAddress.NONE){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server Mac address can not be empty");
            }

            this.serverMac = serverMac;
            return this;
        }

        public DHCPInstanceBuilder setBroadcastIP(IPv4Address broadcastIP) {
            if(broadcastIP == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : Broadcast IP address can not be null");
            }
            if(broadcastIP == IPv4Address.NONE){
                throw  new IllegalArgumentException("Build DHCP instance failed : Broadcast IP address can not be empty");
            }


            this.broadcastIP = broadcastIP;
            return this;
        }

        public DHCPInstanceBuilder setRouterIP(IPv4Address routerIP) {
            if(routerIP == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : Router/Gateway IP address can not be null");
            }

            this.routerIP = routerIP;
            return this;
        }

        public DHCPInstanceBuilder setSubnetMask(IPv4Address subnetMask) {
            if(subnetMask == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : Subnet Mask can not be null");
            }

            this.subnetMask = subnetMask;
            return this;
        }

        public DHCPInstanceBuilder setStartIP(IPv4Address start) {
            if(start == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP Pool Starter IP address can not be null");
            }
            if(start == IPv4Address.NONE){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP Pool Starter IP address can not be empty");
            }

            this.startIPAddress = start;
            return this;
        }

        public DHCPInstanceBuilder setEndIP(IPv4Address end) {
            if(end == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP Pool Stopper IP address can not be null");
            }
            if(end == IPv4Address.NONE){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP Pool Stopper IP address can not be empty");
            }

            this.endIPAddress = end;
            return this;
        }

        public DHCPInstanceBuilder setLeaseTimeSec(int timeSec) {
            if(timeSec < 0){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server lease time can not be less than 0");
            }

            this.leaseTimeSec = timeSec;
            return this;
        }
/*
        public DHCPInstanceBuilder setRebindTimeSec(int timeSec){
            if(timeSec < 0){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server rebind time can not be less than 0");
            }

            this.rebindTimeSec = timeSec;
            return this;
        }

        public DHCPInstanceBuilder setRenewalTimeSec(int timeSec){
            if(timeSec < 0){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server renewal time can not be less than 0");
            }

            this.renewalTimeSec = timeSec;
            return this;
        }
*/

        public DHCPInstanceBuilder setDNSServers(List<IPv4Address> dnsServers) {
            if(dnsServers == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server DNS Servers can not be null");
            }

            this.dnsServers = dnsServers;
            return this;
        }

        public DHCPInstanceBuilder setNTPServers(List<IPv4Address> ntpServers) {
            if(ntpServers == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP server NTP Servers can not be null");
            }

            this.ntpServers = ntpServers;
            return this;
        }

        public DHCPInstanceBuilder setIPforwarding(boolean ipforwarding) {
            this.ipforwarding = ipforwarding;
            return this;
        }

        public DHCPInstanceBuilder setDomainName(String name) {
            if(name == null || name.isEmpty()){
                throw  new IllegalArgumentException("Build DHCP instance failed : DHCP Server Domain Name can not be null or empty");
            }

            this.domainName = name;
            return this;
        }

        public DHCPInstanceBuilder setStaticAddresses(MacAddress mac, IPv4Address ip) {
            if(mac == null || ip == null){
                throw new IllegalArgumentException("BUild DHCP instance faild : DHCP static address can not be null");
            }
            if(mac == MacAddress.NONE || ip == IPv4Address.NONE){
                throw new IllegalArgumentException("BUild DHCP instance faild : DHCP static address can not be empty");
            }

            this.staticAddresseses.put(mac, ip);
            return this;
        }

        public DHCPInstanceBuilder setVlanMembers(Set<VlanVid> vlanMembers) {
            if(vlanMembers == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : VLAN member vid can not be null");
            }

            this.vlanMembers = vlanMembers;
            return this;
        }

        public DHCPInstanceBuilder setNptMembers(Set<NodePortTuple> nptMembers) {
            if(nptMembers == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : Ports member can not be null");
            }

            this.nptMembers = nptMembers;
            return this;
        }

        public DHCPInstanceBuilder setClientMembers(Set<MacAddress> clientMembers) {
            if(clientMembers == null){
                throw  new IllegalArgumentException("Build DHCP instance failed : Client Member can not be null");
            }

            this.clientMembers = clientMembers;
            return this;
        }

        public DHCPInstance build() {
            if (startIPAddress.compareTo(endIPAddress) >= 0) {
                throw new IllegalArgumentException("Build DHCP instance failed : Starter IP must be less than Stopper IP in order to create a DHCP pool");
            }

            this.rebindTimeSec = (int)(leaseTimeSec * 0.875);
            this.renewalTimeSec = (int)(leaseTimeSec * 0.5);
            this.dhcpPool = new DHCPPool(startIPAddress, endIPAddress.getInt()-startIPAddress.getInt()+1);

            if (this.dnsServers == null) {
                this.dnsServers = new ArrayList<IPv4Address>();
            }
            if (this.ntpServers == null) {
                this.ntpServers = new ArrayList<IPv4Address>();
            }
            if (this.clientMembers == null) {
                this.clientMembers = new HashSet<MacAddress>();
            }
            if (this.vlanMembers == null) {
                this.vlanMembers = new HashSet<VlanVid>();
            }
            if (this.nptMembers == null) {
                this.nptMembers = new HashSet<NodePortTuple>();
            }

            if (this.staticAddresseses != null) {
                for(Map.Entry<MacAddress, IPv4Address> entry : this.staticAddresseses.entrySet()) {
                    this.dhcpPool.configureFixedIPLease(entry.getValue(), entry.getKey());
                }
            }

            return new DHCPInstance(this);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DHCPInstance that = (DHCPInstance) o;

        if (leaseTimeSec != that.leaseTimeSec) return false;
        if (rebindTimeSec != that.rebindTimeSec) return false;
        if (renewalTimeSec != that.renewalTimeSec) return false;
        if (ipforwarding != that.ipforwarding) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (dhcpPool != null ? !dhcpPool.equals(that.dhcpPool) : that.dhcpPool != null) return false;
        if (serverIP != null ? !serverIP.equals(that.serverIP) : that.serverIP != null) return false;
        if (serverMac != null ? !serverMac.equals(that.serverMac) : that.serverMac != null) return false;
        if (broadcastIP != null ? !broadcastIP.equals(that.broadcastIP) : that.broadcastIP != null) return false;
        if (routerIP != null ? !routerIP.equals(that.routerIP) : that.routerIP != null) return false;
        if (subnetMask != null ? !subnetMask.equals(that.subnetMask) : that.subnetMask != null) return false;
        if (dnsServers != null ? !dnsServers.equals(that.dnsServers) : that.dnsServers != null) return false;
        if (ntpServers != null ? !ntpServers.equals(that.ntpServers) : that.ntpServers != null) return false;
        if (domainName != null ? !domainName.equals(that.domainName) : that.domainName != null) return false;
        if (clientMembers != null ? !clientMembers.equals(that.clientMembers) : that.clientMembers != null)
            return false;
        if (vlanMembers != null ? !vlanMembers.equals(that.vlanMembers) : that.vlanMembers != null) return false;
        return nptMembers != null ? nptMembers.equals(that.nptMembers) : that.nptMembers == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (dhcpPool != null ? dhcpPool.hashCode() : 0);
        result = 31 * result + (serverIP != null ? serverIP.hashCode() : 0);
        result = 31 * result + (serverMac != null ? serverMac.hashCode() : 0);
        result = 31 * result + (broadcastIP != null ? broadcastIP.hashCode() : 0);
        result = 31 * result + (routerIP != null ? routerIP.hashCode() : 0);
        result = 31 * result + (subnetMask != null ? subnetMask.hashCode() : 0);
        result = 31 * result + leaseTimeSec;
        result = 31 * result + rebindTimeSec;
        result = 31 * result + renewalTimeSec;
        result = 31 * result + (dnsServers != null ? dnsServers.hashCode() : 0);
        result = 31 * result + (ntpServers != null ? ntpServers.hashCode() : 0);
        result = 31 * result + (ipforwarding ? 1 : 0);
        result = 31 * result + (domainName != null ? domainName.hashCode() : 0);
        result = 31 * result + (clientMembers != null ? clientMembers.hashCode() : 0);
        result = 31 * result + (vlanMembers != null ? vlanMembers.hashCode() : 0);
        result = 31 * result + (nptMembers != null ? nptMembers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DHCPInstance{" +
                "name='" + name + '\'' +
                ", dhcpPool=" + dhcpPool +
                ", serverIP=" + serverIP +
                ", serverMac=" + serverMac +
                ", broadcastIP=" + broadcastIP +
                ", routerIP=" + routerIP +
                ", subnetMask=" + subnetMask +
                ", leaseTimeSec=" + leaseTimeSec +
                ", rebindTimeSec=" + rebindTimeSec +
                ", renewalTimeSec=" + renewalTimeSec +
                ", dnsServers=" + dnsServers +
                ", ntpServers=" + ntpServers +
                ", ipforwarding=" + ipforwarding +
                ", domainName='" + domainName + '\'' +
                '}';
    }

}
