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
package com.viaversion.viabackwards.protocol.v1_20_5to1_20_3;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.provider.TransferProvider;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter.BlockItemPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter.BlockPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter.ComponentRewriter1_20_5;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter.EntityPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.CookieStorage;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.RegistryDataStorage;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.SecureChatStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.storage.ArmorTrimStorage;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.storage.BannerPatternStorage;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_20_5To1_20_3 extends BackwardsProtocol<ClientboundPacket1_20_5, ClientboundPacket1_20_3, ServerboundPacket1_20_5, ServerboundPacket1_20_3> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.20.5", "1.20.3", Protocol1_20_3To1_20_5.class);
    private final EntityPacketRewriter1_20_5 entityRewriter = new EntityPacketRewriter1_20_5(this);
    private final BlockItemPacketRewriter1_20_5 itemRewriter = new BlockItemPacketRewriter1_20_5(this);
    private final ParticleRewriter<ClientboundPacket1_20_5> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPacket1_20_5> translatableRewriter = new ComponentRewriter1_20_5(this);
    private final TagRewriter<ClientboundPacket1_20_5> tagRewriter = new TagRewriter<>(this);
    private final BlockPacketRewriter1_20_5 blockRewriter = new BlockPacketRewriter1_20_5(this);

    public Protocol1_20_5To1_20_3() {
        super(ClientboundPacket1_20_5.class, ClientboundPacket1_20_3.class, ServerboundPacket1_20_5.class, ServerboundPacket1_20_3.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.addEmptyTag(RegistryType.ITEM, "minecraft:axolotl_tempt_items");
        replaceClientbound(ClientboundConfigurationPackets1_20_5.UPDATE_TAGS, wrapper -> {
            // Send off registry data first, needed for tags
            sendRegistryData(wrapper.user());
            tagRewriter.handleGeneric(wrapper);
        });

        appendClientbound(ClientboundConfigurationPackets1_20_5.FINISH_CONFIGURATION, wrapper -> {
            // In case the server for some reason does not send tags
            sendRegistryData(wrapper.user());
        });
        appendClientbound(ClientboundPackets1_20_5.START_CONFIGURATION, wrapper -> wrapper.user().get(RegistryDataStorage.class).clear());

        registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO, wrapper -> {
            wrapper.passthrough(Types.STRING); // Server ID
            wrapper.passthrough(Types.BYTE_ARRAY_PRIMITIVE); // Public key
            wrapper.passthrough(Types.BYTE_ARRAY_PRIMITIVE); // Challenge
            wrapper.read(Types.BOOLEAN); // Authenticate
        });

        replaceClientbound(ClientboundPackets1_20_5.SERVER_DATA, wrapper -> {
            translatableRewriter.passthroughAndProcess(wrapper); // MOTD
            wrapper.passthrough(Types.OPTIONAL_BYTE_ARRAY_PRIMITIVE); // Icon
            wrapper.write(Types.BOOLEAN, wrapper.user().get(SecureChatStorage.class).enforcesSecureChat());
        });

        // Always write as signed, even if there is 0 signatures attached, else the validation chain gets broken
        registerServerbound(ServerboundPackets1_20_3.CHAT_COMMAND, ServerboundPackets1_20_5.CHAT_COMMAND_SIGNED);

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
            response.write(Types.VAR_INT, 0); // Empty, we don't know anything
            response.sendToServer(Protocol1_20_5To1_20_3.class);
        });

        final CommandRewriter1_19_4<ClientboundPacket1_20_5> commandRewriter = new CommandRewriter1_19_4<>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) {
                if (argumentType.equals("minecraft:loot_table")
                    || argumentType.equals("minecraft:loot_predicate")
                    || argumentType.equals("minecraft:loot_modifier")) {
                    wrapper.write(Types.VAR_INT, 0);
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        };
        replaceClientbound(ClientboundPackets1_20_5.COMMANDS, commandRewriter::handle1_19);

        registerClientbound(State.LOGIN, ClientboundLoginPackets.LOGIN_FINISHED, wrapper -> {
            wrapper.passthrough(Types.UUID); // UUID
            wrapper.passthrough(Types.STRING); // Name
            wrapper.passthrough(Types.PROFILE_PROPERTY_ARRAY);
            wrapper.read(Types.BOOLEAN); // Strict error handling
        });

        cancelClientbound(ClientboundPackets1_20_5.PROJECTILE_POWER);
        cancelClientbound(ClientboundPackets1_20_5.DEBUG_SAMPLE);
    }

    private void sendRegistryData(final UserConnection connection) {
        final RegistryDataStorage registryDataStorage = connection.get(RegistryDataStorage.class);

        final CompoundTag registryData = registryDataStorage.registryData();
        if (!registryDataStorage.sentRegistryData() && !registryData.isEmpty()) {
            final PacketWrapper registryDataPacket = PacketWrapper.create(ClientboundConfigurationPackets1_20_3.REGISTRY_DATA, connection);
            registryDataPacket.write(Types.COMPOUND_TAG, registryData.copy());
            registryDataPacket.send(Protocol1_20_5To1_20_3.class);
            registryDataStorage.setSentRegistryData();
        }
    }

    private void handleStoreCookie(final PacketWrapper wrapper) {
        wrapper.cancel();

        final String resourceLocation = wrapper.read(Types.STRING);
        final byte[] data = wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
        if (data.length > 5120) {
            throw new IllegalArgumentException("Cookie data too large");
        }

        wrapper.user().get(CookieStorage.class).cookies().put(resourceLocation, data);
    }

    private void handleCookieRequest(final PacketWrapper wrapper, final ServerboundPacketType responseType) {
        wrapper.cancel();

        final String resourceLocation = wrapper.read(Types.STRING);
        final byte[] data = wrapper.user().get(CookieStorage.class).cookies().get(resourceLocation);
        final PacketWrapper responsePacket = wrapper.create(responseType);
        responsePacket.write(Types.STRING, resourceLocation);
        responsePacket.write(Types.OPTIONAL_BYTE_ARRAY_PRIMITIVE, data);
        responsePacket.sendToServer(Protocol1_20_5To1_20_3.class);
    }

    private void handleTransfer(final PacketWrapper wrapper) {
        wrapper.cancel();

        final String host = wrapper.read(Types.STRING);
        final int port = wrapper.read(Types.VAR_INT);
        Via.getManager().getProviders().get(TransferProvider.class).connectToServer(wrapper.user(), host, port);
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
        user.put(new SecureChatStorage());
        user.put(new CookieStorage());
        user.put(new RegistryDataStorage());
        user.put(new BannerPatternStorage());
        user.put(new ArmorTrimStorage());
    }

    @Override
    public void register(final ViaProviders providers) {
        providers.register(TransferProvider.class, TransferProvider.NOOP);
    }

    @Override
    public BackwardsMappingData getMappingData() {
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
    public BlockPacketRewriter1_20_5 getBlockRewriter() {
        return blockRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_20_5> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPacket1_20_5> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_20_5> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V1_20_5;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return new Types1_20_3();
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
