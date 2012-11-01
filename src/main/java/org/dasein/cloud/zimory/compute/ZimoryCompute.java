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
package org.dasein.cloud.zimory.compute;

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.zimory.Zimory;
import org.dasein.cloud.zimory.compute.image.Appliances;
import org.dasein.cloud.zimory.compute.vm.Deployments;

import javax.annotation.Nonnull;

/**
 * Implements compute services for Zimory in accordance with the Dasein Cloud API.
 * <p>Created by George Reese: 10/30/12 2:51 PM</p>
 * @author George Reese
 * @version 2012.09 initial version
 * @since 2012.09 initial version
 */
public class ZimoryCompute extends AbstractComputeServices {
    private Zimory provider;

    public ZimoryCompute(@Nonnull Zimory provider) { this.provider = provider; }

    @Override
    public @Nonnull Appliances getImageSupport() {
        return new Appliances(provider);
    }

    @Override
    public @Nonnull Deployments getVirtualMachineSupport() {
        return new Deployments(provider);
    }
}
