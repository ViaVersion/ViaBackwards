/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.provider.TransferProvider;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.BlockItemPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.EntityPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.CookieStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.SecureChatStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ByteArrayType;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.rewriter.ComponentRewriter.ReadType;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

public final class Protocol1_20_3To1_20_5 extends BackwardsProtocol<ClientboundPackets1_20_5, ClientboundPackets1_20_3, ServerboundPackets1_20_5, ServerboundPackets1_20_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private static final ByteArrayType COOKIE_DATA_TYPE = new ByteArrayType(5120);
    private final EntityPacketRewriter1_20_5 entityRewriter = new EntityPacketRewriter1_20_5(this);
    private final BlockItemPacketRewriter1_20_5 itemRewriter = new BlockItemPacketRewriter1_20_5(this);
    private final TranslatableRewriter<ClientboundPackets1_20_5> translatableRewriter = new TranslatableRewriter<>(this, ReadType.NBT);

    public Protocol1_20_3To1_20_5() {
        super(ClientboundPackets1_20_5.class, ClientboundPackets1_20_3.class, ServerboundPackets1_20_5.class, ServerboundPackets1_20_3.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        final TagRewriter<ClientboundPackets1_20_5> tagRewriter = new TagRewriter<>(this);
        tagRewriter.registerGeneric(ClientboundPackets1_20_5.TAGS);
        tagRewriter.registerGeneric(State.CONFIGURATION, ClientboundConfigurationPackets1_20_5.UPDATE_TAGS);

        final SoundRewriter<ClientboundPackets1_20_5> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.SOUND);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.ENTITY_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_20_5.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_5.STATISTICS);

        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_20_5.BOSSBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_20_5.TAB_LIST);
        translatableRewriter.registerCombatKill1_20(ClientboundPackets1_20_5.COMBAT_KILL);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.DISGUISED_CHAT);
        translatableRewriter.registerPing();


        registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO, wrapper -> {
            wrapper.passthrough(Type.STRING); // Server ID
            wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE); // Public key
            wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE); // Challenge
            wrapper.read(Type.BOOLEAN); // Authenticate
        });

        registerClientbound(ClientboundPackets1_20_5.SERVER_DATA, wrapper -> {
            wrapper.passthrough(Type.TAG); // MOTD
            wrapper.passthrough(Type.OPTIONAL_BYTE_ARRAY_PRIMITIVE); // Icon
            wrapper.write(Type.BOOLEAN, wrapper.user().get(SecureChatStorage.class).enforcesSecureChat());
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.COOKIE_REQUEST.getId(), -1, wrapper -> handleCookieRequest(wrapper, ServerboundLoginPackets.COOKIE_RESPONSE));
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_5.COOKIE_REQUEST.getId(), -1, wrapper -> handleCookieRequest(wrapper, ServerboundConfigurationPackets1_20_5.COOKIE_RESPONSE));
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_5.STORE_COOKIE.getId(), -1, this::handleStoreCookie);
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_5.TRANSFER.getId(), -1, this::handleTransfer);
        registerClientbound(ClientboundPackets1_20_5.COOKIE_REQUEST, null, wrapper -> handleCookieRequest(wrapper, ServerboundPackets1_20_5.COOKIE_RESPONSE));
        registerClientbound(ClientboundPackets1_20_5.STORE_COOKIE, null, this::handleStoreCookie);
        registerClientbound(ClientboundPackets1_20_5.TRANSFER, null, this::handleTransfer);

        registerClientboundPacketIdChanges(State.CONFIGURATION, ClientboundConfigurationPackets1_20_5.class, ClientboundConfigurationPackets1_20_3.class);
        registerServerboundPacketIdChanges(State.CONFIGURATION, ServerboundConfigurationPackets1_20_2.class, ServerboundConfigurationPackets1_20_5.class);
    }

    private void handleStoreCookie(final PacketWrapper wrapper) throws Exception {
        wrapper.cancel();

        final String resourceLocation = wrapper.read(Type.STRING);
        final byte[] data = wrapper.read(COOKIE_DATA_TYPE);
        wrapper.user().get(CookieStorage.class).cookies().put(resourceLocation, data);
    }

    private void handleCookieRequest(final PacketWrapper wrapper, final ServerboundPacketType responseType) throws Exception {
        wrapper.cancel();

        final String resourceLocation = wrapper.read(Type.STRING);
        final byte[] data = wrapper.user().get(CookieStorage.class).cookies().get(resourceLocation);
        if (data == null) {
            return;
        }

        final PacketWrapper responsePacket = wrapper.create(responseType);
        responsePacket.write(Type.STRING, resourceLocation);
        responsePacket.write(COOKIE_DATA_TYPE, data);
        responsePacket.scheduleSendToServer(Protocol1_20_3To1_20_5.class);
    }

    private void handleTransfer(final PacketWrapper wrapper) throws Exception {
        wrapper.cancel();

        final String host = wrapper.read(Type.STRING);
        final int port = wrapper.read(Type.VAR_INT);
        Via.getManager().getProviders().get(TransferProvider.class).connectToServer(wrapper.user(), host, port);
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
        user.put(new SecureChatStorage());
        user.put(new CookieStorage());
    }

    @Override
    public void register(final ViaProviders providers) {
        providers.register(TransferProvider.class, TransferProvider.NOOP);
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_20_5 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_20_5 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_20_5> getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    protected ClientboundPacketType clientboundFinishConfigurationPacket() {
        return ClientboundConfigurationPackets1_20_5.FINISH_CONFIGURATION;
    }

    @Override
    protected ServerboundPacketType serverboundFinishConfigurationPacket() {
        return ServerboundConfigurationPackets1_20_2.FINISH_CONFIGURATION;
    }
}