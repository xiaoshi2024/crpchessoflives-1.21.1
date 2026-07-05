package com.xiaoshi2022.crpchessoflives.clone.blockentities;

import com.mojang.authlib.GameProfile;
import com.xiaoshi2022.crpchessoflives.Config;
import com.xiaoshi2022.crpchessoflives.clone.blocks.ShellForgeBlock.*;
import com.xiaoshi2022.crpchessoflives.clone.container.ShellForgeContainerMenu;
import com.xiaoshi2022.crpchessoflives.clone.registry.*;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellSavedData;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellState;
import com.xiaoshi2022.crpchessoflives.clone.utils.ObservableEnergyStorage;
import com.xiaoshi2022.crpchessoflives.clone.utils.PositionReference;
import com.xiaoshi2022.crpchessoflives.clone.utils.ShellPlayer;
import com.xiaoshi2022.crpchessoflives.clone.utils.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

import static com.xiaoshi2022.crpchessoflives.Config.SHELL_FORGE_DNA_AMOUNT;
import static com.xiaoshi2022.crpchessoflives.clone.blocks.AbstractMultiblockBlock.FACING;
import static com.xiaoshi2022.crpchessoflives.clone.blocks.ShellForgeBlock.*;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.*;

public class ShellForgeBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    private static final float OPEN_SPEED = 0.12f;

    public final ItemStackHandler inventoryHandler = new ItemStackHandler(1) {
        @Override
        public void onContentsChanged(final int slot) {
            ShellForgeBlockEntity.super.setChanged();
        }
    };
    public final ObservableEnergyStorage energyStorage = new ObservableEnergyStorage(Config.SHELL_FORGE_ENERGY_CAPACITY.get()) {
        @Override
        public void onEnergyChanged() {
            ShellForgeBlockEntity.this.setChanged();
        }
    };

    private UUID playerUuid = EMPTY_UUID;
    private PlayerStates playerState = PlayerStates.NONE;
    private UUID shellUuid = EMPTY_UUID;
    private ShellStates shellState = ShellStates.CREATE;
    private int shellPercentage;
    private int shellPercentageCooldownTick;

    private float animProgress;
    private float animPrevProgress;

    private int lastRedstoneLevel = 0;
    private int maintenanceCounter = 0;

    public ShellForgeBlockEntity(final BlockPos pos,
                                 final BlockState blockState) {
        super(ModBlockEntityTypes.SHELL_FORGE.get(), pos, blockState);
    }

    public int getRedstoneLevel() {
        return this.lastRedstoneLevel;
    }

    public void refreshRedstoneSignal() {
        if (this.level == null) return;
        int signal = 0;
        final BlockPos bottomPos = this.getBlockPos();
        final BlockPos topPos = bottomPos.above();
        for (final Direction dir : Direction.values()) {
            signal = Math.max(signal, this.level.getSignal(bottomPos.relative(dir), dir));
            signal = Math.max(signal, this.level.getSignal(topPos.relative(dir), dir));
        }
        signal = Math.min(signal, 15);
        if (signal != this.lastRedstoneLevel) {
            this.lastRedstoneLevel = signal;
            this.setChanged();
        }
    }

    private boolean canRedstoneOperate() {
        if (!Config.REDSTONE_SIGNAL_REQUIRED.get()) return true;
        return this.lastRedstoneLevel >= Config.REDSTONE_MIN_ACTIVATION_LEVEL.get();
    }

    private void chargeEnergyFromRedstone() {
        if (!Config.REDSTONE_POWER_ENABLED.get()) return;
        if (this.lastRedstoneLevel <= 0) return;
        final int fePerTick = this.lastRedstoneLevel * Config.REDSTONE_ENERGY_PER_LEVEL_PER_TICK.get();
        if (fePerTick > 0) {
            this.energyStorage.receiveEnergy(fePerTick, false);
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ShellForgeBlockEntity blockEntity) {
        blockEntity.refreshRedstoneSignal();
        blockEntity.chargeEnergyFromRedstone();
        blockEntity.updatePlayerStateAndOpened(state, pos);

        blockEntity.drainEnergyPassive();

        final boolean redstoneOk = blockEntity.canRedstoneOperate();

        // Decay/Exterminating shell stuff
        if (blockEntity.shellState == ShellStates.DECAYING || blockEntity.shellState == ShellStates.EXTERMINATING) {
            if (blockEntity.shellPercentage <= 0) {
                blockEntity.exterminateShell();
                return;
            }
            if (blockEntity.shellState == ShellStates.DECAYING && blockEntity.shellPercentageCooldownTick++ <= Config.SHELL_FORGE_DECAY_COOLDOWN.get()) {
                blockEntity.setChanged();
                return;
            }
            blockEntity.shellPercentageCooldownTick = 0;
            blockEntity.shellPercentage -= 1;
            blockEntity.updateShellCreationProgressOnClient();
            blockEntity.setChanged();
            return;
        }

        // Create shell stuff - requires redstone activation when configured
        if (blockEntity.shellState != ShellStates.CREATING) {
            return;
        }
        if (!redstoneOk) {
            blockEntity.setChanged();
            return;
        }
        if (blockEntity.shellPercentage == 100) {
            blockEntity.shellState = ShellStates.EXTERMINATE;
            blockEntity.setChanged();
            return;
        }

        if (blockEntity.shellPercentageCooldownTick++ <= Config.SHELL_FORGE_CREATION_COOLDOWN.get()) {
            blockEntity.setChanged();
            return;
        }

        blockEntity.shellPercentageCooldownTick = 0;
        blockEntity.shellPercentage += 1;
        blockEntity.updateShellCreationProgressOnClient();
        blockEntity.setChanged();
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final ShellForgeBlockEntity blockEntity) {
        blockEntity.animPrevProgress = blockEntity.animProgress;
        final float target = state.getValue(OPEN) ? 1.0f : 0.0f;

        if (blockEntity.animProgress < target) {
            blockEntity.animProgress = Math.min(target, blockEntity.animProgress + OPEN_SPEED);
        } else if (blockEntity.animProgress > target) {
            blockEntity.animProgress = Math.max(target, blockEntity.animProgress - OPEN_SPEED);
        }

        if (blockEntity.shellState == ShellStates.EXTERMINATING) {
            if (level.random.nextDouble() < 0.1) {
                SoundHandler.startBlockSound(ModSoundEvents.FLAMETHROWER.get(), SoundSource.BLOCKS, 1.5F, 1.0F, level.random, pos);
            }
        } else {
            SoundHandler.stopAllBlockSounds(pos);
        }

        final Player player = Minecraft.getInstance().player;
        if (player == null || !state.getValue(OPEN)) {
            return;
        }

        if (blockEntity.level != null && !isPlayerInFront(pos, blockEntity.level, player.getUUID(), state.getValue(FACING))) {
            return;
        }
        if (blockEntity.playerState == PlayerStates.GOING_IN || blockEntity.playerState == PlayerStates.INSIDE) {
            movePlayerInside(player, pos, state.getValue(FACING));
        } else if (blockEntity.playerState == PlayerStates.GOING_OUT) {
            movePlayerTo(player, pos.relative(state.getValue(FACING)), 0);
        }
    }

    private void drainEnergyPassive() { //TODO: decying doesn't stop
        if (this.shellState == ShellStates.EXTERMINATE) {
            final int simulateExtract = this.energyStorage.extractEnergy(Config.SHELL_FORGE_ENERGY_USAGE_MAINTENANCE.get(), true);
            if (simulateExtract != Config.SHELL_FORGE_ENERGY_USAGE_MAINTENANCE.get()) {
                this.shellState = ShellStates.DECAYING;
                this.setChanged();
            } else {
                this.energyStorage.extractEnergy(Config.SHELL_FORGE_ENERGY_USAGE_MAINTENANCE.get(), false);
                this.setChanged();
            }
        }
    }

    private void updateShellCreationProgressOnClient() {
        if (this.level instanceof ServerLevel serverLevel) {
            ShellSavedData.getShellData(serverLevel).updateShellCreationProgress(this.playerUuid, this.shellUuid, this.shellPercentage);
        }
    }

    public void transferPlayerTo(final Player player) {
        //TODO decrease water level, remove mask on client
        if (this.level instanceof ServerLevel serverLevel && player instanceof ShellPlayer shellPlayer) {
            final ShellState shellState = ShellSavedData.getShellData(serverLevel).get(this.playerUuid, this.shellUuid);
            if (shellState != null) {
                shellPlayer.playershells$applyData(shellState.playerData(), shellState.shellForgePos());

                final UUID enteringPlayerUuid = player.getUUID();
                final UUID shellOwnerUuid = (!EMPTY_UUID.equals(shellState.ownerUuid())) ? shellState.ownerUuid() : this.playerUuid;

                if (!EMPTY_UUID.equals(shellOwnerUuid) && !enteringPlayerUuid.equals(shellOwnerUuid)) {
                    final GameProfile ownerProfile = resolveOwnerProfile(serverLevel, shellOwnerUuid, shellState);
                    if (ownerProfile != null) {
                        shellPlayer.playershells$applyGameProfile(ownerProfile);
                    }
                } else {
                    shellPlayer.playershells$restoreGameProfile();
                }

                this.playerState = PlayerStates.TRANSFERRED;
                this.exterminateShell();

                setValue(this.getBlockState(), this.level, this.getBlockPos(), new BoolProperty(OPEN, true));
            }
        }
    }

    @Nullable
    private GameProfile resolveOwnerProfile(final ServerLevel serverLevel, final UUID ownerUuid, final ShellState shellState) {
        if (serverLevel.getServer() == null) return null;

        // 1) 优先在线玩家，直接拿到完整的 GameProfile（含 textures 皮肤）
        final ServerPlayer onlineOwner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        if (onlineOwner != null) {
            return onlineOwner.getGameProfile();
        }

        // 2) 离线玩家：尝试从 shellState.playerData 的 CustomName/SaveId 中构造基础 GameProfile
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

    public void transferPlayerFrom(final Player player) {
        if (player instanceof ShellPlayer shellPlayer) {
            shellPlayer.playershells$restoreGameProfile();
        }
        final UUID owner = player.getUUID();
        this.setPlayerUuid(owner);
        this.shellState = ShellStates.EXTERMINATE;
        this.playerState = PlayerStates.NONE;
        this.shellPercentage = 100;
        if (this.level instanceof ServerLevel serverLevel && player instanceof ShellPlayer shellPlayer) {
            this.shellUuid = UUID.randomUUID();
            ShellSavedData.getShellData(serverLevel).add(this.playerUuid, new ShellState(
                    this.shellUuid,
                    owner,
                    new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), this.level.dimension().location()),
                    shellPlayer.playershells$getData(),
                    100));
        }
        this.setChanged();
    }

    public void createShell() {
        if (!this.canCreateShell()) {
            return;
        }

        final UUID playerUUID = Objects.requireNonNull(this.inventoryHandler.getStackInSlot(0).get(ModDataComponentTypes.OWNER_PLAYER.get())).playerUUID();

        this.energyStorage.extractEnergy(Config.SHELL_FORGE_ENERGY_USAGE_CREATION.get(), false);
        this.inventoryHandler.extractItem(0, SHELL_FORGE_DNA_AMOUNT.get(), false);
        this.setPlayerUuid(playerUUID);
        this.shellState = ShellStates.CREATING;
        if (this.level instanceof ServerLevel serverLevel) {
            this.shellUuid = UUID.randomUUID();
            ShellSavedData.getShellData(serverLevel).add(this.playerUuid, new ShellState(
                    this.shellUuid,
                    playerUUID,
                    new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), this.level.dimension().location()),
                    this.createFreshPlayerData(serverLevel), 0));
        }
        this.setChanged();
    }

    public void exterminateShell() {
        this.setPlayerUuid(EMPTY_UUID);
        this.shellPercentage = 0;
        this.shellState = ShellStates.CREATE;
        this.shellUuid = EMPTY_UUID;
        if (this.level instanceof ServerLevel serverLevel) {
            ShellSavedData.getShellData(serverLevel).validateShellData(serverLevel);
        }
        this.setChanged();
    }

    private CompoundTag createFreshPlayerData(final ServerLevel serverLevel) {
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), ""), false);
        return new ServerPlayer(serverLevel.getServer(), serverLevel, cookie.gameProfile(), cookie.clientInformation())
                .saveWithoutId(new CompoundTag());
    }

    public boolean canCreateShell() {
        if (!this.canRedstoneOperate()) {
            return false;
        }
        final int energyNeeded = Config.SHELL_FORGE_ENERGY_USAGE_CREATION.get();

        final ItemStack stack = this.inventoryHandler.getStackInSlot(0);
        if (stack.isEmpty() || !stack.is(ModItems.DNA) || !stack.has(ModDataComponentTypes.OWNER_PLAYER.get())) {
            return false;
        }

        final boolean canExtractStack = this.inventoryHandler.extractItem(0, SHELL_FORGE_DNA_AMOUNT.get(), true).getCount() >= SHELL_FORGE_DNA_AMOUNT.get();
        if (!canExtractStack) {
            return false;
        }

        final int simulatedExtract = this.energyStorage.extractEnergy(energyNeeded, true);
        return simulatedExtract >= energyNeeded;
    }

    private void updatePlayerStateAndOpened(final BlockState state, final BlockPos pos) {
        if (this.level == null) {
            return;
        }

        if (!isOpen(state) && (this.playerState == PlayerStates.GOING_IN || this.playerState == PlayerStates.TRANSFERRED)) {
            setValue(state, this.level, pos, new BoolProperty(OPEN, true));
        } else if (this.playerState == PlayerStates.GOING_IN) {
            if (hasPlayerInside(pos, this.level)) {
                this.playerState = PlayerStates.INSIDE;
                this.setChanged();
            }
        } else if (!hasPlayerInside(pos, this.level)) {
            this.playerState = PlayerStates.NONE;
            this.setChanged();
            setValue(state, this.level, pos, new BoolProperty(OPEN, false));
        }
    }

    public void shellButtonPressed() {
        if (this.shellState == ShellStates.CREATE) {
            this.createShell();
        } else if (this.shellState == ShellStates.EXTERMINATE) {
            this.shellState = ShellStates.EXTERMINATING;
            this.setChanged();
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("inv")) {
            this.inventoryHandler.deserializeNBT(registries, tag.getCompound("inv"));
        }
        if (tag.contains("energy")) {
            this.energyStorage.deserializeNBT(registries, tag.get("energy"));
        }
        if (tag.contains("playerUuid")) {
            this.playerUuid = tag.getUUID("playerUuid");
        }
        if (tag.contains("playerState")) {
            this.playerState = PlayerStates.values()[tag.getInt("playerState")];
        }
        if (tag.contains("shellUuid")) {
            this.shellUuid = tag.getUUID("shellUuid");
        }
        if (tag.contains("shellState")) {
            this.shellState = ShellStates.values()[tag.getInt("shellState")];
        }
        if (tag.contains("shellPercentage")) {
            this.shellPercentage = tag.getInt("shellPercentage");
        }
        if (tag.contains("shellPercentageCooldownTick")) {
            this.shellPercentageCooldownTick = tag.getInt("shellPercentageCooldownTick");
        }
        if (tag.contains("lastRedstoneLevel")) {
            this.lastRedstoneLevel = tag.getInt("lastRedstoneLevel");
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("inv", this.inventoryHandler.serializeNBT(registries));
        tag.put("energy", this.energyStorage.serializeNBT(registries));
        tag.putUUID("playerUuid", this.playerUuid);
        tag.putInt("playerState", this.playerState.ordinal());
        tag.putUUID("shellUuid", this.shellUuid);
        tag.putInt("shellState", this.shellState.ordinal());
        tag.putInt("shellPercentage", this.shellPercentage);
        tag.putInt("shellPercentageCooldownTick", this.shellPercentageCooldownTick);
        tag.putInt("lastRedstoneLevel", this.lastRedstoneLevel);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public void setPlayerUuid(final UUID uuid) {
        this.playerUuid = uuid;
    }

    public UUID getShellUuid() {
        return this.shellUuid;
    }

    public ShellStates getShellState() {
        return this.shellState;
    }

    public PlayerStates getPlayerState() {
        return this.playerState;
    }

    public void setPlayerState(final PlayerStates playerState) {
        this.playerState = playerState;
    }

    public int getShellPercentage() {
        return this.shellPercentage;
    }

    public float getAnimPrevProgress() {
        return this.animPrevProgress;
    }

    public float getAnimProgress() {
        return this.animProgress;
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.SHELL_FORGE.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new ShellForgeContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    public enum ShellStates {
        CREATE,
        CREATING,
        EXTERMINATE,
        EXTERMINATING,
        DECAYING
    }

    public enum PlayerStates {
        NONE,
        GOING_IN,
        INSIDE,
        GOING_OUT,
        TRANSFERRED
    }
}
