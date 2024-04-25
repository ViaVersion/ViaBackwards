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
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.provider.TransferProvider;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.BlockItemPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.EntityPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.CookieStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.RegistryDataStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.SecureChatStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.rewriter.ComponentRewriter.ReadType;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_20_3To1_20_5 extends BackwardsProtocol<ClientboundPacket1_20_5, ClientboundPacket1_20_3, ServerboundPacket1_20_5, ServerboundPacket1_20_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.20.5", "1.20.3", Protocol1_20_5To1_20_3.class);
    private final EntityPacketRewriter1_20_5 entityRewriter = new EntityPacketRewriter1_20_5(this);
    private final BlockItemPacketRewriter1_20_5 itemRewriter = new BlockItemPacketRewriter1_20_5(this);
    private final TranslatableRewriter<ClientboundPacket1_20_5> translatableRewriter = new TranslatableRewriter<>(this, ReadType.NBT);
    private final TagRewriter<ClientboundPacket1_20_5> tagRewriter = new TagRewriter<>(this);

    public Protocol1_20_3To1_20_5() {
        super(ClientboundPacket1_20_5.class, ClientboundPacket1_20_3.class, ServerboundPacket1_20_5.class, ServerboundPacket1_20_3.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.registerGeneric(ClientboundPackets1_20_5.TAGS);
        registerClientbound(ClientboundConfigurationPackets1_20_5.UPDATE_TAGS, wrapper -> {
            // Send off registry data first
            final PacketWrapper registryDataPacket = wrapper.create(ClientboundConfigurationPackets1_20_3.REGISTRY_DATA);
            registryDataPacket.write(Type.COMPOUND_TAG, wrapper.user().get(RegistryDataStorage.class).registryData().copy());
            registryDataPacket.send(Protocol1_20_3To1_20_5.class);

            tagRewriter.getGenericHandler().handle(wrapper);
        });

        registerClientbound(ClientboundPackets1_20_5.START_CONFIGURATION, wrapper -> wrapper.user().get(RegistryDataStorage.class).clear());

        final SoundRewriter<ClientboundPacket1_20_5> soundRewriter = new SoundRewriter<>(this);
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

        registerServerbound(ServerboundPackets1_20_3.CHAT_COMMAND, ServerboundPackets1_20_5.CHAT_COMMAND_SIGNED, wrapper -> {
            final String command = wrapper.passthrough(Type.STRING); // Command
            wrapper.passthrough(Type.LONG); // Timestamp
            wrapper.passthrough(Type.LONG); // Salt
            final int signatures = wrapper.passthrough(Type.VAR_INT); // Signatures
            if (signatures == 0) {
                wrapper.cancel();

                final PacketWrapper chatCommand = wrapper.create(ServerboundPackets1_20_5.CHAT_COMMAND);
                chatCommand.write(Type.STRING, command);
                chatCommand.sendToServer(Protocol1_20_3To1_20_5.class);
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.COOKIE_REQUEST.getId(), -1, wrapper -> handleCookieRequest(wrapper, ServerboundLoginPackets.COOKIE_RESPONSE));
        cancelClientbound(ClientboundConfigurationPackets1_20_5.RESET_CHAT); // Old clients already reset chat when entering the configuration phase
        registerClientbound(ClientboundConfigurationPackets1_20_5.COOKIE_REQUEST, null, wrapper -> handleCookieRequest(wrapper, ServerboundConfigurationPackets1_20_5.COOKIE_RESPONSE));
        registerClientbound(ClientboundConfigurationPackets1_20_5.STORE_COOKIE, null, this::handleStoreCookie);
        registerClientbound(ClientboundConfigurationPackets1_20_5.TRANSFER, null, this::handleTransfer);
        registerClientbound(ClientboundPackets1_20_5.COOKIE_REQUEST, null, wrapper -> handleCookieRequest(wrapper, ServerboundPackets1_20_5.COOKIE_RESPONSE));
        registerClientbound(ClientboundPackets1_20_5.STORE_COOKIE, null, this::handleStoreCookie);
        registerClientbound(ClientboundPackets1_20_5.TRANSFER, null, this::handleTransfer);

        registerClientbound(ClientboundConfigurationPackets1_20_5.SELECT_KNOWN_PACKS, null, wrapper -> {
            wrapper.cancel();

            final PacketWrapper response = wrapper.create(ServerboundConfigurationPackets1_20_5.SELECT_KNOWN_PACKS);
            response.write(Type.VAR_INT, 0); // Empty, we don't know anything
            response.sendToServer(Protocol1_20_3To1_20_5.class);
        });

        new CommandRewriter1_19_4<ClientboundPacket1_20_5>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) throws Exception {
                if (argumentType.equals("minecraft:loot_table")
                    || argumentType.equals("minecraft:loot_predicate")
                    || argumentType.equals("minecraft:loot_modifier")) {
                    wrapper.write(Type.VAR_INT, 0);
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        }.registerDeclareCommands1_19(ClientboundPackets1_20_5.DECLARE_COMMANDS);

        registerClientbound(State.LOGIN, ClientboundLoginPackets.GAME_PROFILE, wrapper -> {
            wrapper.passthrough(Type.UUID); // UUID
            wrapper.passthrough(Type.STRING); // Name

            final int properties = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < properties; i++) {
                wrapper.passthrough(Type.STRING); // Name
                wrapper.passthrough(Type.STRING); // Value
                wrapper.passthrough(Type.OPTIONAL_STRING); // Signature
            }

            wrapper.read(Type.BOOLEAN); // Strict error handling
        });

        cancelClientbound(ClientboundPackets1_20_5.PROJECTILE_POWER);
        cancelClientbound(ClientboundPackets1_20_5.DEBUG_SAMPLE);
    }

    private void handleStoreCookie(final PacketWrapper wrapper) throws Exception {
        wrapper.cancel();

        final String resourceLocation = wrapper.read(Type.STRING);
        final byte[] data = wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
        if (data.length > 5120) {
            throw new IllegalArgumentException("Cookie data too large");
        }

        wrapper.user().get(CookieStorage.class).cookies().put(resourceLocation, data);
    }

    private void handleCookieRequest(final PacketWrapper wrapper, final ServerboundPacketType responseType) throws Exception {
        wrapper.cancel();

        final String resourceLocation = wrapper.read(Type.STRING);
        final byte[] data = wrapper.user().get(CookieStorage.class).cookies().get(resourceLocation);
        final PacketWrapper responsePacket = wrapper.create(responseType);
        responsePacket.write(Type.STRING, resourceLocation);
        responsePacket.write(Type.OPTIONAL_BYTE_ARRAY_PRIMITIVE, data);
        responsePacket.sendToServer(Protocol1_20_3To1_20_5.class);
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
        user.put(new RegistryDataStorage());
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
    public TranslatableRewriter<ClientboundPacket1_20_5> getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_20_5> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_20_5, ClientboundPacket1_20_3, ServerboundPacket1_20_5, ServerboundPacket1_20_3> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_20_5.class, ClientboundConfigurationPackets1_20_5.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_20_3.class, ClientboundConfigurationPackets1_20_3.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_20_5.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_20_3.class, ServerboundConfigurationPackets1_20_2.class)
        );
    }
}