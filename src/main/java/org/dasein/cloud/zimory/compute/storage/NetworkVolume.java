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
package org.dasein.cloud.zimory.compute.storage;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeFormat;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.compute.VolumeType;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.zimory.NoContextException;
import org.dasein.cloud.zimory.Zimory;
import org.dasein.cloud.zimory.ZimoryConfigurationException;
import org.dasein.cloud.zimory.ZimoryMethod;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Implements support for Zimory network volumes as a Dasein Cloud volume.
 * <p>Created by George Reese: 11/20/12 7:18 PM</p>
 * @author George Reese
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class NetworkVolume implements VolumeSupport {
    static private final Logger logger = Zimory.getLogger(NetworkVolume.class);

    private Zimory provider;

    public NetworkVolume(@Nonnull Zimory provider) { this.provider = provider; }

    @Override
    public void attach(@Nonnull String volumeId, @Nonnull String toServer, @Nonnull String deviceId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Dynamic attachment of network volumes is not supported");
    }

    @Override
    public @Nonnull String create(@Nullable String fromSnapshot, @Nonnegative int sizeInGb, @Nonnull String inZone) throws InternalException, CloudException {
        if( fromSnapshot != null ) {
            return createVolume(VolumeCreateOptions.getInstanceForSnapshot(fromSnapshot, new Storage<Gigabyte>(sizeInGb, Storage.GIGABYTE), "dsn-auto-volume", "dsn-auto-volume").inDataCenter(inZone));
        }
        else {
            return createVolume(VolumeCreateOptions.getInstance(new Storage<Gigabyte>(sizeInGb, Storage.GIGABYTE), "dsn-auto-volume", "dsn-auto-volume").inDataCenter(inZone));
        }
    }

    //<networkStorageCreationInfoMPO>
    //<networkStorageName>new storage</networkStorageName>
    //<networkStorageDescription>description</networkStorageDescription>
    //<networkStorageProvider>Dummy</networkStorageProvider>
    //<networkStorageExternalProtocol>dummy</networkStorageExternalProtocol>
    //<networkStorageSizeGb>128</networkStorageSizeGb>
    //<network>
    //<networkId>1</networkId>
    //</network>
    //<providerId>1</providerId>
    //<qualifierId>5</qualifierId>
    //</networkStorageCreationInfoMPO>

    @Override
    public @Nonnull String createVolume(@Nonnull VolumeCreateOptions options) throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new ZimoryConfigurationException("No region was specified for this request");
        }
        String productId = options.getVolumeProductId();
        StringBuilder xml = new StringBuilder();
        VolumeProduct prd = null;

        for( VolumeProduct p : listVolumeProducts() ) {
            if( p.getProviderProductId().equals(productId) ) {
                prd = p;
                break;
            }
        }
        if( prd == null ) {
            throw new CloudException("No such product: " + productId);
        }
        String[] product = prd.getProviderProductId().split(":");
        xml.append("<networkStorageCreationInfoMPO>");
        xml.append("<networkStorageProvider>").append(product[0]).append("</networkStorageProvider>");
        xml.append("<networkStorageExternalProtocol>").append(product[1]).append("</networkStorageExternalProtocol>");
        xml.append("<networkStorageName>").append(Zimory.escapeXml(options.getName())).append("</networkStorageName>");
        xml.append("<networkStorageDescription>").append(Zimory.escapeXml(options.getDescription())).append("</networkStorageDescription>");
        xml.append("<networkStorageSizeGb>").append(options.getVolumeSize().intValue()).append("</networkStorageSizeGb>");
        xml.append("<network><networkId>").append(options.getVlanId()).append("</networkId></network>");

        String[] parts = regionId.split(":");

        xml.append("<providerId>").append(parts[1]).append("</providerId>");

        String qualifierId = provider.getQualifierId(parts[0], parts[1]);

        xml.append("<qualifierId>").append(qualifierId).append("</qualifierId>");

        xml.append("</networkStorageCreationInfoMPO>");

        ZimoryMethod method = new ZimoryMethod(provider);

        Document doc = method.postObject("networkStorages", xml.toString());

        if( doc == null ) {
            logger.error("Unable to POST to network storages endpoint");
            throw new CloudException("Unable to POST to network storages endpoint");
        }
        NodeList results = doc.getElementsByTagName("networkStorage");

        for( int i=0; i<results.getLength(); i++ ) {
            Volume volume = toVolume(results.item(i));

            if( volume != null ) {
                return volume.getProviderVolumeId();
            }
        }
        logger.error("The POST to create a new volume in Zimory succeeded, but nothing was returned");
        throw new CloudException("The POST to create a new volume in Zimory succeeded, but nothing was returned");

    }

    @Override
    public void detach(@Nonnull String volumeId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Dynamic attachment of network volumes is not supported");
    }

    @Override
    public void detach(@Nonnull String volumeId, boolean force) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Dynamic attachment of network volumes is not supported");
    }

    @Override
    public int getMaximumVolumeCount() throws InternalException, CloudException {
        return -2;
    }

    @Override
    public Storage<Gigabyte> getMaximumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(100, Storage.GIGABYTE);
    }

    @Override
    public @Nonnull Storage<Gigabyte> getMinimumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(50, Storage.GIGABYTE);
    }

    @Override
    public @Nonnull String getProviderTermForVolume(@Nonnull Locale locale) {
        return "network storage";
    }

    @Override
    public Volume getVolume(@Nonnull String volumeId) throws InternalException, CloudException {
        APITrace.begin(provider, "getVolume");
        try {
            try {
                Long.parseLong(volumeId);
            }
            catch( NumberFormatException ignore ) {
                return null;
            }
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("networkStorages/" + volumeId);

            if( response == null ) {
                logger.error("Unable to identify endpoint for network storage in Zimory");
                throw new CloudException("Unable to identify endpoint for volumes (network storage)");
            }
            NodeList list = response.getElementsByTagName("networkStorage");

            for( int i=0; i<list.getLength(); i++ ) {
                Volume v = toVolume(list.item(i));

                if( v != null ) {
                    return v;
                }
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Requirement getVolumeProductRequirement() throws InternalException, CloudException {
        return Requirement.REQUIRED;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        APITrace.begin(provider, "isSubscribedVolumes");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            return (method.getObject("accounts") != null);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isVolumeSizeDeterminedByProduct() throws InternalException, CloudException {
        return false;
    }

    @Override
    public @Nonnull Iterable<String> listPossibleDeviceIds(@Nonnull Platform platform) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<VolumeFormat> listSupportedFormats() throws InternalException, CloudException {
        return Collections.singletonList(VolumeFormat.NFS);
    }

    @Override
    public @Nonnull Iterable<VolumeProduct> listVolumeProducts() throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        Cache<VolumeProduct> cache = Cache.getInstance(provider, "volumeProducts", VolumeProduct.class, CacheLevel.CLOUD, new TimePeriod<Day>(1, TimePeriod.DAY));
        Iterable<VolumeProduct> products = cache.get(ctx);

        if( products == null ) {
            ArrayList<VolumeProduct> tmp = new ArrayList<VolumeProduct>();

            tmp.add(VolumeProduct.getInstance("Dummy:dummy", "Dummy", "Dummy", VolumeType.HDD));
            tmp.add(VolumeProduct.getInstance("NETAPP:nfs", "NetApp NFS", "NetApp via NFS", VolumeType.HDD));
            products = Collections.unmodifiableList(tmp);
            cache.put(ctx, products);
        }
        return products;
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listVolumeStatus() throws InternalException, CloudException {
        APITrace.begin(provider, "listVolumeStatus");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("networkStorages");

            if( response == null ) {
                logger.error("Unable to identify endpoint for network storage in Zimory");
                throw new CloudException("Unable to identify endpoint for volumes (network storage)");
            }
            ArrayList<ResourceStatus> volumes = new ArrayList<ResourceStatus>();
            NodeList list = response.getElementsByTagName("networkStorage");

            for( int i=0; i<list.getLength(); i++ ) {
                ResourceStatus v = toStatus(list.item(i));

                if( v != null ) {
                    volumes.add(v);
                }
            }
            return volumes;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<Volume> listVolumes() throws InternalException, CloudException {
        APITrace.begin(provider, "listVolumes");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("networkStorages");

            if( response == null ) {
                logger.error("Unable to identify endpoint for network storage in Zimory");
                throw new CloudException("Unable to identify endpoint for volumes (network storage)");
            }
            ArrayList<Volume> volumes = new ArrayList<Volume>();
            NodeList list = response.getElementsByTagName("networkStorage");

            for( int i=0; i<list.getLength(); i++ ) {
                Volume v = toVolume(list.item(i));

                if( v != null ) {
                    volumes.add(v);
                }
            }
            return volumes;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public void remove(@Nonnull String volumeId) throws InternalException, CloudException {
        APITrace.begin(provider, "removeVolume");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            method.delete("networkStorages/" + volumeId);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    private @Nullable ResourceStatus toStatus(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        VolumeState state = VolumeState.AVAILABLE;

        Node idNode = node.getAttributes().getNamedItem("id");
        if( idNode == null ) {
            return null;
        }
        return new ResourceStatus(idNode.getNodeValue().trim(), state);
    }

    //<networkStorageSizeGb>128</networkStorageSizeGb>
    private @Nullable Volume toVolume(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        Volume volume = new Volume();

        volume.setProviderRegionId(ctx.getRegionId());
        volume.setProviderDataCenterId(ctx.getRegionId());
        volume.setRootVolume(false);
        volume.setType(VolumeType.HDD);
        volume.setFormat(VolumeFormat.NFS);
        volume.setCurrentState(VolumeState.AVAILABLE);

        NodeList attributes = node.getChildNodes();
        String provider = null, protocol = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeName().equalsIgnoreCase("networkStorageName") && attribute.hasChildNodes() ) {
                volume.setName(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkStorageId") && attribute.hasChildNodes() ) {
                volume.setProviderVolumeId(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkStorageProvider") && attribute.hasChildNodes() ) {
                provider = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkStorageStorageExportProtocol") && attribute.hasChildNodes() ) {
                protocol = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkStorageDescription") && attribute.hasChildNodes() ) {
                volume.setDescription(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkStorageStorageSizeGb") && attribute.hasChildNodes() ) {
                try {
                    volume.setSize(new Storage<Gigabyte>(Integer.parseInt(attribute.getFirstChild().getNodeValue().trim()), Storage.GIGABYTE));
                }
                catch( NumberFormatException e ) {
                    logger.warn("Unknown storage size: " + attribute.getFirstChild().getNodeValue().trim());
                    volume.setSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkStorageSizeGb") && attribute.hasChildNodes() ) {
                int size;

                try {
                    size = Integer.parseInt(attribute.getFirstChild().getNodeValue().trim());
                }
                catch( NumberFormatException e ) {
                    logger.warn("Invalid volume size: " + attribute.getFirstChild().getNodeValue().trim());
                    size = 1;
                }
                volume.setSize(new Storage<Gigabyte>(size, Storage.GIGABYTE));
            }
            else if( attribute.getNodeName().equalsIgnoreCase("networkStorageStorageExportUrl") && attribute.hasChildNodes() ) {
                volume.setDeviceId(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("network") && attribute.hasChildNodes() ) {
                NodeList networkInfo = attribute.getChildNodes();

                for( int j=0; j<networkInfo.getLength(); j++ ) {
                    Node na = networkInfo.item(j);

                    if( na.getNodeName().equalsIgnoreCase("networkId") && na.hasChildNodes() ) {
                        volume.setProviderVlanId(na.getFirstChild().getNodeValue().trim());
                    }
                }
            }
        }
        if( volume.getProviderVolumeId() == null ) {
            return null;
        }
        volume.setProviderProductId(provider + ":" + protocol);
        if( volume.getName() == null ) {
            volume.setName(volume.getProviderVolumeId());
        }
        if( volume.getDescription() == null ) {
            volume.setDescription(volume.getName());
        }
        return volume;
    }
}
