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

package org.dasein.cloud.zimory.compute;

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.zimory.Zimory;
import org.dasein.cloud.zimory.compute.image.Appliances;
import org.dasein.cloud.zimory.compute.storage.NetworkVolume;
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

    @Override
    public @Nonnull NetworkVolume getVolumeSupport() {
        return new NetworkVolume(provider);
    }
}
