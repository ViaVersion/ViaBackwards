/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectArrayMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectMap;

import static com.viaversion.viabackwards.utils.ChatUtil.translate;

public final class ServerLinks implements StorableObject {

    private static final CompoundTag REPORT_BUG = translate("known_server_link.report_bug");
    private static final CompoundTag COMMUNITY_GUIDELINES = translate("known_server_link.community_guidelines");
    private static final CompoundTag SUPPORT = translate("known_server_link.support");
    private static final CompoundTag STATUS = translate("known_server_link.status");
    private static final CompoundTag FEEDBACK = translate("known_server_link.feedback");
    private static final CompoundTag COMMUNITY = translate("known_server_link.community");
    private static final CompoundTag WEBSITE = translate("known_server_link.website");
    private static final CompoundTag FORUMS = translate("known_server_link.forums");
    private static final CompoundTag NEWS = translate("known_server_link.news");
    private static final CompoundTag ANNOUNCEMENTS = translate("known_server_link.announcements");

    private final Object2ObjectMap<Tag, String> links = new Object2ObjectArrayMap<>();

    public void storeLink(final Tag tag, final String uri) {
        links.put(tag, uri);
    }

    public void storeLink(final int id, final String uri) {
        switch (id) {
            case 1 -> storeLink(COMMUNITY_GUIDELINES, uri);
            case 2 -> storeLink(SUPPORT, uri);
            case 3 -> storeLink(STATUS, uri);
            case 4 -> storeLink(FEEDBACK, uri);
            case 5 -> storeLink(COMMUNITY, uri);
            case 6 -> storeLink(WEBSITE, uri);
            case 7 -> storeLink(FORUMS, uri);
            case 8 -> storeLink(NEWS, uri);
            case 9 -> storeLink(ANNOUNCEMENTS, uri);
            default -> storeLink(REPORT_BUG, uri);
        }
    }

    public Object2ObjectMap<Tag, String> links() {
        return links;
    }
}
