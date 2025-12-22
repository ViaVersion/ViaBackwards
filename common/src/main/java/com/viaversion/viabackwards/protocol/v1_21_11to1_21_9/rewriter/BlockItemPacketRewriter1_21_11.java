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
package com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.Protocol1_21_11To1_21_9;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.storage.GameTimeStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.AttackRange;
import com.viaversion.viaversion.api.minecraft.item.data.DamageType;
import com.viaversion.viaversion.api.minecraft.item.data.KineticWeapon;
import com.viaversion.viaversion.api.minecraft.item.data.PiercingWeapon;
import com.viaversion.viaversion.api.minecraft.item.data.SwingAnimation;
import com.viaversion.viaversion.api.minecraft.item.data.UseEffects;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPacket1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;
import com.viaversion.viaversion.util.Either;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.viaversion.viaversion.protocols.v1_21_9to1_21_11.rewriter.BlockItemPacketRewriter1_21_11.downgradeData;
import static com.viaversion.viaversion.protocols.v1_21_9to1_21_11.rewriter.BlockItemPacketRewriter1_21_11.upgradeData;

public final class BlockItemPacketRewriter1_21_11 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_11, ServerboundPacket1_21_9, Protocol1_21_11To1_21_9> {

    public BlockItemPacketRewriter1_21_11(final Protocol1_21_11To1_21_9 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21_11> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21_11.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21_11.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21_11.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_21_11.LEVEL_EVENT, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21_11.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_21_5::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21_11.BLOCK_ENTITY_DATA);

        registerSetCursorItem(ClientboundPackets1_21_11.SET_CURSOR_ITEM);
        registerSetPlayerInventory(ClientboundPackets1_21_11.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets1_21_11.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21_11.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21_11.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21_11.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21_11.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_11.MERCHANT_OFFERS);
        registerContainerClick1_21_5(ServerboundPackets1_21_6.CONTAINER_CLICK);
        registerSetCreativeModeSlot1_21_5(ServerboundPackets1_21_6.SET_CREATIVE_MODE_SLOT);

