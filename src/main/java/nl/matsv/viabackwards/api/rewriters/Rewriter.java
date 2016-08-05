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

package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;

public abstract class Rewriter<T extends BackwardsProtocol> {

    /**
     * Register everything
     *
     * @param protocol Protocol instance
     */
    public void register(T protocol) {
        registerPackets(protocol);
        registerRewrites();
    }

    /**
     * Register packet listeners
     *
     * @param protocol Protocol instance
     */
    protected abstract void registerPackets(T protocol);

    /**
     * Register rewrites
     */
    protected abstract void registerRewrites();
}
