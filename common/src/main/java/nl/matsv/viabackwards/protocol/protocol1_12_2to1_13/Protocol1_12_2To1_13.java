/*
 * ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.BlockItemPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.EntityPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.PlayerPacket1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.SoundPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.PlayerPositionStorage1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.TabCompleteStorage;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import us.myles.ViaVersion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.gson.JsonObject;

public class Protocol1_12_2To1_13 extends BackwardsProtocol<ClientboundPackets1_13, ClientboundPackets1_12_1, ServerboundPackets1_13, ServerboundPackets1_12_1> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private BlockItemPackets1_13 blockItemPackets;

    public Protocol1_12_2To1_13() {
        super(ClientboundPackets1_13.class, ClientboundPackets1_12_1.class, ServerboundPackets1_13.class, ServerboundPackets1_12_1.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_13To1_12_2.class, () -> {
            MAPPINGS.load();
            PaintingMapping.init();
            Via.getManager().getProviders().register(BackwardsBlockEntityProvider.class, new BackwardsBlockEntityProvider());
        });

        TranslatableRewriter translatableRewriter = new TranslatableRewriter(this) {
            @Override
            protected void handleTranslate(JsonObject root, String translate) {
                String newTranslate = newTranslatables.get(translate);
                if (newTranslate != null || (newTranslate = getMappingData().getTranslateMappings().get(translate)) != null) {
                    root.addProperty("translate", newTranslate);
                }
            }
        };
        translatableRewriter.registerPing();
        translatableRewriter.registerBossBar(ClientboundPackets1_13.BOSSBAR);
        translatableRewriter.registerChatMessage(ClientboundPackets1_13.CHAT_MESSAGE);
        translatableRewriter.registerLegacyOpenWindow(ClientboundPackets1_13.OPEN_WINDOW);
        translatableRewriter.registerDisconnect(ClientboundPackets1_13.DISCONNECT);
        translatableRewriter.registerCombatEvent(ClientboundPackets1_13.COMBAT_EVENT);
        translatableRewriter.registerTitle(ClientboundPackets1_13.TITLE);
        translatableRewriter.registerTabList(ClientboundPackets1_13.TAB_LIST);

        (blockItemPackets = new BlockItemPackets1_13(this)).register();
        new EntityPackets1_13(this).register();
        new PlayerPacket1_13(this).register();
        new SoundPackets1_13(this).register();

        cancelOutgoing(ClientboundPackets1_13.DECLARE_COMMANDS); //TODO
        cancelOutgoing(ClientboundPackets1_13.NBT_QUERY);
        cancelOutgoing(ClientboundPackets1_13.CRAFT_RECIPE_RESPONSE);
        cancelOutgoing(ClientboundPackets1_13.UNLOCK_RECIPES);
        cancelOutgoing(ClientboundPackets1_13.ADVANCEMENTS);
        cancelOutgoing(ClientboundPackets1_13.DECLARE_RECIPES);
        cancelOutgoing(ClientboundPackets1_13.TAGS);

        cancelIncoming(ServerboundPackets1_12_1.CRAFT_RECIPE_REQUEST);
        cancelIncoming(ServerboundPackets1_12_1.RECIPE_BOOK_DATA);
    }

    @Override
    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld(user));
        }

        initEntityTracker(user);

        // Register Block Storage
        if (!user.has(BackwardsBlockStorage.class)) {
            user.put(new BackwardsBlockStorage(user));
        }
        // Register Block Storage
        if (!user.has(TabCompleteStorage.class)) {
            user.put(new TabCompleteStorage(user));
        }

        if (ViaBackwards.getConfig().isFix1_13FacePlayer() && !user.has(PlayerPositionStorage1_13.class)) {
            user.put(new PlayerPositionStorage1_13(user));
        }
    }

    public BlockItemPackets1_13 getBlockItemPackets() {
        return blockItemPackets;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }
}
