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
package org.dasein.cloud.zimory.compute.image;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.identity.ServiceAction;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * Implements Dasein Cloud machine image support tying in Zimory appliances as machine images.
 * <p>Created by George Reese: 10/30/12 9:37 PM</p>
 * @author George Reese
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class Appliances extends AbstractImageSupport {
    static private final Logger logger = Zimory.getLogger(Appliances.class);

    private Zimory provider;

    public Appliances(@Nonnull Zimory provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public MachineImage getImage(@Nonnull String providerImageId) throws CloudException, InternalException {
        APITrace.begin(provider, "getImage");
        try {
            try {
                Long.parseLong(providerImageId);
            }
            catch( NumberFormatException ignore ) {
                return null;
            }
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("appliances/" + providerImageId);

            if( response == null ) {
                logger.error("Unable to identify endpoint for deployments in Zimory");
                throw new CloudException("Unable to identify endpoint for virtual machines (deployments)");
            }
            NodeList appliances = response.getElementsByTagName("appliance");

            for( int i=0; i<appliances.getLength(); i++ ) {
                MachineImage img = toMachineImage(appliances.item(i));

                if( img != null ) {
                    return img;
                }
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull String getProviderTermForImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        return "appliance";
    }

    @Override
    public boolean hasPublicLibrary() {
        return true;
    }

    @Override
    public @Nonnull Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public boolean isImageSharedWithPublic(@Nonnull String providerImageId) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        APITrace.begin(provider, "isSubscribedMachineImages");
        try {
            ZimoryMethod method = new ZimoryMethod(provider);

            return (method.getObject("accounts") != null);
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<ResourceStatus> listImageStatus(@Nonnull ImageClass cls) throws CloudException, InternalException {
        APITrace.begin(provider, "listImageStatus");
        if( !ImageClass.MACHINE.equals(cls) ) {
            return Collections.emptyList();
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("appliances");

            if( response == null ) {
                logger.error("Unable to identify endpoint for deployments in Zimory");
                throw new CloudException("Unable to identify endpoint for virtual machines (deployments)");
            }
            ArrayList<ResourceStatus> images = new ArrayList<ResourceStatus>();
            NodeList appliances = response.getElementsByTagName("appliance");

            for( int i=0; i<appliances.getLength(); i++ ) {
                ResourceStatus status = toStatus(appliances.item(i), ctx.getAccountNumber());

                if( status != null ) {
                    images.add(status);
                }
            }
            return images;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<MachineImage> listImages(@Nullable ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(provider, "listImages");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            ImageClass cls = (options == null ? null : options.getImageClass());
            String ownedBy = (options == null ? null : options.getAccountNumber());

            if( cls != null && !ImageClass.MACHINE.equals(cls) ) {
                return Collections.emptyList();
            }

            ZimoryMethod method = new ZimoryMethod(provider);

            Document response = method.getObject("appliances");

            if( response == null ) {
                logger.error("Unable to identify endpoint for deployments in Zimory");
                throw new CloudException("Unable to identify endpoint for virtual machines (deployments)");
            }
            ArrayList<MachineImage> images = new ArrayList<MachineImage>();
            NodeList appliances = response.getElementsByTagName("appliance");

            for( int i=0; i<appliances.getLength(); i++ ) {
                MachineImage img = toMachineImage(appliances.item(i));
                if( img != null && (ownedBy == null || ownedBy.equals(img.getProviderOwnerId())) ) {
                    images.add(img);
                }
            }
            return images;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<String> listShares(@Nonnull String providerImageId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
        return Collections.singletonList(ImageClass.MACHINE);
    }

    @Override
    public @Nonnull Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return Collections.singletonList(MachineImageType.VOLUME);
    }

    @Override
    public void remove(@Nonnull String providerImageId, boolean checkState) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Cannot remove this image");
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }


    @Override
    public @Nonnull Iterable<MachineImage> searchImages(@Nullable String accountNumber, @Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture, @Nullable ImageClass... imageClasses) throws CloudException, InternalException {
        APITrace.begin(provider, "searchImages");
        try {
            ArrayList<MachineImage> images = new ArrayList<MachineImage>();

            if( accountNumber == null ) {
                ZimoryMethod method = new ZimoryMethod(provider);
                Document response = method.getObject("appliances");

                if( response == null ) {
                    logger.error("Unable to identify endpoint for deployments in Zimory");
                    throw new CloudException("Unable to identify endpoint for virtual machines (deployments)");
                }
                NodeList appliances = response.getElementsByTagName("appliance");

                for( int i=0; i<appliances.getLength(); i++ ) {
                    MachineImage img = toMachineImage(appliances.item(i));
                    if( img != null && img.getProviderOwnerId() != null && matches(img, keyword, platform, architecture) ) {
                        images.add(img);
                    }
                }
            }
            else {
                for( MachineImage img : listImages(ImageClass.MACHINE, accountNumber) ) {
                    if( matches(img, keyword, platform, architecture) ) {
                        images.add(img);
                    }
                }
            }
            return images;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public @Nonnull Iterable<MachineImage> searchPublicImages(@Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture, @Nullable ImageClass... imageClasses) throws CloudException, InternalException {
        APITrace.begin(provider, "searchPublicImages");
        try {
            System.out.println("PIS: " + keyword + "/" + platform + "/" + architecture + "/" + Arrays.toString(imageClasses));
            if( imageClasses != null && imageClasses.length > 0 ) {
                boolean ok = false;

                for( ImageClass cls : imageClasses ) {
                    if( cls.equals(ImageClass.MACHINE) ) {
                        ok = true;
                    }
                }
                if( !ok ) {
                    return Collections.emptyList();
                }
            }
            ArrayList<MachineImage> images = new ArrayList<MachineImage>();
            ZimoryMethod method = new ZimoryMethod(provider);
            Document response = method.getObject("appliances");

            if( response == null ) {
                logger.error("Unable to identify endpoint for deployments in Zimory");
                throw new CloudException("Unable to identify endpoint for virtual machines (deployments)");
            }
            NodeList appliances = response.getElementsByTagName("appliance");

            System.out.println("Possibles=" + appliances.getLength());
            for( int i=0; i<appliances.getLength(); i++ ) {
                MachineImage img = toMachineImage(appliances.item(i));
                if( img != null && img.getProviderOwnerId() == null && matches(img, keyword, platform, architecture) ) {
                    images.add(img);
                }
            }
            return images;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean supportsCustomImages() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsDirectImageUpload() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageCapture(@Nonnull MachineImageType type) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageSharing() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsImageSharingWithPublic() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsPublicLibrary(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return true;
    }

    private @Nullable MachineImage toMachineImage(@Nullable Node node) throws CloudException, InternalException {
        if( node == null ) {
            return null;
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        MachineImage image = new MachineImage();
        NodeList attributes = node.getChildNodes();
        String osId = null;

        if( node.hasAttributes() ) {
            Node idNode = node.getAttributes().getNamedItem("id");

            if( idNode != null ) {
                image.setProviderMachineImageId(idNode.getNodeValue().trim());
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
        image.setProviderOwnerId(null);
        image.setProviderRegionId(ctx.getRegionId());
        image.setSoftware("");
        image.setStorageFormat(null);
        image.setType(MachineImageType.VOLUME);
        image.setCurrentState(MachineImageState.ACTIVE);
        image.setImageClass(ImageClass.MACHINE);
        image.setPlatform(Platform.UNKNOWN);
        image.setArchitecture(Architecture.I64);
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeName().equalsIgnoreCase("architecture") && attribute.hasChildNodes() ) {
                String architecture = attribute.getFirstChild().getNodeValue().trim();

                if( architecture.equals("32") ) {
                    image.setArchitecture(Architecture.I32);
                }
                else if( architecture.equals("64") ) {
                    image.setArchitecture(Architecture.I64);
                }
                else {
                    logger.warn("DEBUG: Unknown Zimory architecture: " + architecture);
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("custom") ) {
                if( attribute.hasChildNodes() && attribute.getFirstChild().getNodeValue().trim().equalsIgnoreCase("true") ) {
                    image.setProviderOwnerId(ctx.getAccountNumber());
                }
                else {
                    image.setProviderOwnerId(null);
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("description") && attribute.hasChildNodes() ) {
                image.setDescription(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
                image.setName(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("osId") && attribute.hasChildNodes() ) {
                osId = attribute.getFirstChild().getNodeValue().trim();
            }
        }

        if( image.getProviderMachineImageId() == null ) {
            return null;
        }
        if( image.getName() == null ) {
            image.setName(image.getProviderMachineImageId());
        }
        if( image.getDescription() == null ) {
            image.setDescription(image.getName());
        }
        image.setPlatform(toPlatform(osId, image.getName(), image.getDescription()));
        return image;
    }

    private @Nonnull Platform toPlatform(@Nullable String osId, @Nullable String name, @Nullable String description) {
        if( osId == null && name == null && description == null ) {
            return Platform.UNKNOWN;
        }
        if( osId != null ) {
            if( osId.equals("1") ) {
                return Platform.WINDOWS;
            }
            else if( osId.equals("2") ) {
                return Platform.SUSE;
            }
            else {
                logger.warn("DEBUG: Unknown Zimory OS ID: " + osId);
            }
        }
        if( name == null && description != null ) {
            return Platform.UNKNOWN;
        }
        else if( name == null ) {
            return Platform.guess(description);
        }
        else if( description == null ) {
            return Platform.guess(name);
        }
        return Platform.guess(name + " " + description);
    }

    private @Nullable ResourceStatus toStatus(@Nullable Node node, @Nullable String accountNumber) throws InternalException, CloudException {
        if( node == null ) {
            return null;
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new NoContextException();
        }
        NodeList attributes = node.getChildNodes();
        String id;

        if( node.hasAttributes() ) {
            Node idNode = node.getAttributes().getNamedItem("id");

            if( idNode != null ) {
                id = idNode.getNodeValue().trim();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeName().equalsIgnoreCase("custom") ) {
                if( attribute.hasChildNodes() && attribute.getFirstChild().getNodeValue().trim().equalsIgnoreCase("true") ) {
                    if( accountNumber != null && accountNumber.equals(ctx.getAccountNumber()) ) {
                        return new ResourceStatus(id, MachineImageState.ACTIVE);
                    }
                }
                else {
                    if( accountNumber == null ) {
                        return new ResourceStatus(id, MachineImageState.ACTIVE);
                    }
                }
            }
        }
        return null;
    }
}
