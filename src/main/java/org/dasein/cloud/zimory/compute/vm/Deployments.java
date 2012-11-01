/**
 * ========= CONFIDENTIAL =========
 *
 * Copyright (C) 2012 enStratus Networks Inc - ALL RIGHTS RESERVED
 *
 * ====================================================================
 *  NOTICE: All information contained herein is, and remains the
 *  property of enStratus Networks Inc. The intellectual and technical
 *  concepts contained herein are proprietary to enStratus Networks Inc
 *  and may be covered by U.S. and Foreign Patents, patents in process,
 *  and are protected by trade secret or copyright law. Dissemination
 *  of this information or reproduction of this material is strictly
 *  forbidden unless prior written permission is obtained from
 *  enStratus Networks Inc.
 * ====================================================================
 */
package org.dasein.cloud.zimory.compute.vm;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VmStatistics;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.zimory.NoContextException;
import org.dasein.cloud.zimory.Zimory;
import org.dasein.cloud.zimory.ZimoryMethod;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implements support for Zimory virtual guests as Dasein Cloud virtual machines.
 * <p>Created by George Reese: 10/30/12 2:52 PM</p>
 * @author George Reese
 * @version 2012.09 initial version
 * @since 2012.09
 */
public class Deployments implements VirtualMachineSupport {
    static private final Logger logger = Zimory.getLogger(Deployments.class);

    private Zimory provider;

    public Deployments(@Nonnull Zimory provider) { this.provider = provider; }

    @Override
    public @Nonnull VirtualMachine clone(@Nonnull String vmId, @Nonnull String intoDcId, @Nonnull String name, @Nonnull String description, boolean powerOn, @Nullable String... firewallIds) throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void disableAnalytics(String vmId) throws InternalException, CloudException {
        // NO-OP
    }

    @Override
    public void enableAnalytics(String vmId) throws InternalException, CloudException {
        // NO-OP
    }

    @Override
    public @Nonnull String getConsoleOutput(@Nonnull String vmId) throws InternalException, CloudException {
        return "";
    }

