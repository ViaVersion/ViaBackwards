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
package com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v26_3to26_2.Protocol26_3To26_2;
import com.viaversion.viabackwards.protocol.v26_3to26_2.storage.ProtocolStorables26_3;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.ResolvableFloat;
import com.viaversion.viaversion.api.minecraft.ResolvableInt;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.BrewingFuel;
import com.viaversion.viaversion.api.minecraft.item.data.Compostable;
import com.viaversion.viaversion.api.minecraft.item.data.CookingFuel;
import com.viaversion.viaversion.api.minecraft.item.data.DeathProtection;
import com.viaversion.viaversion.api.minecraft.item.data.MobVisibility;
import com.viaversion.viaversion.api.minecraft.item.data.SignText;
import com.viaversion.viaversion.api.minecraft.item.data.SwingAnimation;
import com.viaversion.viaversion.api.minecraft.item.data.VillagerFood;
import com.viaversion.viaversion.api.minecraft.item.data.consumable.Consumable1_21_2;
import com.viaversion.viaversion.api.minecraft.item.data.consumable.ConsumeEffect;
import com.viaversion.viaversion.api.minecraft.item.data.consumable.TeleportRandomlyConsumeEffect;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.rewriter.text.NBTComponentRewriter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3.downgradeData;
import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3.upgradeData;

public final class BlockItemPacketRewriter26_3 extends BackwardsStructuredItemRewriter<ClientboundPacket26_3, ServerboundPacket26_1, Protocol26_3To26_2> {

    private static final int DEFAULT_RANDOMIZATION = 0;
    private static final int ALTERNATIVE_WITH_SPEED_RANDOMIZATION = 2;
    private static final int BREWING_STAND_MENU_TYPE = 11;
    private static final Holder<SoundEvent> SILENT_SOUND = Holder.of(new SoundEvent("intentionally_empty", null));

    public BlockItemPacketRewriter26_3(final Protocol26_3To26_2 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        protocol.registerClientbound(ClientboundPackets26_3.MAP_ITEM_DATA, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Map id
            wrapper.passthrough(Types.BYTE); // Scale
            wrapper.passthrough(Types.BOOLEAN); // Locked
            if (wrapper.passthrough(Types.BOOLEAN)) {
                final int icons = wrapper.passthrough(Types.VAR_INT);
                for (int i = 0; i < icons; i++) {
                    final int decorationType = wrapper.read(Types.VAR_INT);
                    wrapper.write(Types.VAR_INT, Math.min(decorationType, 34)); // Map new ones to trial chambers
                    wrapper.passthrough(Types.BYTE); // X
                    wrapper.passthrough(Types.BYTE); // Y
                    wrapper.passthrough(Types.BYTE); // Rotation
                    wrapper.passthrough(Types.TRUSTED_OPTIONAL_TAG); // Display name
                }
            }
        });

