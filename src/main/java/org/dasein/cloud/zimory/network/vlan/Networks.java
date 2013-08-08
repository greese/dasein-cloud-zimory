/**
 * Copyright (C) 2012 enStratus Networks Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.zimory.network.vlan;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.AbstractVLANSupport;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.NetworkInterface;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Networkable;
import org.dasein.cloud.network.RoutingTable;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.zimory.NoContextException;
import org.dasein.cloud.zimory.Zimory;
import org.dasein.cloud.zimory.ZimoryMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Implements VLAN support in Zimory-based clouds.
 * <p>Created by George Reese: 11/20/12 9:14 AM</p>
 * @author George Reese
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class Networks extends AbstractVLANSupport {
    static private final Logger logger = Zimory.getLogger(Networks.class);

    private Zimory provider;

    public Networks(@Nonnull Zimory provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public void addRouteToAddress(@Nonnull String toRoutingTableId, @Nonnull IPVersion version, @Nullable String destinationCidr, @Nonnull String address) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public void addRouteToGateway(@Nonnull String toRoutingTableId, @Nonnull IPVersion version, @Nullable String destinationCidr, @Nonnull String gatewayId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public void addRouteToNetworkInterface(@Nonnull String toRoutingTableId, @Nonnull IPVersion version, @Nullable String destinationCidr, @Nonnull String nicId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public void addRouteToVirtualMachine(@Nonnull String toRoutingTableId, @Nonnull IPVersion version, @Nullable String destinationCidr, @Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public boolean allowsNewNetworkInterfaceCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsNewVlanCreation() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean allowsNewSubnetCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public void assignRoutingTableToSubnet(@Nonnull String subnetId, @Nonnull String routingTableId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public void assignRoutingTableToVlan(@Nonnull String vlanId, @Nonnull String routingTableId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public void attachNetworkInterface(@Nonnull String nicId, @Nonnull String vmId, int index) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Network interfaces are not supported");
    }

    @Override
    public String createInternetGateway(@Nonnull String forVlanId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Internet gateways are not supported");
    }

    @Override
    public @Nonnull String createRoutingTable(@Nonnull String forVlanId, @Nonnull String name, @Nonnull String description) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public @Nonnull NetworkInterface createNetworkInterface(@Nonnull NICCreateOptions options) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Network interfaces are not supported");
    }

    @Override
    public @Nonnull Subnet createSubnet(@Nonnull SubnetCreateOptions options) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Subnets are not supported");
    }

    @Override
    public @Nonnull VLAN createVlan(@Nonnull String cidr, @Nonnull String name, @Nonnull String description, @Nonnull String domainName, @Nonnull String[] dnsServers, @Nonnull String[] ntpServers) throws CloudException, InternalException {
        APITrace.begin(provider, "createVLAN");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Zimory.AccountOwner owner = provider.getAccountOwner();
            StringBuilder xml = new StringBuilder();

            xml.append("<networkCreationInfo>");
            xml.append("<networkName>").append(Zimory.escapeXml(name)).append("</networkName>");
            xml.append("<account><accountId>").append(ctx.getAccountNumber()).append("</accountId></account>");
            xml.append("<networkOwner><id>").append(owner.userId).append("</id><login>").append(owner.login).append("</login></networkOwner>");
            xml.append("</networkCreationInfo>");

            ZimoryMethod method = new ZimoryMethod(provider);

            String id = method.create("networks", xml.toString());

            if( id == null ) {
                logger.error("Unable to POST to networks endpoint");
                throw new CloudException("Unable to POST to networks endpoint");
            }
            VLAN vlan = getVlan(id);

            if( vlan != null ) {
                return vlan;
            }
            logger.error("The POST to create a new network in Zimory succeeded, but nothing was returned");
            throw new CloudException("The POST to create a new network in Zimory succeeded, but nothing was returned");
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void detachNetworkInterface(@Nonnull String nicId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Network interfaces are not supported");
    }

    @Override
    public int getMaxNetworkInterfaceCount() throws CloudException, InternalException {
        return 0;
    }

    @Override
    public int getMaxVlanCount() throws CloudException, InternalException {
        return -2;
    }

    @Override
    public @Nonnull String getProviderTermForNetworkInterface(@Nonnull Locale locale) {
        return "network interface";
    }

    @Override
    public @Nonnull String getProviderTermForSubnet(@Nonnull Locale locale) {
        return "subnet";
    }

    @Override
    public @Nonnull String getProviderTermForVlan(@Nonnull Locale locale) {
        return "network";
    }

    @Override
    public NetworkInterface getNetworkInterface(@Nonnull String nicId) throws CloudException, InternalException {
        return null;
    }

    @Override
    public RoutingTable getRoutingTableForSubnet(@Nonnull String subnetId) throws CloudException, InternalException {
        return null;
    }

    @Nonnull
    @Override
    public Requirement getRoutingTableSupport() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public RoutingTable getRoutingTableForVlan(@Nonnull String vlanId) throws CloudException, InternalException {
        return null;
    }

    @Override
    public Subnet getSubnet(@Nonnull String subnetId) throws CloudException, InternalException {
        return null;
    }

    @Override
    public @Nonnull Requirement getSubnetSupport() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public VLAN getVlan(@Nonnull String vlanId) throws CloudException, InternalException {
        for( VLAN vlan : listVlans() ) {
            if( vlanId.equals(vlan.getProviderVlanId()) ) {
                return vlan;
            }
        }
        return null;
    }

    @Override
    public boolean isNetworkInterfaceSupportEnabled() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        APITrace.begin(provider, "isSubscribedNetworks");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            return (method.getObject("accounts") != null);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isSubnetDataCenterConstrained() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isVlanDataCenterConstrained() throws CloudException, InternalException {
        return false;
    }

    @Override
    public @Nonnull Collection<String> listFirewallIdsForNIC(@Nonnull String nicId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listNetworkInterfaceStatus() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<NetworkInterface> listNetworkInterfaces() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<NetworkInterface> listNetworkInterfacesForVM(@Nonnull String forVmId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<NetworkInterface> listNetworkInterfacesInSubnet(@Nonnull String subnetId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<NetworkInterface> listNetworkInterfacesInVLAN(@Nonnull String vlanId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<Networkable> listResources(@Nonnull String inVlanId) throws CloudException, InternalException {
        ArrayList<Networkable> resources = new ArrayList<Networkable>();
        NetworkServices network = provider.getNetworkServices();

        FirewallSupport fwSupport = network.getFirewallSupport();

        if( fwSupport != null ) {
            for( Firewall fw : fwSupport.list() ) {
                if( inVlanId.equals(fw.getProviderVlanId()) ) {
                    resources.add(fw);
                }
            }
        }

        IpAddressSupport ipSupport = network.getIpAddressSupport();

        if( ipSupport != null ) {
            for( IPVersion version : ipSupport.listSupportedIPVersions() ) {
                for( org.dasein.cloud.network.IpAddress addr : ipSupport.listIpPool(version, false) ) {
                    if( inVlanId.equals(addr.getProviderVlanId()) ) {
                        resources.add(addr);
                    }
                }

            }
        }
        for( RoutingTable table : listRoutingTables(inVlanId) ) {
            resources.add(table);
        }
        Iterable<VirtualMachine> vms = provider.getComputeServices().getVirtualMachineSupport().listVirtualMachines();

        for( VirtualMachine vm : vms ) {
            if( inVlanId.equals(vm.getProviderVlanId()) ) {
                resources.add(vm);
            }
        }
        return resources;
    }


    @Override
    public @Nonnull Iterable<RoutingTable> listRoutingTables(@Nonnull String inVlanId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<Subnet> listSubnets(@Nonnull String inVlanId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<IPVersion> listSupportedIPVersions() throws CloudException, InternalException {
        return Collections.singletonList(IPVersion.IPV4);
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listVlanStatus() throws CloudException, InternalException {
        APITrace.begin(provider, "listVlanStatus");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("networks");

            if( response == null ) {
                logger.error("Unable to identify endpoint for networks in Zimory");
                throw new CloudException("Unable to identify endpoint for VLANs (networks)");
            }
            ArrayList<ResourceStatus> vlans = new ArrayList<ResourceStatus>();
            NodeList list = response.getElementsByTagName("network");

            for( int i=0; i<list.getLength(); i++ ) {
                ResourceStatus v = toVLANStatus(list.item(i));

                if( v != null ) {
                    vlans.add(v);
                }
            }
            return vlans;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<VLAN> listVlans() throws CloudException, InternalException {
        APITrace.begin(provider, "listVlans");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("networks");

            if( response == null ) {
                logger.error("Unable to identify endpoint for networks in Zimory");
                throw new CloudException("Unable to identify endpoint for VLANs (networks)");
            }
            ArrayList<VLAN> vlans = new ArrayList<VLAN>();
            NodeList list = response.getElementsByTagName("network");

            for( int i=0; i<list.getLength(); i++ ) {
                VLAN v = toVLAN(list.item(i));

                if( v != null ) {
                    vlans.add(v);
                }
            }
            return vlans;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void removeInternetGateway(@Nonnull String forVlanId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Internet gateways are not supported");
    }

    @Override
    public void removeNetworkInterface(@Nonnull String nicId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Network interfaces are not supported");
    }

    @Override
    public void removeRoute(@Nonnull String inRoutingTableId, @Nonnull String destinationCidr) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public void removeRoutingTable(@Nonnull String routingTableId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables are not supported");
    }

    @Override
    public void removeSubnet(String providerSubnetId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Subnets are not supported");
    }

    @Override
    public void removeVlan(String vlanId) throws CloudException, InternalException {
        APITrace.begin(provider, "removeVLAN");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            method.delete("networks/" + vlanId);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean supportsInternetGatewayCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsRawAddressRouting() throws CloudException, InternalException {
        return false;
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    private @Nullable ResourceStatus toVLANStatus(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }
        String id = null;
        NodeList attributes = node.getChildNodes();

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeName().equalsIgnoreCase("networkId") && attribute.hasChildNodes() ) {
                id = attribute.getFirstChild().getNodeValue().trim();
            }
        }
        if( id == null ) {
            return null;
        }
        return new ResourceStatus(id, VLANState.AVAILABLE);
    }

    private @Nullable VLAN toVLAN(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        NodeList attributes = node.getChildNodes();
        VLAN vlan = new VLAN();

        vlan.setCidr("0.0.0.0/0");
        vlan.setProviderRegionId(ctx.getRegionId());
        vlan.setSupportedTraffic(new IPVersion[] { IPVersion.IPV4 });
        vlan.setCurrentState(VLANState.AVAILABLE);
        vlan.setProviderOwnerId(ctx.getAccountNumber());
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeName().equalsIgnoreCase("networkId") && attribute.hasChildNodes() ) {
                vlan.setProviderVlanId(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkName") && attribute.hasChildNodes() ) {
                vlan.setName(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("account") && attribute.hasChildNodes() ) {
                NodeList ownerNodes = attribute.getChildNodes();

                for( int j=0; j<ownerNodes.getLength(); j++ ) {
                    Node oa = ownerNodes.item(j);

                    if( oa.getNodeName().equalsIgnoreCase("accountId") && oa.hasChildNodes() ) {
                        vlan.setProviderOwnerId(oa.getFirstChild().getNodeValue().trim());
                    }
                }
            }
        }
        if( vlan.getProviderVlanId() == null ) {
            return null;
        }
        if( vlan.getName() != null ) {
            vlan.setName(vlan.getProviderVlanId());
        }
        if( vlan.getDescription() == null ) {
            vlan.setDescription(vlan.getName());
        }
        return vlan;
    }
}