        final RecipeDisplayRewriter<ClientboundPacket1_21_11> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(protocol);
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_21_11.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_21_11.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_21_11.PLACE_GHOST_RECIPE);

        protocol.registerClientbound(ClientboundPackets1_21_11.SET_BORDER_LERP_SIZE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // oldSize
            wrapper.passthrough(Types.DOUBLE); // newSize
            wrapper.write(Types.VAR_LONG, wrapper.read(Types.VAR_LONG) * 50); // lerpTime
        });
        protocol.registerClientbound(ClientboundPackets1_21_11.INITIALIZE_BORDER, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // newCenterX
            wrapper.passthrough(Types.DOUBLE); // newCenterZ
            wrapper.passthrough(Types.DOUBLE); // oldSize
            wrapper.passthrough(Types.DOUBLE); // newSize
            wrapper.write(Types.VAR_LONG, wrapper.read(Types.VAR_LONG) * 50); // lerpTime
        });
        protocol.registerClientbound(ClientboundPackets1_21_11.SET_TIME, wrapper -> {
            final long gameTime = wrapper.passthrough(Types.LONG);
            wrapper.user().get(GameTimeStorage.class).setGameTime(gameTime);
        });
        protocol.registerServerbound(ServerboundPackets1_21_6.CLIENT_TICK_END, wrapper -> {
            wrapper.user().get(GameTimeStorage.class).incrementGameTime();
        });
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToClient(connection, item, container);
        downgradeData(item, container);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToServer(connection, item, container);
        upgradeData(item, container);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);

        if (!(customData.remove(nbtTagName("backup")) instanceof final CompoundTag backupTag)) {
            return;
        }

        final CompoundTag swingTag = backupTag.getCompoundTag("swing_animation");
        if (swingTag != null) {
            final int type = swingTag.getInt("type");
            final int duration = swingTag.getInt("duration");
            container.set(StructuredDataKey.SWING_ANIMATION, new SwingAnimation(type, duration));
        }

        final CompoundTag kineticTag = backupTag.getCompoundTag("kinetic_weapon");
        if (kineticTag != null) {
            final int contactCooldownTicks = kineticTag.getInt("contact_cooldown_ticks");
            final int delayTicks = kineticTag.getInt("delay_ticks");
            final KineticWeapon.Condition damageConditions = loadDamageCondition(kineticTag, "damage_conditions");
            final KineticWeapon.Condition dismountConditions = loadDamageCondition(kineticTag, "dismount_conditions");
            final KineticWeapon.Condition knockbackConditions = loadDamageCondition(kineticTag, "knockback_conditions");
            final float forwardMovement = kineticTag.getFloat("forward_movement");
            final float damageMultiplier = kineticTag.getFloat("damage_multiplier");
            final Holder<SoundEvent> sound = kineticTag.contains("sound") ? restoreSoundEventHolder(kineticTag, "sound") : null;
            final Holder<SoundEvent> hitSound = kineticTag.contains("hit_sound") ? restoreSoundEventHolder(kineticTag, "hit_sound") : null;
            container.set(StructuredDataKey.KINETIC_WEAPON, new KineticWeapon(contactCooldownTicks, delayTicks, damageConditions, dismountConditions, knockbackConditions, forwardMovement, damageMultiplier, sound, hitSound));
        }

        final CompoundTag piercingTag = backupTag.getCompoundTag("piercing_weapon");
        if (piercingTag != null) {
            final boolean dealsKnockback = piercingTag.getBoolean("deals_knockback");
            final boolean dismounts = piercingTag.getBoolean("dismounts");
            final Holder<SoundEvent> sound = piercingTag.contains("sound") ? restoreSoundEventHolder(piercingTag, "sound") : null;
            final Holder<SoundEvent> hitSound = piercingTag.contains("hit_sound") ? restoreSoundEventHolder(piercingTag, "hit_sound") : null;
            container.set(StructuredDataKey.PIERCING_WEAPON, new PiercingWeapon(dealsKnockback, dismounts, sound, hitSound));
        }

        final Tag damageTypeId = backupTag.get("damage_type_id");
        if (damageTypeId != null) {
            if (damageTypeId instanceof StringTag stringTag) {
                container.set(StructuredDataKey.DAMAGE_TYPE, new DamageType(Either.right(stringTag.getValue())));
            } else if (damageTypeId instanceof IntTag intTag) {
                container.set(StructuredDataKey.DAMAGE_TYPE, new DamageType(Either.left(intTag.asInt())));
            }
        }

        final CompoundTag useEffectsTag = backupTag.getCompoundTag("use_effects");
        if (useEffectsTag != null) {
            final boolean canSprint = useEffectsTag.getBoolean("can_sprint");
            final boolean interactVibrations = useEffectsTag.getBoolean("interact_vibrations");
            final float speedMultiplier = useEffectsTag.getFloat("speed_multiplier");
            container.set(StructuredDataKey.USE_EFFECTS, new UseEffects(canSprint, interactVibrations, speedMultiplier));
        }

        final CompoundTag attackRangeTag = backupTag.getCompoundTag("attack_range");
        if (attackRangeTag != null) {
            final float minRange = attackRangeTag.getFloat("min_range");
            final float maxRange = attackRangeTag.getFloat("max_range");
            final float minCreativeRange = attackRangeTag.getFloat("min_creative_reach");
            final float maxCreativeRange = attackRangeTag.getFloat("max_creative_reach");
            final float hitboxMargin = attackRangeTag.getFloat("hitbox_margin");
            final float mobFactor = attackRangeTag.getFloat("mob_factor");
            container.set(StructuredDataKey.ATTACK_RANGE, new AttackRange(minRange, maxRange, minCreativeRange, maxCreativeRange, hitboxMargin, mobFactor));
        }

        final Tag zombieNautilusVariantTag = backupTag.get("zombie_nautilus_variant");
        if (zombieNautilusVariantTag != null) {
            if (zombieNautilusVariantTag instanceof StringTag stringTag) {
                container.set(StructuredDataKey.ZOMBIE_NAUTILUS_VARIANT, Either.right(stringTag.getValue()));
            } else if (zombieNautilusVariantTag instanceof IntTag intTag) {
                container.set(StructuredDataKey.ZOMBIE_NAUTILUS_VARIANT, Either.left(intTag.asInt()));
            }
        }

        restoreFloatData(StructuredDataKey.MINIMUM_ATTACK_CHARGE, container, backupTag);
    }

    private KineticWeapon.Condition loadDamageCondition(final CompoundTag tag, final String key) {
        final CompoundTag conditionTag = tag.getCompoundTag(key);
        if (conditionTag == null) {
            return null;
        }

        final int maxDurationTicks = conditionTag.getInt("max_duration_ticks");
        final float minSpeed = conditionTag.getFloat("min_speed");
        final float minRelativeSpeed = conditionTag.getFloat("min_relative_speed");
        return new KineticWeapon.Condition(maxDurationTicks, minSpeed, minRelativeSpeed);
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);

        final SwingAnimation swingAnimation = dataContainer.get(StructuredDataKey.SWING_ANIMATION);
        if (swingAnimation != null) {
            final CompoundTag swingTag = new CompoundTag();
            swingTag.putInt("type", swingAnimation.type());
            swingTag.putInt("duration", swingAnimation.duration());
            backupTag.put("swing_animation", swingTag);
        }

        final KineticWeapon kineticWeapon = dataContainer.get(StructuredDataKey.KINETIC_WEAPON);
        if (kineticWeapon != null) {
            final CompoundTag kineticTag = new CompoundTag();
            kineticTag.putInt("contact_cooldown_ticks", kineticWeapon.contactCooldownTicks());
            kineticTag.putInt("delay_ticks", kineticWeapon.delayTicks());
            saveDamageCondition(kineticTag, "damage_conditions", kineticWeapon.damageConditions());
            saveDamageCondition(kineticTag, "dismount_conditions", kineticWeapon.dismountConditions());
            saveDamageCondition(kineticTag, "knockback_conditions", kineticWeapon.knockbackConditions());
            kineticTag.putFloat("forward_movement", kineticWeapon.forwardMovement());
            kineticTag.putFloat("damage_multiplier", kineticWeapon.damageMultiplier());
            if (kineticWeapon.sound() != null) {
                kineticTag.put("sound", holderToTag(kineticWeapon.sound(), this::saveSoundEvent));
            }
            if (kineticWeapon.hitSound() != null) {
                kineticTag.put("hit_sound", holderToTag(kineticWeapon.hitSound(), this::saveSoundEvent));
            }
            backupTag.put("kinetic_weapon", kineticTag);
        }

        final PiercingWeapon piercingWeapon = dataContainer.get(StructuredDataKey.PIERCING_WEAPON);
        if (piercingWeapon != null) {
            final CompoundTag piercingTag = new CompoundTag();
            piercingTag.putBoolean("deals_knockback", piercingWeapon.dealsKnockback());
            piercingTag.putBoolean("dismounts", piercingWeapon.dismounts());
            if (piercingWeapon.sound() != null) {
                piercingTag.put("sound", holderToTag(piercingWeapon.sound(), this::saveSoundEvent));
            }
            if (piercingWeapon.hitSound() != null) {
                piercingTag.put("hit_sound", holderToTag(piercingWeapon.hitSound(), this::saveSoundEvent));
            }
            backupTag.put("piercing_weapon", piercingTag);
        }

        final DamageType damageType = dataContainer.get(StructuredDataKey.DAMAGE_TYPE);
        if (damageType != null) {
            if (damageType.id().isLeft()) {
                backupTag.putInt("damage_type_id", damageType.id().left());
            } else {
                backupTag.putString("damage_type_id", damageType.id().right());
            }
        }

        final UseEffects useEffects = dataContainer.get(StructuredDataKey.USE_EFFECTS);
        if (useEffects != null) {
            final CompoundTag useEffectsTag = new CompoundTag();
            useEffectsTag.putBoolean("can_sprint", useEffects.canSprint());
            useEffectsTag.putBoolean("interact_vibrations", useEffects.interactVibrations());
            useEffectsTag.putFloat("speed_multiplier", useEffects.speedMultiplier());
            backupTag.put("use_effects", useEffectsTag);
        }

        final AttackRange attackRange = dataContainer.get(StructuredDataKey.ATTACK_RANGE);
        if (attackRange != null) {
            final CompoundTag attackRangeTag = new CompoundTag();
            attackRangeTag.putFloat("min_range", attackRange.minRange());
            attackRangeTag.putFloat("max_range", attackRange.maxRange());
            attackRangeTag.putFloat("min_creative_reach", attackRange.minCreativeRange());
            attackRangeTag.putFloat("max_creative_reach", attackRange.maxCreativeRange());
            attackRangeTag.putFloat("hitbox_margin", attackRange.hitboxMargin());
            attackRangeTag.putFloat("mob_factor", attackRange.mobFactor());
            backupTag.put("attack_range", attackRangeTag);
        }

        final Either<Integer, String> zombieNautilusVariant = dataContainer.get(StructuredDataKey.ZOMBIE_NAUTILUS_VARIANT);
        if (zombieNautilusVariant != null) {
            if (zombieNautilusVariant.isLeft()) {
                backupTag.putInt("zombie_nautilus_variant", zombieNautilusVariant.left());
            } else {
                backupTag.putString("zombie_nautilus_variant", zombieNautilusVariant.right());
            }
        }

        saveFloatData(StructuredDataKey.MINIMUM_ATTACK_CHARGE, dataContainer, backupTag);
    }

    private void saveDamageCondition(final CompoundTag tag, final String key, final KineticWeapon.@Nullable Condition condition) {
        if (condition == null) {
            return;
        }

        final CompoundTag conditionTag = new CompoundTag();
        conditionTag.putInt("max_duration_ticks", condition.maxDurationTicks());
        conditionTag.putFloat("min_speed", condition.minSpeed());
        conditionTag.putFloat("min_relative_speed", condition.minRelativeSpeed());
        tag.put(key, conditionTag);
    }
}
