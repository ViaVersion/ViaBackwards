/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.Dialog;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.provider.ChestDialogViewProvider;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.provider.DialogViewProvider;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.rewriter.BlockItemPacketRewriter1_21_6;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.rewriter.ComponentRewriter1_21_6;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.rewriter.EntityPacketRewriter1_21_6;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.ChestDialogStorage;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.ClickEvents;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.RegistryAndTags;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.ServerLinks;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_6;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundConfigurationPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundConfigurationPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.Key;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_21_6To1_21_5 extends BackwardsProtocol<ClientboundPacket1_21_6, ClientboundPacket1_21_5, ServerboundPacket1_21_6, ServerboundPacket1_21_5> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21.6", "1.21.5", Protocol1_21_5To1_21_6.class);
    private final EntityPacketRewriter1_21_6 entityRewriter = new EntityPacketRewriter1_21_6(this);
    private final BlockItemPacketRewriter1_21_6 itemRewriter = new BlockItemPacketRewriter1_21_6(this);
    private final ParticleRewriter<ClientboundPacket1_21_6> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket1_21_6> translatableRewriter = new ComponentRewriter1_21_6(this);
    private final TagRewriter<ClientboundPacket1_21_6> tagRewriter = new TagRewriter<>(this);

    public Protocol1_21_6To1_21_5() {
        super(ClientboundPacket1_21_6.class, ClientboundPacket1_21_5.class, ServerboundPacket1_21_6.class, ServerboundPacket1_21_5.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        registerClientbound(ClientboundPackets1_21_6.UPDATE_TAGS, this::updateTags);
        registerClientbound(ClientboundConfigurationPackets1_21_6.UPDATE_TAGS, this::updateTags);

        final SoundRewriter<ClientboundPacket1_21_6> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_6.SOUND);
        appendClientbound(ClientboundPackets1_21_6.SOUND, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Source
            fixSoundSource(wrapper);
        });
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_6.SOUND_ENTITY);
        appendClientbound(ClientboundPackets1_21_6.SOUND_ENTITY, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Source
            fixSoundSource(wrapper);
        });
        soundRewriter.registerStopSound(ClientboundPackets1_21_6.STOP_SOUND);
        appendClientbound(ClientboundPackets1_21_6.STOP_SOUND, wrapper -> {
            final byte flags = wrapper.get(Types.BYTE, 0);
            if ((flags & 0x01) != 0) {
                fixSoundSource(wrapper);
            }
        });

        new StatisticsRewriter<>(this).register(ClientboundPackets1_21_6.AWARD_STATS);
        new AttributeRewriter<>(this).register1_21(ClientboundPackets1_21_6.UPDATE_ATTRIBUTES);
        new CommandRewriter1_19_4<>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) {
                if (argumentType.equals("minecraft:hex_color") || argumentType.equals("minecraft:dialog")) {
                    wrapper.write(Types.VAR_INT, 0); // Word
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        }.registerDeclareCommands1_19(ClientboundPackets1_21_6.COMMANDS);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_21_6.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_6.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_6.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_6.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_6.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_6.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_6.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_6.PLAYER_COMBAT_KILL);
        translatableRewriter.registerPlayerInfoUpdate1_21_4(ClientboundPackets1_21_6.PLAYER_INFO_UPDATE);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_6.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_21_6.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat1_21_5(ClientboundPackets1_21_6.PLAYER_CHAT);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_21_4(ClientboundPackets1_21_6.LEVEL_PARTICLES);
        particleRewriter.registerExplode1_21_2(ClientboundPackets1_21_6.EXPLODE);

        registerClientbound(ClientboundPackets1_21_6.CHANGE_DIFFICULTY, wrapper -> {
            final int difficulty = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.UNSIGNED_BYTE, (short) difficulty);
        });
        registerServerbound(ServerboundPackets1_21_5.CHANGE_DIFFICULTY, wrapper -> {
            final short difficulty = wrapper.read(Types.UNSIGNED_BYTE);
            wrapper.write(Types.VAR_INT, (int) difficulty);
        });

        // Are you sure you want to see this? This is your last chance to turn back.
        registerClientbound(ClientboundPackets1_21_6.SHOW_DIALOG, null, wrapper -> {
            wrapper.cancel();

            final RegistryAndTags registryAndTags = wrapper.user().get(RegistryAndTags.class);
            final ServerLinks serverLinks = wrapper.user().get(ServerLinks.class);
            final int id = wrapper.read(Types.VAR_INT) - 1;
            CompoundTag tag;
            if (id == -1) {
                tag = (CompoundTag) wrapper.read(Types.TAG);
            } else {
                tag = registryAndTags.fromRegistry(id);
            }

            final DialogViewProvider provider = Via.getManager().getProviders().get(DialogViewProvider.class);
            provider.openDialog(wrapper.user(), new Dialog(registryAndTags, serverLinks, tag));
        });
        registerClientbound(ClientboundConfigurationPackets1_21_6.SHOW_DIALOG, null, wrapper -> {
            wrapper.cancel();

            final RegistryAndTags registryAndTags = wrapper.user().get(RegistryAndTags.class);
            final ServerLinks serverLinks = wrapper.user().get(ServerLinks.class);
            final CompoundTag tag = (CompoundTag) wrapper.read(Types.TAG);

            final DialogViewProvider provider = Via.getManager().getProviders().get(DialogViewProvider.class);
            provider.openDialog(wrapper.user(), new Dialog(registryAndTags, serverLinks, tag));
        });
        registerClientbound(ClientboundPackets1_21_6.CLEAR_DIALOG, null, this::clearDialog);
        registerClientbound(ClientboundConfigurationPackets1_21_6.CLEAR_DIALOG, null, this::clearDialog);

        registerClientbound(ClientboundPackets1_21_6.SERVER_LINKS, this::storeServerLinks);
        registerClientbound(ClientboundConfigurationPackets1_21_6.SERVER_LINKS, this::storeServerLinks);

        registerServerbound(ServerboundPackets1_21_5.CHAT_COMMAND, wrapper -> {
            final String command = wrapper.passthrough(Types.STRING);

            final ClickEvents clickEvents = wrapper.user().get(ClickEvents.class);
            if (clickEvents.handleChatCommand(wrapper.user(), command)) {
                wrapper.cancel();
            }
        });

        // The ones below are specific to the chest dialog view provider
        registerServerbound(ServerboundPackets1_21_5.CONTAINER_CLOSE, wrapper -> {
            final ChestDialogStorage storage = wrapper.user().get(ChestDialogStorage.class);
            if (storage == null) {
                return;
            }

            final ChestDialogViewProvider provider = (ChestDialogViewProvider) Via.getManager().getProviders().get(DialogViewProvider.class);
            if (storage.phase() == ChestDialogStorage.Phase.ANVIL_VIEW) {
                wrapper.cancel();
                provider.openChestView(wrapper.user(), storage, ChestDialogStorage.Phase.DIALOG_VIEW);
                return;
            }
            if (storage.phase() == ChestDialogStorage.Phase.WAITING_FOR_RESPONSE) {
                wrapper.cancel();
                if (storage.closeButtonEnabled()) {
                    provider.openChestView(wrapper.user(), storage, ChestDialogStorage.Phase.DIALOG_VIEW);
                } else {
                    provider.openChestView(wrapper.user(), storage, ChestDialogStorage.Phase.WAITING_FOR_RESPONSE);
                }
                return;
            }

            final boolean allowClosing = storage.allowClosing();
            if (!allowClosing) {
                wrapper.cancel();
                if (storage.dialog().canCloseWithEscape()) {
                    provider.clickButton(wrapper.user(), Dialog.AfterAction.CLOSE, storage.dialog().actionButton());
                } else {
                    provider.openChestView(wrapper.user(), storage, ChestDialogStorage.Phase.DIALOG_VIEW);
                }
            }
            storage.setAllowClosing(false);
        });
        registerServerbound(ServerboundPackets1_21_5.RENAME_ITEM, wrapper -> {
            final ChestDialogStorage storage = wrapper.user().get(ChestDialogStorage.class);
            if (storage == null || storage.phase() != ChestDialogStorage.Phase.ANVIL_VIEW) {
                return;
            }

            wrapper.cancel();
            final String name = wrapper.read(Types.STRING);

            final ChestDialogViewProvider provider = (ChestDialogViewProvider) Via.getManager().getProviders().get(DialogViewProvider.class);
            provider.updateAnvilText(wrapper.user(), name);
        });
        appendServerbound(ServerboundPackets1_21_5.CONTAINER_CLICK, wrapper -> {
            final ChestDialogViewProvider provider = (ChestDialogViewProvider) Via.getManager().getProviders().get(DialogViewProvider.class);
            if (provider == null) {
                return;
            }

            final int containerId = wrapper.get(Types.VAR_INT, 0);
            final int slot = wrapper.get(Types.SHORT, 0);
            final byte button = wrapper.get(Types.BYTE, 0);
            final int mode = wrapper.get(Types.VAR_INT, 2);
            if (provider.clickDialog(wrapper.user(), containerId, slot, button, mode)) {
                wrapper.cancel();
            }
        });

        cancelClientbound(ClientboundPackets1_21_6.TRACKED_WAYPOINT);
    }

    private void fixSoundSource(final PacketWrapper wrapper) {
        final int source = wrapper.get(Types.VAR_INT, 0);
        if (source == 10) { // New ui source, map to master
            wrapper.set(Types.VAR_INT, 0, 0);
        }
    }

    private void updateTags(final PacketWrapper wrapper) {
        tagRewriter.handleGeneric(wrapper);
        wrapper.resetReader();

        final RegistryAndTags registryAndTags = wrapper.user().get(RegistryAndTags.class);
        final int length = wrapper.passthrough(Types.VAR_INT);
        for (int i = 0; i < length; i++) {
            final String registryKey = wrapper.read(Types.STRING);
            final boolean dialog = "dialog".equals(Key.stripMinecraftNamespace(registryKey));
            if (dialog) {
                final int tagsSize = wrapper.read(Types.VAR_INT);
                for (int j = 0; j < tagsSize; j++) {
                    final String key = wrapper.read(Types.STRING);
                    final int[] ids = wrapper.read(Types.VAR_INT_ARRAY_PRIMITIVE);
                    registryAndTags.storeTags(key, ids);
                }
            } else {
                wrapper.write(Types.STRING, registryKey); // Write back
                final int tagsSize = wrapper.passthrough(Types.VAR_INT);
                for (int j = 0; j < tagsSize; j++) {
                    wrapper.passthrough(Types.STRING);
                    wrapper.passthrough(Types.VAR_INT_ARRAY_PRIMITIVE);
                }
            }
        }

        if (registryAndTags.tagsSent()) {
            wrapper.set(Types.VAR_INT, 0, length - 1); // Dialog tags have been read, remove from size
        }
    }

    private void clearDialog(final PacketWrapper wrapper) {
        wrapper.cancel();
        final DialogViewProvider provider = Via.getManager().getProviders().get(DialogViewProvider.class);
        provider.closeDialog(wrapper.user());
    }

    private void storeServerLinks(final PacketWrapper wrapper) {
        final ServerLinks serverLinks = new ServerLinks();
        final int length = wrapper.passthrough(Types.VAR_INT);
        for (int i = 0; i < length; i++) {
            if (wrapper.passthrough(Types.BOOLEAN)) {
                final int id = wrapper.passthrough(Types.VAR_INT);
                final String url = wrapper.passthrough(Types.STRING);
                serverLinks.storeLink(id, url);
            } else {
                final Tag tag = wrapper.passthrough(Types.TAG);
                final String url = wrapper.passthrough(Types.STRING);
                serverLinks.storeLink(tag, url);
            }
        }
        wrapper.user().put(serverLinks);
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_21_6.PLAYER));
        addItemHasher(user, new ItemHasherBase(this, user));
        user.put(new RegistryAndTags());
        user.put(new ClickEvents());
    }

    @Override
    public void register(final ViaProviders providers) {
        providers.register(DialogViewProvider.class, new ChestDialogViewProvider(this));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21_6 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21_6 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21_6> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket1_21_6> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_6> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V1_21_6;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21_5;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_6, ClientboundPacket1_21_5, ServerboundPacket1_21_6, ServerboundPacket1_21_5> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_6.class, ClientboundConfigurationPackets1_21_6.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_5.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_6.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_5.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}