    @Override
    public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
        return -2;
    }

    @Override
    public VirtualMachineProduct getProduct(@Nonnull String productId) throws InternalException, CloudException {
        String[] parts = productId.split(":");

        if( parts.length == 3 ) {
            int pu, ram, cpu;

            try {
                pu = Integer.parseInt(parts[0]);
                cpu = Integer.parseInt(parts[1]);
                ram = Integer.parseInt(parts[2]);
            }
            catch( NumberFormatException e ) {
                return null;
            }
            VirtualMachineProduct product = new VirtualMachineProduct();

            product.setProviderProductId(productId);
            product.setName(pu + " PU / " + cpu + " Core / " + ram + " MB");
            product.setDescription(pu + " Performance Units across " + cpu + " cores with " + ram + " MB RAM");
            product.setRamSize(new Storage<Megabyte>(ram, Storage.MEGABYTE));
            product.setRootVolumeSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
            product.setCpuCount(cpu);
            return product;
        }
        return null;
    }

    @Override
    public @Nonnull String getProviderTermForServer(@Nonnull Locale locale) {
        return "deployment";
    }

    @Override
    public VirtualMachine getVirtualMachine(@Nonnull String vmId) throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public VmStatistics getVMStatistics(String vmId, long from, long to) throws InternalException, CloudException {
        return null;
    }

    @Override
    public @Nonnull Iterable<VmStatistics> getVMStatisticsForPeriod(@Nonnull String vmId, @Nonnegative long from, @Nonnegative long to) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Requirement identifyPasswordRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyShellKeyRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyVlanRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public boolean isAPITerminationPreventable() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        return false;
    }

    @Nonnull
    @Override
    public VirtualMachine launch(VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
        StringBuilder xml = new StringBuilder();

        xml.append("<deploymentCreationInfo>");
        xml.append("<applianceId>").append(Zimory.escapeXml(withLaunchOptions.getMachineImageId())).append("</applianceId>");
        xml.append("<name>").append(Zimory.escapeXml(withLaunchOptions.getHostName())).append("</name>");
        xml.append("<description>").append(Zimory.escapeXml(withLaunchOptions.getDescription())).append("</description>");

        VirtualMachineProduct product = getProduct(withLaunchOptions.getStandardProductId());

        if( product == null ) {
            logger.error("Attempt to launch a VM with an unknown product " + withLaunchOptions.getStandardProductId());
            throw new CloudException("Unknown product: " + withLaunchOptions.getStandardProductId());
        }
        String[] id = product.getProviderProductId().split(":");

        xml.append("<performanceUnit>").append(Zimory.escapeXml(id[0])).append("</performanceUnit>");
        xml.append("<memoryMb>").append(String.valueOf(product.getRamSize().intValue())).append("</memoryMb>");
        xml.append("<virtualCPUs>").append(String.valueOf(product.getCpuCount())).append("</virtualCPUs>");
        xml.append("<useExternalIp>true</useExternalIp>");
        xml.append("<permanentIp>false</permanentIp>");
        if( withLaunchOptions.getVlanId() != null ) {
            xml.append("<network><networkId>").append(Zimory.escapeXml(withLaunchOptions.getVlanId())).append("</networkId></network>");
        }
        if( !withLaunchOptions.getMetaData().isEmpty() ) {
            // TODO: map tags to custom deployment properties
            xml.append("<customProperties>");
            for( Map.Entry<String,Object> entry : withLaunchOptions.getMetaData().entrySet() ) {
                xml.append("<customProperty><id>").append(Zimory.escapeXml(entry.getKey())).append("</id>");
                xml.append("<value>").append(Zimory.escapeXml(entry.getValue().toString())).append("</value></customProperty>");
            }
            xml.append("</customProperties>");
        }
        // TODO: figure out how these map
        // <providerId>1</providerId>
        // <qualifierId>5</qualifierId>
        // <reservationId>1</reservationId>
        xml.append("</deploymentCreationInfo>");

        ZimoryMethod method = new ZimoryMethod(provider);

        Document doc = method.postObject("deployments", xml.toString());

        if( doc == null ) {
            logger.error("Unable to POST to deployments endpoint");
            throw new CloudException("Unable to POST to deployments endpoint");
        }
        NodeList results = doc.getElementsByTagName("deployment");

        for( int i=0; i<results.getLength(); i++ ) {
            VirtualMachine vm = toVirtualMachine(results.item(i));

            if( vm != null ) {
                return vm;
            }
        }
        logger.error("The POST to create a new virtual machine in Zimory succeeded, but nothing was returned");
        throw new CloudException("The POST to create a new virtual machine in Zimory succeeded, but nothing was returned");
    }

    @Override
    public @Nonnull VirtualMachine launch(@Nonnull String fromMachineImageId, @Nonnull VirtualMachineProduct product, @Nonnull String dataCenterId, @Nonnull String name, @Nonnull String description, @Nullable String withKeypairId, @Nullable String inVlanId, boolean withAnalytics, boolean asSandbox, @Nullable String... firewallIds) throws InternalException, CloudException {
        return launch(fromMachineImageId, product, dataCenterId, name, description, withKeypairId, inVlanId, withAnalytics, asSandbox, firewallIds, new Tag[0]);
    }

    @Override
    public @Nonnull VirtualMachine launch(@Nonnull String fromMachineImageId, @Nonnull VirtualMachineProduct product, @Nonnull String dataCenterId, @Nonnull String name, @Nonnull String description, @Nullable String withKeypairId, @Nullable String inVlanId, boolean withAnalytics, boolean asSandbox, @Nullable String[] firewallIds, @Nullable Tag... tags) throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull Iterable<String> listFirewalls(@Nonnull String vmId) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    static private List<VirtualMachineProduct> products;
    @Override
    public Iterable<VirtualMachineProduct> listProducts(Architecture architecture) throws InternalException, CloudException {
        if( products == null ) {
            ArrayList<VirtualMachineProduct> tmp = new ArrayList<VirtualMachineProduct>();

            for( int i=1; i<=16; i++ ) {
                for( int cpu : new int[] { 1, 2, 4, 8, 16 } ) {
                    for( int ram : new int[] { 512, 1024, 2048, 4096, 8192, 10240, 16386, 20480, 24574 } ) {
                        VirtualMachineProduct product = new VirtualMachineProduct();
                        String id = i + ":" + cpu + ":" + ram;

                        product.setProviderProductId(id);
                        product.setName(i + " PU / " + cpu + " Core / " + ram + " MB");
                        product.setDescription(i + " Performance Units across " + cpu + " cores with " + ram + " MB RAM");
                        product.setRamSize(new Storage<Megabyte>(ram, Storage.MEGABYTE));
                        product.setRootVolumeSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
                        product.setCpuCount(cpu);
                        tmp.add(product);
                    }
                }
            }
            products = Collections.unmodifiableList(tmp);
        }
        return products;
    }

    static private List<Architecture> architectures;

    @Override
    public Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
        if( architectures == null ) {
            ArrayList<Architecture> tmp = new ArrayList<Architecture>();

            tmp.add(Architecture.I32);
            tmp.add(Architecture.I64);
            architectures = Collections.unmodifiableList(tmp);
        }
        return architectures;
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        ZimoryMethod method = new ZimoryMethod(provider);

        Document response = method.getObject("deployments");

        if( response == null ) {
            logger.error("Unable to identify endpoint for deployments in Zimory");
            throw new CloudException("Unable to identify endpoint for virtual machines (deployments)");
        }
        ArrayList<VirtualMachine> vms = new ArrayList<VirtualMachine>();

        // TODO: process response
        return vms;
    }

    @Override
    public void pause(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reboot(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void resume(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void start(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stop(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsAnalytics() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsPauseUnpause(@Nonnull VirtualMachine vm) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsStartStop(@Nonnull VirtualMachine vm) {
        return true;
    }

    @Override
    public boolean supportsSuspendResume(@Nonnull VirtualMachine vm) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void suspend(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void terminate(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void unpause(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    private @Nonnull VmState toState(@Nonnull String state) {
        if( state.equals("") ) {
            return VmState.PENDING;
        }
        else if( state.equals("STOPPED") ) {
            return VmState.STOPPED;
        }
        else {
            logger.warn("DEBUG: Unknown Zimory virtual machine state: " + state);
            System.out.println("Unknown Zimory virtual machine state: " + state);
            return VmState.PENDING;
        }
    }

    private @Nullable VirtualMachine toVirtualMachine(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        String id = null;

        if( node.hasAttributes() ) {
            Node attr = node.getAttributes().getNamedItem("id");

            if( attr != null ) {
                id = attr.getNodeValue().trim();
            }
        }
        if( id == null || id.equals("") ) {
            return null;
        }
        String imageId = null, name = null, locationId = null, networkId = null, password = null;
        NodeList attributes = node.getChildNodes();
        VmState state = VmState.PENDING;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);

            if( attr.getNodeName().equalsIgnoreCase("applianceId") && attr.hasChildNodes() ) {
                imageId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("locationId") && attr.hasChildNodes() ) {
                locationId = attr.getFirstChild().getNodeValue().trim();
                if( !locationId.equals(ctx.getRegionId()) ) {
                    return null;
                }
            }
            else if( attr.getNodeName().equalsIgnoreCase("name") && attr.hasChildNodes() ) {
                name = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("networkId") && attr.hasChildNodes() ) {
                networkId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("password") && attr.hasChildNodes() ) {
                password = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("state") && attr.hasChildNodes() ) {
                state = toState(attr.getFirstChild().getNodeValue().trim());
            }
        }
        if( locationId == null || locationId.equals("") ) {
            return null;
        }
        if( name == null || name.equals("") ) {
            name = id;
        }
        VirtualMachine vm = new VirtualMachine();

        vm.setProviderVirtualMachineId(id);
        vm.setProviderOwnerId(ctx.getAccountNumber());
        vm.setProviderRegionId(locationId);
        vm.setProviderDataCenterId(locationId);
        if( networkId != null && !networkId.equals("") ) {
            vm.setProviderVlanId(networkId);
        }

        vm.setName(name);
        vm.setDescription(name);
        vm.setCurrentState(state);
        if( password != null && !password.equals("") ) {
            vm.setRootPassword(password);
            vm.setRootUser("root");
        }

        vm.setPausable(false);
        vm.setPersistent(true);
        vm.setPlatform(Platform.UNKNOWN); // TODO: pull from appliance
        vm.setRebootable(true);
        vm.setArchitecture(Architecture.I64); // TODO: pull from appliance

        vm.setClonable(false);
        vm.setImagable(VmState.STOPPED.equals(vm.getCurrentState()));

        //vm.setProductId();
        /*try {
            if( json.has("createDate") ) {
                vm.setCreationTimestamp(provider.parseTimestamp(json.getString("createDate")));
            }
            if( json.has("primaryBackendIpAddress") ) {
                vm.setPrivateIpAddresses(new String[] { json.getString("primaryBackendIpAddress") } );
            }
            if( json.has("primaryIpAddress") ) {
                vm.setPublicIpAddresses(new String[] { json.getString("primaryIpAddress") });
            }
        }
        catch( JSONException e ) {
            logger.error("Error parsing JSON from cloud: " + e.getMessage());
            e.printStackTrace();
            throw new CloudException(e);
        }
                    */
        return vm;
    }
}
