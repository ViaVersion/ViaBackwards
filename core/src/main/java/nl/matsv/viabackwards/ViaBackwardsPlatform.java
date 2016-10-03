/*
 *
 *     Copyright (C) 2016 Matsv
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.matsv.viabackwards;

import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9To1_10;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;

import java.util.Collections;
import java.util.logging.Logger;

public interface ViaBackwardsPlatform {
    /**
     * Initialize ViaBackwards
     */
    default void init() {
        ViaBackwards.init(this);
        ProtocolRegistry.registerProtocol(new Protocol1_9To1_10(), Collections.singletonList(ProtocolVersion.v1_9_3.getId()), ProtocolVersion.v1_10.getId());
    }

    /**
     * Logger provided by the platform
     *
     * @return logger instance
     */
    Logger getLogger();
}
