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
package org.dasein.cloud.zimory.network;

import org.dasein.cloud.network.AbstractNetworkServices;
import org.dasein.cloud.zimory.Zimory;
import org.dasein.cloud.zimory.network.vlan.Networks;

import javax.annotation.Nonnull;

/**
 * Gateway into the network services supported through Zimory.
 * <p>Created by George Reese: 11/20/12 9:12 AM</p>
 * @author George Reese
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class ZimoryNetwork extends AbstractNetworkServices {
    private Zimory provider;

    public ZimoryNetwork(@Nonnull Zimory provider) { this.provider = provider; }

    @Override
    public @Nonnull Networks getVlanSupport() {
        return new Networks(provider);
    }
}
