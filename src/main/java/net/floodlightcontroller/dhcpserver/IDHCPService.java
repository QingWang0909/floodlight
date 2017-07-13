package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.topology.NodePortTuple;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.VlanVid;

import java.util.Collection;

/**
 * @author Qing Wang (qw@g.clemson.edu) on 6/17/17.
 */

public interface IDHCPService extends IFloodlightService {

    public void enableDHCP();
    public void disableDHCP();
    public boolean isDHCPEnabled();

    public DHCPInstance getInstance(String name);
    public DHCPInstance getInstance(IPv4Address ip);
    public DHCPInstance getInstance(NodePortTuple npt);
    public DHCPInstance getInstance(VlanVid vid);
    public Collection<DHCPInstance> getInstances();

    public boolean addInstance(DHCPInstance instance);
    public boolean deleteInstance(String name);

}