        protocol.replaceClientbound(ClientboundPackets26_3.LEVEL_PARTICLES, wrapper -> {
            final Particle particle = wrapper.read(protocol.getParticleRewriter().particleType());
            protocol.getParticleRewriter().rewriteParticle(wrapper.user(), particle);

            final boolean overrideLimiter = wrapper.read(Types.BOOLEAN);
            final boolean alwaysShow = wrapper.read(Types.BOOLEAN);
            final double x = wrapper.read(Types.DOUBLE);
            final double y = wrapper.read(Types.DOUBLE);
            final double z = wrapper.read(Types.DOUBLE);
            final float offsetX = wrapper.read(Types.FLOAT);
            final float offsetY = wrapper.read(Types.FLOAT);
            final float offsetZ = wrapper.read(Types.FLOAT);
            final float maxSpeedX = wrapper.read(Types.FLOAT);
            final float maxSpeedY = wrapper.read(Types.FLOAT);
            final float maxSpeedZ = wrapper.read(Types.FLOAT);
            final int count = wrapper.read(Types.VAR_INT);
            final int randomizationType = wrapper.read(Types.VAR_INT);

            final boolean singleSpeed = maxSpeedX == maxSpeedY && maxSpeedY == maxSpeedZ;
            if (count <= 0) {
                // When the count is 0, the client uses the offsets multiplied by the max speed as the particle's velocity,
                // or as extra data for particles like note or dust
                if (singleSpeed) {
                    writeParticle(wrapper, particle, overrideLimiter, alwaysShow, x, y, z, offsetX, offsetY, offsetZ, maxSpeedX, count);
                } else {
                    // Write max speed of 1 to keep values intact
                    writeParticle(
                        wrapper, particle, overrideLimiter, alwaysShow,
                        x, y, z,
                        (float) ((double) maxSpeedX * offsetX), (float) ((double) maxSpeedY * offsetY), (float) ((double) maxSpeedZ * offsetZ),
                        1, count
                    );
                }
                return;
            }

            if (randomizationType == DEFAULT_RANDOMIZATION && singleSpeed) { // Same as 26.2
                writeParticle(wrapper, particle, overrideLimiter, alwaysShow, x, y, z, offsetX, offsetY, offsetZ, maxSpeedX, count);
                return;
            }

            if (count > 3) {
                // Instead of spamming individual particle packets, spread them uniformly
                final float maxSpeed = Math.max(maxSpeedX, Math.max(maxSpeedY, maxSpeedZ));
                if (randomizationType == DEFAULT_RANDOMIZATION) {
                    writeParticle(wrapper, particle, overrideLimiter, alwaysShow, x, y, z, offsetX, offsetY, offsetZ, maxSpeed, count);
                } else {
                    writeParticle(
                        wrapper, particle, overrideLimiter, alwaysShow,
                        x + offsetX / 2, y + offsetY / 2, z + offsetZ / 2,
                        offsetX / 2, offsetY / 2, offsetZ / 2,
                        maxSpeed, count
                    );
                }
                return;
            }

            wrapper.cancel();

            // Do the randomization here and send the particles one by one with a count of 0 and exact positions
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                final double varianceX, varianceY, varianceZ;
                final double speedX, speedY, speedZ;
                if (randomizationType == DEFAULT_RANDOMIZATION) {
                    varianceX = random.nextGaussian() * offsetX;
                    varianceY = random.nextGaussian() * offsetY;
                    varianceZ = random.nextGaussian() * offsetZ;
                    speedX = random.nextGaussian() * maxSpeedX;
                    speedY = random.nextGaussian() * maxSpeedY;
                    speedZ = random.nextGaussian() * maxSpeedZ;
                } else {
                    varianceX = random.nextDouble() * offsetX;
                    varianceY = random.nextDouble() * offsetY;
                    varianceZ = random.nextDouble() * offsetZ;
                    final boolean randomizeSpeed = randomizationType == ALTERNATIVE_WITH_SPEED_RANDOMIZATION;
                    speedX = randomizeSpeed ? maxSpeedX * random.nextDouble() : maxSpeedX;
                    speedY = randomizeSpeed ? maxSpeedY * random.nextDouble() : maxSpeedY;
                    speedZ = randomizeSpeed ? maxSpeedZ * random.nextDouble() : maxSpeedZ;
                }

                final PacketWrapper particlePacket = wrapper.create(ClientboundPackets26_1.LEVEL_PARTICLES);
                writeParticle(particlePacket, particle, overrideLimiter, alwaysShow,
                    x + varianceX, y + varianceY, z + varianceZ,
                    (float) speedX, (float) speedY, (float) speedZ,
                    1, 0
                );
                particlePacket.send(Protocol26_3To26_2.class);
            }
        });

        protocol.appendClientbound(ClientboundPackets26_3.EXPLODE, wrapper -> {
            if (!wrapper.read(Types.BOOLEAN)) { // Play sound
                // 26.2 unconditionally plays the packet's sound, so mute it with an empty sound event
                wrapper.set(Types.SOUND_EVENT, 0, SILENT_SOUND);
            }
        });

        protocol.registerClientbound(ClientboundPackets26_3.OPEN_SIGN_EDITOR, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14);
            final boolean frontText = wrapper.read(Types.VAR_INT) == 1;
            wrapper.write(Types.BOOLEAN, frontText);
        });

        protocol.appendClientbound(ClientboundPackets26_3.OPEN_SCREEN, wrapper -> {
            wrapper.resetReader();

            final int containerId = wrapper.passthrough(Types.VAR_INT);
            final int menuType = wrapper.passthrough(Types.VAR_INT);
            final ProtocolStorables26_3 storables = wrapper.user().storables(protocol);
            storables.setBrewingStandContainerId(menuType == BREWING_STAND_MENU_TYPE ? containerId : -1);
        });

        protocol.registerClientbound(ClientboundPackets26_3.CONTAINER_SET_DATA, wrapper -> {
            final int containerId = wrapper.passthrough(Types.VAR_INT);
            final short slot = wrapper.passthrough(Types.SHORT);
            if (slot < 2) {
                return;
            }

            final ProtocolStorables26_3 storables = wrapper.user().storables(protocol);
            if (storables.brewingStandContainerId() == containerId) {
                // Remove total brewing time and total fuel
                wrapper.cancel();
            }
        });

        protocol.registerServerbound(ServerboundPackets26_1.SIGN_UPDATE, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14);

            final boolean frontText = wrapper.read(Types.BOOLEAN);

            for (int i = 0; i < 4; i++) {
                wrapper.passthrough(Types.STRING); // Line
            }

            wrapper.write(Types.VAR_INT, frontText ? 1 : 0);
        });

        protocol.registerClientbound(ClientboundPackets26_3.LIGHT_UPDATE, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // X
            wrapper.passthrough(Types.VAR_INT); // Y
            handleLightMasks(wrapper);
        });
        protocol.appendClientbound(ClientboundPackets26_3.LEVEL_CHUNK_WITH_LIGHT, this::handleLightMasks);

        protocol.replaceClientbound(ClientboundPackets26_3.UPDATE_ADVANCEMENTS, wrapper -> {
            int lastPositionIndex = 0; // Index of the x display data position, easier than keeping state while reading

            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            final int size = wrapper.passthrough(Types.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.STRING); // Identifier
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                final boolean hasDisplayData = wrapper.passthrough(Types.BOOLEAN);
                if (hasDisplayData) {
                    final Tag title = wrapper.passthrough(Types.TRUSTED_TAG);
                    final Tag description = wrapper.passthrough(Types.TRUSTED_TAG);
                    final NBTComponentRewriter<ClientboundPacket26_3> componentRewriter = protocol.getComponentRewriter();
                    componentRewriter.processTag(wrapper.user(), title);
                    componentRewriter.processTag(wrapper.user(), description);

                    passthroughClientboundItemTemplate(wrapper); // Icon
                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    final int flags = wrapper.passthrough(Types.INT); // Flags
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Types.STRING); // Background texture
                    }

                    // X and Y, set at the end of the loop
                    wrapper.write(Types.FLOAT, 0F);
                    wrapper.write(Types.FLOAT, 0F);
                }

                final int requirements = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY);
                }

                wrapper.passthrough(Types.BOOLEAN); // Send telemetry

                final float x = wrapper.read(Types.FLOAT);
                final float y = wrapper.read(Types.FLOAT);
                if (hasDisplayData) {
                    wrapper.set(Types.FLOAT, lastPositionIndex, x);
                    wrapper.set(Types.FLOAT, lastPositionIndex + 1, y);
                    lastPositionIndex += 2;
                }
            }
        });
    }

    private void writeParticle(final PacketWrapper wrapper, final Particle particle, final boolean overrideLimiter, final boolean alwaysShow,
                               final double x, final double y, final double z, final float offsetX, final float offsetY, final float offsetZ,
                               final float maxSpeed, final int count) {
        wrapper.write(Types.BOOLEAN, overrideLimiter);
        wrapper.write(Types.BOOLEAN, alwaysShow);
        wrapper.write(Types.DOUBLE, x);
        wrapper.write(Types.DOUBLE, y);
        wrapper.write(Types.DOUBLE, z);
        wrapper.write(Types.FLOAT, offsetX);
        wrapper.write(Types.FLOAT, offsetY);
        wrapper.write(Types.FLOAT, offsetZ);
        wrapper.write(Types.FLOAT, maxSpeed);
        wrapper.write(Types.INT, count);
        wrapper.write(protocol.getParticleRewriter().mappedParticleType(), particle);
    }

    private void handleLightMasks(final PacketWrapper wrapper) {
        for (int i = 0; i < 4; i++) {
            final BitSet mask = wrapper.read(Types.BIT_SET);
            wrapper.write(Types.LONG_ARRAY_PRIMITIVE, mask.toLongArray());
        }
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToClient(connection, item, container);
        downgradeData(container);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToServer(connection, item, container);
        upgradeData(container);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);
        if (!(customData.remove(nbtTagName("backup")) instanceof final CompoundTag backupTag)) {
            return;
        }

        restoreIntData(StructuredDataKey.PROVIDES_POTTERY_PATTERN, container, backupTag);
        restoreIntData(StructuredDataKey.BLOCK_TRANSFORMER, container, backupTag);
        restoreIntData(StructuredDataKey.CUSHION_COLOR, container, backupTag);

        if (backupTag.contains("waxed")) {
            container.set(StructuredDataKey.WAXED);
        }

        final IntTag villagerFood = backupTag.getIntTag("villager_food");
        if (villagerFood != null) {
            container.set(StructuredDataKey.VILLAGER_FOOD, new VillagerFood(villagerFood.asInt()));
        }

        final Tag compostable = backupTag.get("compostable");
        if (compostable != null) {
            container.set(StructuredDataKey.COMPOSTABLE, new Compostable(restoreResolvableInt(compostable)));
        }

        final CompoundTag cookingFuel = backupTag.getCompoundTag("cooking_fuel");
        if (cookingFuel != null) {
            container.set(StructuredDataKey.COOKING_FUEL, new CookingFuel(
                restoreResolvableInt(cookingFuel.get("burn_time")),
                restoreResolvableFloat(cookingFuel.get("speed_multiplier"))
            ));
        }

        final CompoundTag brewingFuel = backupTag.getCompoundTag("brewing_fuel");
        if (brewingFuel != null) {
            container.set(StructuredDataKey.BREWING_FUEL, new BrewingFuel(
                restoreResolvableInt(brewingFuel.get("uses")),
                restoreResolvableFloat(brewingFuel.get("speed_multiplier"))
            ));
        }

        final CompoundTag mobVisibility = backupTag.getCompoundTag("mob_visibility");
        if (mobVisibility != null) {
            container.set(StructuredDataKey.MOB_VISIBILITY, new MobVisibility(
                restoreHolderSet(mobVisibility, "targeting_entity_types"),
                mobVisibility.getFloat("visibility")
            ));
        }

        restoreSignText(StructuredDataKey.SIGN_TEXT_FRONT, container, backupTag, "sign_text_front");
        restoreSignText(StructuredDataKey.SIGN_TEXT_BACK, container, backupTag, "sign_text_back");

        final CompoundTag attackAnimation = backupTag.getCompoundTag("attack_animation");
        final CompoundTag interactAnimation = backupTag.getCompoundTag("interact_animation");
        if (attackAnimation != null || interactAnimation != null) {
            // Both were merged into the single swing animation, which upgradeData would otherwise copy back into both
            container.remove(StructuredDataKey.SWING_ANIMATION);
            if (attackAnimation != null) {
                container.set(StructuredDataKey.ATTACK_ANIMATION, restoreSwingAnimation(attackAnimation));
            }
            if (interactAnimation != null) {
                container.set(StructuredDataKey.INTERACT_ANIMATION, restoreSwingAnimation(interactAnimation));
            }
        }

        restoreMapDecorationTypes(container.get(StructuredDataKey.MAP_DECORATIONS), backupTag);

        final Consumable1_21_2 consumable = container.get(StructuredDataKey.CONSUMABLE1_21_2);
        if (consumable != null) {
            restoreDirectionalParticles(backupTag, "consumable_effects", consumable.consumeEffects());
        }

        final DeathProtection deathProtection = container.get(StructuredDataKey.DEATH_PROTECTION1_21_2);
        if (deathProtection != null) {
            restoreDirectionalParticles(backupTag, "death_protection_effects", deathProtection.deathEffects());
        }
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);

        saveIntData(StructuredDataKey.PROVIDES_POTTERY_PATTERN, dataContainer, backupTag);
        saveIntData(StructuredDataKey.BLOCK_TRANSFORMER, dataContainer, backupTag);
        saveIntData(StructuredDataKey.CUSHION_COLOR, dataContainer, backupTag);

        if (dataContainer.hasValue(StructuredDataKey.WAXED)) {
            backupTag.putBoolean("waxed", true);
        }

        final VillagerFood villagerFood = dataContainer.get(StructuredDataKey.VILLAGER_FOOD);
        if (villagerFood != null) {
            backupTag.putInt("villager_food", villagerFood.nutrition());
        }

        final Compostable compostable = dataContainer.get(StructuredDataKey.COMPOSTABLE);
        if (compostable != null) {
            backupTag.put("compostable", resolvableIntToTag(compostable.layers()));
        }

        final CookingFuel cookingFuel = dataContainer.get(StructuredDataKey.COOKING_FUEL);
        if (cookingFuel != null) {
            final CompoundTag fuelTag = new CompoundTag();
            fuelTag.put("burn_time", resolvableIntToTag(cookingFuel.burnTime()));
            fuelTag.put("speed_multiplier", resolvableFloatToTag(cookingFuel.speedMultiplier()));
            backupTag.put("cooking_fuel", fuelTag);
        }

        final BrewingFuel brewingFuel = dataContainer.get(StructuredDataKey.BREWING_FUEL);
        if (brewingFuel != null) {
            final CompoundTag fuelTag = new CompoundTag();
            fuelTag.put("uses", resolvableIntToTag(brewingFuel.uses()));
            fuelTag.put("speed_multiplier", resolvableFloatToTag(brewingFuel.speedMultiplier()));
            backupTag.put("brewing_fuel", fuelTag);
        }

        final MobVisibility mobVisibility = dataContainer.get(StructuredDataKey.MOB_VISIBILITY);
        if (mobVisibility != null) {
            final CompoundTag visibilityTag = new CompoundTag();
            visibilityTag.put("targeting_entity_types", holderSetToTag(mobVisibility.targetingEntityTypes()));
            visibilityTag.putFloat("visibility", mobVisibility.visibility());
            backupTag.put("mob_visibility", visibilityTag);
        }

        saveSignText(backupTag, "sign_text_front", dataContainer.get(StructuredDataKey.SIGN_TEXT_FRONT));
        saveSignText(backupTag, "sign_text_back", dataContainer.get(StructuredDataKey.SIGN_TEXT_BACK));

        saveSwingAnimation(backupTag, "attack_animation", dataContainer.get(StructuredDataKey.ATTACK_ANIMATION));
        saveSwingAnimation(backupTag, "interact_animation", dataContainer.get(StructuredDataKey.INTERACT_ANIMATION));

        saveMapDecorationTypes(backupTag, dataContainer.get(StructuredDataKey.MAP_DECORATIONS));

        final Consumable1_21_2 consumable = dataContainer.get(StructuredDataKey.CONSUMABLE26_3);
        if (consumable != null) {
            saveDirectionalParticles(backupTag, "consumable_effects", consumable.consumeEffects());
        }

        final DeathProtection deathProtection = dataContainer.get(StructuredDataKey.DEATH_PROTECTION26_3);
        if (deathProtection != null) {
            saveDirectionalParticles(backupTag, "death_protection_effects", deathProtection.deathEffects());
        }
    }

    private Tag resolvableIntToTag(final ResolvableInt value) {
        return value.isLeft() ? new IntTag(value.left()) : new StringTag(value.right());
    }

    private ResolvableInt restoreResolvableInt(@Nullable final Tag tag) {
        if (tag instanceof final StringTag stringTag) {
            return ResolvableInt.of(stringTag.getValue());
        }
        return ResolvableInt.of(tag instanceof final NumberTag numberTag ? numberTag.asInt() : 0);
    }

    private Tag resolvableFloatToTag(final ResolvableFloat value) {
        return value.isLeft() ? new FloatTag(value.left()) : new StringTag(value.right());
    }

    private ResolvableFloat restoreResolvableFloat(@Nullable final Tag tag) {
        if (tag instanceof final StringTag stringTag) {
            return ResolvableFloat.of(stringTag.getValue());
        }
        return ResolvableFloat.of(tag instanceof final NumberTag numberTag ? numberTag.asFloat() : 0F);
    }

    private void saveSignText(final CompoundTag backupTag, final String key, @Nullable final SignText signText) {
        if (signText == null) {
            return;
        }

        final CompoundTag tag = new CompoundTag();
        tag.put("messages", messagesToTag(signText.messages()));
        if (signText.filteredMessages() != null) {
            tag.put("filtered_messages", messagesToTag(signText.filteredMessages()));
        }
        tag.putInt("color", signText.color());
        tag.putBoolean("has_glowing_text", signText.hasGlowingText());
        backupTag.put(key, tag);
    }

    private void restoreSignText(final StructuredDataKey<SignText> key, final StructuredDataContainer container, final CompoundTag backupTag, final String name) {
        final CompoundTag tag = backupTag.getCompoundTag(name);
        final CompoundTag messages = tag != null ? tag.getCompoundTag("messages") : null;
        if (messages == null) {
            return;
        }

        final CompoundTag filteredMessages = tag.getCompoundTag("filtered_messages");
        container.set(key, new SignText(
            messagesFromTag(messages),
            filteredMessages != null ? messagesFromTag(filteredMessages) : null,
            tag.getInt("color"),
            tag.getBoolean("has_glowing_text")
        ));
    }

    private CompoundTag messagesToTag(final Tag[] messages) {
        // Components can be of different tag types, so they have to be stored indexed instead of in a list tag
        final CompoundTag tag = new CompoundTag();
        tag.putInt("length", messages.length);
        for (int i = 0; i < messages.length; i++) {
            if (messages[i] != null) {
                tag.put(Integer.toString(i), messages[i]);
            }
        }
        return tag;
    }

    private Tag[] messagesFromTag(final CompoundTag tag) {
        final Tag[] messages = new Tag[tag.getInt("length")];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = tag.get(Integer.toString(i));
        }
        return messages;
    }

    private void saveSwingAnimation(final CompoundTag backupTag, final String key, @Nullable final SwingAnimation animation) {
        if (animation == null) {
            return;
        }

        final CompoundTag tag = new CompoundTag();
        tag.putInt("type", animation.type());
        tag.putInt("duration", animation.duration());
        backupTag.put(key, tag);
    }

    private SwingAnimation restoreSwingAnimation(final CompoundTag tag) {
        return new SwingAnimation(tag.getInt("type"), tag.getInt("duration"));
    }

    private void saveMapDecorationTypes(final CompoundTag backupTag, @Nullable final CompoundTag mapDecorations) {
        if (mapDecorations == null) {
            return;
        }

        final CompoundTag types = new CompoundTag();
        for (final Map.Entry<String, Tag> entry : mapDecorations.entrySet()) {
            types.put(entry.getKey(), ((CompoundTag) entry.getValue()).getStringTag("type"));
        }
        backupTag.put("map_decorations", types);
    }

    private void restoreMapDecorationTypes(@Nullable final CompoundTag mapDecorations, final CompoundTag backupTag) {
        final CompoundTag types = backupTag.getCompoundTag("map_decorations");
        if (types == null || mapDecorations == null) {
            return;
        }

        for (final Map.Entry<String, Tag> entry : types.entrySet()) {
            final CompoundTag decorationTag = mapDecorations.getCompoundTag(entry.getKey());
            if (decorationTag != null) {
                decorationTag.put("type", entry.getValue());
            }
        }
    }

    private void saveDirectionalParticles(final CompoundTag backupTag, final String key, final ConsumeEffect<?>[] effects) {
        final int[] indexes = new int[effects.length];
        int size = 0;
        for (int i = 0; i < effects.length; i++) {
            if (effects[i].value() instanceof final TeleportRandomlyConsumeEffect effect && effect.directionalParticles()) {
                indexes[size++] = i;
            }
        }

        if (size != 0) {
            backupTag.put(key, new IntArrayTag(Arrays.copyOf(indexes, size)));
        }
    }

    private void restoreDirectionalParticles(final CompoundTag backupTag, final String key, final ConsumeEffect<?>[] effects) {
        final IntArrayTag tag = backupTag.getIntArrayTag(key);
        if (tag == null) {
            return;
        }

        for (final int index : tag.getValue()) {
            // Already written as the 26.3 value; upgradeData only touches effects still holding the plain diameter
            if (index >= 0 && index < effects.length && effects[index].value() instanceof final Float diameter) {
                effects[index] = new ConsumeEffect<>(effects[index].id(), ConsumeEffect.TELEPORT_RANDOMLY_TYPE26_3, new TeleportRandomlyConsumeEffect(diameter, true));
            }
        }
    }
}
