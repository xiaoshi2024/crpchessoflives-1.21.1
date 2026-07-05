package com.xiaoshi2022.crpchessoflives.clone.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.xiaoshi2022.crpchessoflives.Config;
import com.xiaoshi2022.crpchessoflives.clone.packet.c2s.TransferPlayerPacket;
import com.xiaoshi2022.crpchessoflives.clone.packet.s2c.AfterDeathPacket;
import com.xiaoshi2022.crpchessoflives.clone.packet.s2c.FinishedSyncPacket;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellSavedData;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellState;
import com.xiaoshi2022.crpchessoflives.clone.utils.PositionReference;
import com.xiaoshi2022.crpchessoflives.clone.utils.ShellPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.Team.Visibility;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static com.xiaoshi2022.crpchessoflives.clone.blocks.AbstractMultiblockBlock.FACING;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.EMPTY_UUID;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.findTargetLevel;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends Player implements ShellPlayer {
    @Shadow
    @Final
    public MinecraftServer server;
    @Shadow
    public ServerGamePacketListenerImpl connection;
    @Shadow
    @Final
    public ServerPlayerGameMode gameMode;

    @Unique
    @Nullable
    private GameProfile playershells$originalProfile;
    @Unique
    @Nullable
    private UUID playershells$disguiseOwnerUuid;

    public MixinServerPlayer(final Level level, final BlockPos pos, final float rotY, final GameProfile gameProfile) {
        super(level, pos, rotY, gameProfile);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void die(final DamageSource cause, final CallbackInfo ci) {
        if (!Config.TRANSFER_INTO_SHELL_AFTER_DEATH.get()) {
            return;
        }

        this.playershells$restoreGameProfile();

        final ShellState shellState = ShellSavedData.getShellData(this.serverLevel())
                .getNearestActive(this.getUUID(), this.level().dimension().location(), this.blockPosition());
        if (shellState == null) {
            return;
        }

        this.playershells$sendDeathMessageInChat();
        this.removeEntitiesOnShoulder();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }

        if (!this.isSpectator()) {
            this.dropAllDeathLoot(this.serverLevel(), cause);
        }

        this.dead = false;
        this.setHealth(0.01F);
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);

        if (this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            this.playershells$dropItemsAndExp(shellState.shellForgePos());
        }

        final ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        final ShellPlayer shellPlayer = (ShellPlayer) serverPlayer;

        shellPlayer.playershells$applyData(shellState.playerData(), shellState.shellForgePos());

        final UUID selfUuid = this.getUUID();
        final UUID shellOwnerUuid = (!EMPTY_UUID.equals(shellState.ownerUuid())) ? shellState.ownerUuid() : selfUuid;

        if (!EMPTY_UUID.equals(shellOwnerUuid) && !selfUuid.equals(shellOwnerUuid)) {
            final GameProfile ownerProfile = playershells$resolveOwnerProfile(this.serverLevel(), shellOwnerUuid, shellState);
            if (ownerProfile != null) {
                shellPlayer.playershells$applyGameProfile(ownerProfile);
            }
        } else {
            shellPlayer.playershells$restoreGameProfile();
        }

        final ServerLevel forgeLevel = findTargetLevel(this.server, shellState.shellForgePos());
        if (forgeLevel != null && forgeLevel.getBlockEntity(shellState.shellForgePos().pos()) instanceof com.xiaoshi2022.crpchessoflives.clone.blockentities.ShellForgeBlockEntity forge) {
            forge.exterminateShell();
        }
        ShellSavedData.getShellData(this.serverLevel()).validateShellData(this.serverLevel());

        PacketDistributor.sendToPlayer(serverPlayer, new AfterDeathPacket(shellState.shellForgePos()));
        ci.cancel();
    }

    @Override
    public void playershells$applyData(final CompoundTag tag, final PositionReference posReference) {
        if (this.getServer() == null) {
            return;
        }
        final ServerLevel targetLevel = findTargetLevel(this.getServer(), posReference);
        if (targetLevel == null) {
            return;
        }

        final BlockPos pos = posReference.pos().immutable();
        final LevelChunk chunk = targetLevel.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.06250;
        final double z = pos.getZ() + 0.5;
        final BlockState state = chunk.getBlockState(pos);
        final float yaw = state.hasProperty(FACING) ? state.getValue(FACING).toYRot() : 0f;

        this.teleportTo(targetLevel, x, y, z, yaw, 0);

        this.removeAllEffects();
        this.load(tag);
        this.loadGameTypes(tag);

        final ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        final PlayerList playerList = this.server.getPlayerList();

        this.onUpdateAbilities();
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(Action.UPDATE_GAME_MODE, serverPlayer));
        this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float) this.gameMode.getGameModeForPlayer().getId()));
        this.getStats().markAllDirty();
        this.updateEffectVisibility();
        if (this.level() == targetLevel) {
            playerList.sendActivePlayerEffects(serverPlayer);
            playerList.sendAllPlayerInfo(serverPlayer);
            EventHooks.firePlayerRespawnEvent(serverPlayer, targetLevel.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY));
        }

        PacketDistributor.sendToPlayer(serverPlayer, new FinishedSyncPacket());
    }

    @Override
    public CompoundTag playershells$getData() {
        final CompoundTag tag = new CompoundTag();

        this.saveWithoutId(tag);
        this.storeGameTypes(tag);

        return tag;
    }

    @Unique
    @Nullable
    private static final sun.misc.Unsafe PLAYERSHELLS$UNSAFE;
    @Unique
    private static final long PLAYERSHELLS$GAME_PROFILE_OFFSET;

    static {
        sun.misc.Unsafe unsafe = null;
        long offset = -1L;
        try {
            final Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (sun.misc.Unsafe) unsafeField.get(null);

            final Field gameProfileField = Player.class.getDeclaredField("gameProfile");
            offset = unsafe.objectFieldOffset(gameProfileField);
        } catch (final Throwable ignore) {
            unsafe = null;
            offset = -1L;
        }
        PLAYERSHELLS$UNSAFE = unsafe;
        PLAYERSHELLS$GAME_PROFILE_OFFSET = offset;
    }

    @Unique
    private void playershells$setGameProfileField(final GameProfile profile) {
        if (PLAYERSHELLS$UNSAFE == null || PLAYERSHELLS$GAME_PROFILE_OFFSET < 0L) return;
        PLAYERSHELLS$UNSAFE.putObject(this, PLAYERSHELLS$GAME_PROFILE_OFFSET, profile);
    }

    @Unique
    private void playershells$rebindTabVisibility() {
        final ServerPlayer self = (ServerPlayer) (Object) this;
        final PlayerList playerList = this.server.getPlayerList();

        playerList.broadcastAll(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(self.getUUID())));

        final Collection<ServerPlayer> selfAsList = Collections.singletonList(self);
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(Action.ADD_PLAYER, Action.INITIALIZE_CHAT, Action.UPDATE_GAME_MODE, Action.UPDATE_LISTED, Action.UPDATE_LATENCY, Action.UPDATE_DISPLAY_NAME),
                selfAsList
        ));
        playerList.sendPlayerPermissionLevel(self);

        playershells$forceResyncEntity(self);
    }

    @Unique
    private void playershells$forceResyncEntity(final ServerPlayer self) {
        final ServerLevel serverLevel = self.serverLevel();

        final int selfId = self.getId();
        final var removePacket = new ClientboundRemoveEntitiesPacket(selfId);
        final Vec3 vel = self.getDeltaMovement();
        final var addEntityPacket = new ClientboundAddEntityPacket(
                selfId,
                self.getUUID(),
                self.getX(), self.getY(), self.getZ(),
                self.getXRot(), self.getYRot(),
                self.getType(),
                0,
                vel,
                self.getYHeadRot()
        );
        final var teleportPacket = new ClientboundTeleportEntityPacket(self);
        final var rotateHeadPacket = new ClientboundRotateHeadPacket(self, (byte) (self.getYRot() * 256.0F / 360.0F));

        final List<ServerPlayer> trackingPlayers = serverLevel.getChunkSource().chunkMap.getPlayers(self.chunkPosition(), false);
        for (final ServerPlayer other : trackingPlayers) {
            if (other == self) {
                continue;
            }
            other.connection.send(removePacket);
            other.connection.send(addEntityPacket);
            other.connection.send(teleportPacket);
            other.connection.send(rotateHeadPacket);
        }
    }

    @Override
    public void playershells$applyGameProfile(@Nullable final GameProfile targetOwnerProfile) {
        if (targetOwnerProfile == null) {
            this.playershells$restoreGameProfile();
            return;
        }
        if (this.playershells$originalProfile == null) {
            this.playershells$originalProfile = this.getGameProfile();
        }
        final GameProfile original = this.playershells$originalProfile;
        final GameProfile disguised = new GameProfile(original.getId(), targetOwnerProfile.getName() != null
                ? targetOwnerProfile.getName()
                : original.getName());
        final Collection<Property> textureProps = targetOwnerProfile.getProperties().get("textures");
        if (textureProps != null) {
            for (final Property prop : textureProps) {
                disguised.getProperties().put("textures", prop);
            }
        }
        this.playershells$disguiseOwnerUuid = targetOwnerProfile.getId();
        this.playershells$setGameProfileField(disguised);
        this.playershells$rebindTabVisibility();
    }

    @Override
    public void playershells$restoreGameProfile() {
        if (this.playershells$originalProfile == null) {
            return;
        }
        this.playershells$setGameProfileField(this.playershells$originalProfile);
        this.playershells$disguiseOwnerUuid = null;
        this.playershells$originalProfile = null;
        this.playershells$rebindTabVisibility();
    }

    @Nullable
    @Override
    public UUID playershells$getDisguiseOwnerUuid() {
        return this.playershells$disguiseOwnerUuid;
    }

    @Unique
    @Nullable
    private static GameProfile playershells$resolveOwnerProfile(final ServerLevel serverLevel, final UUID ownerUuid, final ShellState shellState) {
        if (serverLevel.getServer() == null) return null;

        final ServerPlayer onlineOwner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        if (onlineOwner != null) {
            return onlineOwner.getGameProfile();
        }

        String ownerName = null;
        final CompoundTag playerData = shellState.playerData();
        if (playerData != null) {
            if (playerData.contains("CustomName", Tag.TAG_STRING)) {
                try {
                    final String json = playerData.getString("CustomName");
                    final Component component = Component.Serializer.fromJson(json, serverLevel.registryAccess());
                    if (component != null) {
                        ownerName = component.getString();
                    }
                } catch (final Exception ignore) {
                }
            }
            if (ownerName == null && playerData.contains("ScoreboardName", Tag.TAG_STRING)) {
                ownerName = playerData.getString("ScoreboardName");
            }
        }
        if (ownerName == null) {
            ownerName = "Cloned_" + ownerUuid.toString().substring(0, 8);
        }
        return new GameProfile(ownerUuid, ownerName);
    }

    @Unique
    private void playershells$sendDeathMessageInChat() {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
            final Component deathMessage = this.getCombatTracker().getDeathMessage();
            final Component transferredMessage = Component.translatable("death.crpchessoflives.transferred_to_shell_" + this.random.nextInt(0, 4));
            final Component betterDeathMessage = Component.literal(deathMessage.getString() + ". " + transferredMessage.getString());
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), betterDeathMessage), PacketSendListener.exceptionallySend(() -> {
                final Component component1 = Component.translatable("death.attack.message_too_long", Component.literal(betterDeathMessage.getString(256))
                        .withStyle(ChatFormatting.YELLOW));
                final Component component2 = Component.translatable("death.attack.even_more_magic", this.getDisplayName()).withStyle((style) -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1)));
                return new ClientboundPlayerCombatKillPacket(this.getId(), component2);
            }));
            final Team team = this.getTeam();
            if (team != null && team.getDeathMessageVisibility() != Visibility.ALWAYS) {
                if (team.getDeathMessageVisibility() == Visibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastSystemToTeam(this, betterDeathMessage);
                } else if (team.getDeathMessageVisibility() == Visibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, betterDeathMessage);
                }
            } else {
                this.server.getPlayerList().broadcastSystemMessage(betterDeathMessage, false);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), CommonComponents.EMPTY));
        }
    }

    @Unique
    private void playershells$dropItemsAndExp(final PositionReference posReference) {
        final ServerLevel targetLevel = findTargetLevel(this.server, posReference);
        if (targetLevel == null) {
            return;
        }

        final BlockPos pos = posReference.pos();

        for (int i = 0; i < this.getInventory().getContainerSize(); ++i) {
            final ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                final ItemEntity itementity = new ItemEntity(this.level(), pos.getX(), pos.getY(), pos.getZ(), stack);
                targetLevel.addFreshEntity(itementity);
            }
        }

        ExperienceOrb.award((ServerLevel) this.level(), pos.getCenter(), this.totalExperience);
    }

    @Shadow
    public abstract void teleportTo(ServerLevel newLevel, double x, double y, double z, float yaw, float pitch);

    @Shadow
    protected abstract void tellNeutralMobsThatIDied();

    @Shadow
    public abstract ServerLevel serverLevel();

    @Shadow
    public abstract ServerStatsCounter getStats();

    @Shadow
    public abstract void loadGameTypes(@Nullable CompoundTag tag);

    @Shadow
    public abstract void storeGameTypes(CompoundTag tag);
}
