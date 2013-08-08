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
