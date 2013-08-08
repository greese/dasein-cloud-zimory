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

package org.dasein.cloud.zimory.compute.vm;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.zimory.NoContextException;
import org.dasein.cloud.zimory.Zimory;
import org.dasein.cloud.zimory.ZimoryConfigurationException;
import org.dasein.cloud.zimory.ZimoryMethod;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
public class Deployments extends AbstractVMSupport {
    static private final Logger logger = Zimory.getLogger(Deployments.class);

    private Zimory provider;

    public Deployments(@Nonnull Zimory provider) {
        super(provider);
        this.provider = provider;
    }

    static public class MI {
        public String imageId;
        public Platform platform;
        public Architecture architecture;
    }

    private @Nonnull MI getImage(@Nullable String imageId) throws CloudException, InternalException {
        MI mi = null;

        if( imageId == null ) {
            mi = new MI();
            mi.platform = Platform.UNKNOWN;
            mi.architecture = Architecture.I64;
        }
        else {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<MI> cache = Cache.getInstance(provider, "machineImages", MI.class, CacheLevel.REGION);
            ArrayList<MI> images = (ArrayList<MI>)cache.get(ctx);
            if( images != null ) {
                for( MI m : images ) {
                    if( imageId.equals(m.imageId) ) {
                        mi = m;
                        break;
                    }
                }
            }
            if( mi == null ) {
                MachineImage img = provider.getComputeServices().getImageSupport().getImage(imageId);

                if( img == null ) {
                    mi = new MI();
                    mi.imageId = imageId;
                    mi.platform = Platform.UNKNOWN;
                    mi.architecture = Architecture.I64;
                }
                else {
                    mi = new MI();
                    mi.imageId = imageId;
                    mi.platform = img.getPlatform();
                    mi.architecture = img.getArchitecture();
                }
                if( images == null ) {
                    images = new ArrayList<MI>();
                }
                images.add(mi);
                cache.put(ctx, images);
            }
        }
        return mi;
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
    public @Nullable VirtualMachine getVirtualMachine(@Nonnull String vmId) throws InternalException, CloudException {
        APITrace.begin(provider, "getVirtualMachine");
        try {
            try {
                Long.parseLong(vmId);
            }
            catch( NumberFormatException ignore ) {
                return null;
            }
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("deployments/" + vmId);

            if( response == null ) {
                logger.error("Unable to identify endpoint for deployments in Zimory");
                throw new CloudException("Unable to identify endpoint for virtual machines (deployments)");
            }
            NodeList list = response.getElementsByTagName("deployment");

            for( int i=0; i<list.getLength(); i++ ) {
                VirtualMachine vm = toVirtualMachine(list.item(i));

                if( vm != null ) {
                    return vm;
                }
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Requirement identifyImageRequirement(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return (ImageClass.MACHINE.equals(cls) ? Requirement.REQUIRED : Requirement.NONE);
    }

    @Override
    public @Nonnull Requirement identifyPasswordRequirement(Platform platform) throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyShellKeyRequirement(Platform platform) throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyStaticIPRequirement() throws CloudException, InternalException {
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
        APITrace.begin(provider, "isSubscribedVirtualMachines");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            return (method.getObject("accounts") != null);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        return false;
    }


    // <cpuNumber>1</cpuNumber>
    // <memSize>512</memSize>
    // <customProperties/>

    // <netTrough>0</netTrough>
    // <qualityClassId>1</qualityClassId>
    // <reservedResourceId>766</reservedResourceId>
    // <storageSize>0</storageSize>



    // <qualifierId>7</qualifierId>

    @Override
    public @Nonnull VirtualMachine launch(@Nonnull VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
        APITrace.begin(provider, "launchVirtualMachine");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            String[] parts = ctx.getRegionId().split(":");
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
            xml.append("<locationId>").append(parts[0]).append("</locationId>");
            xml.append("<providerId>").append(parts[1]).append("</providerId>");

            String qualifierId = provider.getQualifierId(parts[0], parts[1]);

            xml.append("<qualifierId>").append(qualifierId).append("</qualifierId>");


            String delegateRoleId = provider.getDelegateRoleId();

            xml.append("<defaultDelegateRole><id>").append(delegateRoleId).append("</id>").append("</defaultDelegateRole>");

            xml.append("</deploymentCreationInfo>");

            ZimoryMethod method = new ZimoryMethod(provider);
            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L);

            method.postObject("deployments", xml.toString());

            while( timeout > System.currentTimeMillis() ) {
                for( VirtualMachine vm : listVirtualMachines() ) {
                    if( vm.getName().equalsIgnoreCase(withLaunchOptions.getHostName()) ) {
                        return vm;
                    }
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            /*
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
            */
            logger.error("The POST to create a new virtual machine in Zimory succeeded, but nothing was returned");
            throw new CloudException("The POST to create a new virtual machine in Zimory succeeded, but nothing was returned");
        }
        finally {
            APITrace.end();
        }
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
    public @Nonnull Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
        APITrace.begin(provider, "listVirtualMachineStatus");
        try {
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
            ArrayList<ResourceStatus> vms = new ArrayList<ResourceStatus>();
            NodeList list = response.getElementsByTagName("deployment");

            for( int i=0; i<list.getLength(); i++ ) {
                ResourceStatus status = toStatus(list.item(i));

                if( status != null ) {
                    vms.add(status);
                }
            }
            return vms;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        APITrace.begin(provider, "listVirtualMachines");
        try {
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
            NodeList list = response.getElementsByTagName("deployment");

            for( int i=0; i<list.getLength(); i++ ) {
                VirtualMachine vm = toVirtualMachine(list.item(i));

                if( vm != null ) {
                    vms.add(vm);
                }
            }
            return vms;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void start(@Nonnull String vmId) throws InternalException, CloudException {
        APITrace.begin(provider, "startVm");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            method.postString("deployments/" + vmId + "/start", "");
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void stop(@Nonnull String vmId, boolean force) throws InternalException, CloudException {
        APITrace.begin(provider, "stopVm");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            method.postString("deployments/" + vmId + "/stop", "");
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean supportsPauseUnpause(@Nonnull VirtualMachine vm) {
        return false;
    }

    @Override
    public boolean supportsStartStop(@Nonnull VirtualMachine vm) {
        return true;
    }

    @Override
    public boolean supportsSuspendResume(@Nonnull VirtualMachine vm) {
        return false;
    }

    @Override
    public void terminate(@Nonnull String vmId) throws InternalException, CloudException {
        APITrace.begin(provider, "terminateVm");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            method.delete("deployments/" + vmId);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void terminate(@Nonnull String vmId, String explanation)throws InternalException, CloudException{
        terminate(vmId);
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
        else if( state.equals("RUNNING") ) {
            return VmState.RUNNING;
        }
        else {
            logger.warn("DEBUG: Unknown Zimory virtual machine state: " + state);
            System.out.println("Unknown Zimory virtual machine state: " + state);
            return VmState.PENDING;
        }
    }

    private @Nullable ResourceStatus toStatus(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new ZimoryConfigurationException("No region was configured for this request");
        }
        String id = null, locationId = null, providerId = null;

        if( node.hasAttributes() ) {
            Node attr = node.getAttributes().getNamedItem("id");

            if( attr != null ) {
                id = attr.getNodeValue().trim();
            }
        }
        if( id == null || id.equals("") ) {
            return null;
        }
        NodeList attributes = node.getChildNodes();
        VmState state = VmState.PENDING;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);

            if( attr.getNodeName().equalsIgnoreCase("state") && attr.hasChildNodes() ) {
                state = toState(attr.getFirstChild().getNodeValue().trim());
            }
            else if( attr.getNodeName().equalsIgnoreCase("locationId") && attr.hasChildNodes() ) {
                locationId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("providerId") && attr.hasChildNodes() ) {
                providerId = attr.getFirstChild().getNodeValue().trim();
            }
        }
        if( !regionId.equals(locationId + ":" + providerId) ) {
            return null;
        }
        return new ResourceStatus(id, state);
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
        String imageId = null, name = null, locationId = null, networkId = null, password = null, providerId = null, publicIp= null, privateIp = null;
        NodeList attributes = node.getChildNodes();
        int memory = 512, cpu = 1, performance = 1;
        long creationDate = 0L;
        VmState state = null;
        Boolean active = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attr = attributes.item(i);

            if( attr.getNodeName().equalsIgnoreCase("applianceId") && attr.hasChildNodes() ) {
                imageId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("locationId") && attr.hasChildNodes() ) {
                locationId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("providerId") && attr.hasChildNodes() ) {
                providerId = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("name") && attr.hasChildNodes() ) {
                name = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("active") && attr.hasChildNodes() ) {
                active = attr.getFirstChild().getNodeValue().trim().equalsIgnoreCase("true");
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
            else if( attr.getNodeName().equalsIgnoreCase("memSize") && attr.hasChildNodes() ) {
                memory = Integer.parseInt(attr.getFirstChild().getNodeValue().trim());
            }
            else if( attr.getNodeName().equalsIgnoreCase("cpuNumber") && attr.hasChildNodes() ) {
                cpu = Integer.parseInt(attr.getFirstChild().getNodeValue().trim());
            }
            else if( attr.getNodeName().equalsIgnoreCase("performanceUnit") && attr.hasChildNodes() ) {
                performance = Integer.parseInt(attr.getFirstChild().getNodeValue().trim());
            }
            else if( attr.getNodeName().equalsIgnoreCase("state") && attr.hasChildNodes() ) {
                state = toState(attr.getFirstChild().getNodeValue().trim());
            }
            else if( attr.getNodeName().equalsIgnoreCase("active") && attr.hasChildNodes() && state == null ) {
                if( attr.getFirstChild().getNodeValue().trim().equalsIgnoreCase("false") ) {
                    state = VmState.PENDING;
                }
            }
            else if( attr.getNodeName().equalsIgnoreCase("creationDate") && attr.hasChildNodes() ) {
                String ds = attr.getFirstChild().getNodeValue().trim();

                if( ds.length() > 0 ) {
                    creationDate = provider.parseTimestamp(ds);
                }
            }
            else if( attr.getNodeName().equalsIgnoreCase("externalIpAddress") ) {
                publicIp = attr.getFirstChild().getNodeValue().trim();
            }
            else if( attr.getNodeName().equalsIgnoreCase("internalIpAddress") ) {
                privateIp = attr.getFirstChild().getNodeValue().trim();
            }
        }
        if( !ctx.getRegionId().equals(locationId + ":" + providerId) ) {
            return null;
        }
        if( state == null ) {
            if( active == null || !active ) {
                return null;
            }
            else {
                state = VmState.STOPPED;
            }
        }
        if( name == null || name.equals("") ) {
            name = id;
        }
        VirtualMachine vm = new VirtualMachine();

        vm.setProviderVirtualMachineId(id);
        vm.setProviderOwnerId(ctx.getAccountNumber());
        vm.setProviderRegionId(ctx.getRegionId());
        vm.setProviderDataCenterId(ctx.getRegionId());
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
        vm.setRebootable(true);

        vm.setClonable(false);
        vm.setImagable(VmState.STOPPED.equals(vm.getCurrentState()));

        MI img = getImage(imageId);

        vm.setPlatform(img.platform);
        vm.setArchitecture(img.architecture);
        vm.setProviderMachineImageId(imageId);
        vm.setCreationTimestamp(creationDate);
        vm.setProductId(performance + ":" + cpu + ":" + memory);
        if( publicIp != null ) {
            vm.setPublicAddresses(new RawAddress(publicIp));
        }
        if( privateIp != null ) {
            vm.setPrivateAddresses(new RawAddress(privateIp));
        }

        return vm;
    }
}
