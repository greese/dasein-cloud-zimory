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
package org.dasein.cloud.zimory;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Region;
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
 * Implements data center services for Zimory describing the different Zimory regions. This class maps
 * Zimory data centers to Dasein Cloud regions. Zimory regions are ignored.
 * <p>Created by George Reese: 10/25/12 7:18 PM</p>
 * @author George Reese
 * @version 2012.09 initial version
 * @since 2012.09
 */
public class ZimoryDataCenters implements DataCenterServices {
    static private final Logger logger = Zimory.getLogger(ZimoryDataCenters.class);

    private Zimory provider;

    ZimoryDataCenters(@Nonnull Zimory provider) { this.provider = provider; }

    @Override
    public @Nullable DataCenter getDataCenter(@Nonnull String dataCenterId) throws InternalException, CloudException {
        for( Region region : listRegions() ) {
            for( DataCenter dc : listDataCenters(region.getProviderRegionId()) ) {
                if( dataCenterId.equals(dc.getProviderDataCenterId()) ) {
                    return dc;
                }
            }
        }
        return null;
    }

    @Override
    public @Nonnull String getProviderTermForDataCenter(@Nonnull Locale locale) {
        return "data center";
    }

    @Override
    public @Nonnull String getProviderTermForRegion(@Nonnull Locale locale) {
        return "region";
    }

    @Override
    public @Nullable Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for( Region r : listRegions() ) {
            if( providerRegionId.equals(r.getProviderRegionId()) ) {
                return r;
            }
        }
        return null;
    }

    @Override
    public @Nonnull Collection<DataCenter> listDataCenters(@Nonnull String providerRegionId) throws InternalException, CloudException {
        Region r = getRegion(providerRegionId);

        if( r == null ) {
            throw new CloudException("No such region: " + providerRegionId);
        }
        DataCenter dc = new DataCenter();

        dc.setActive(r.isActive());
        dc.setAvailable(r.isAvailable());
        dc.setName(r.getName());
        dc.setProviderDataCenterId(providerRegionId);
        dc.setRegionId(providerRegionId);
        return Collections.singletonList(dc);
    }

    @Override
    public Collection<Region> listRegions() throws InternalException, CloudException {
        ZimoryMethod method = new ZimoryMethod(provider);
        Document xml = method.getObject("constants/locations");

        if( xml == null ) {
            logger.error("Unable to communicate with the Zimory locations endpoint");
            throw new CloudException("Could not communicate with the Zimory locations endpoint");
        }
        NodeList locations = xml.getElementsByTagName("location");
        ArrayList<Region> regions = new ArrayList<Region>();

        for( int i=0; i<locations.getLength(); i++ ) {
            Node location = locations.item(i);

            if( location.hasChildNodes() ) {
                Region r = toRegion(location);

                if( r != null ) {
                    regions.add(r);
                }
            }
        }

        return regions;
    }

    private @Nullable Region toRegion(@Nullable Node xml) throws CloudException, InternalException {
        if( xml == null ) {
            return null;
        }

        NodeList attributes = xml.getChildNodes();
        String description = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeName().equalsIgnoreCase("description") && attribute.hasChildNodes() ) {
                description = attribute.getFirstChild().getNodeValue().trim();
            }
        }
        if( description == null ) {
            return null;
        }

        Region region = new Region();

        region.setActive(true);
        region.setAvailable(true);
        region.setJurisdiction("EU");
        region.setName(description);
        region.setProviderRegionId(description);

        if( region.getName() == null ) {
            region.setName(region.getProviderRegionId());
        }
        if( description.toLowerCase().startsWith("eu") ) {
            region.setJurisdiction("EU");
        }
        return region;
    }
}
