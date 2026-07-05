package com.xiaoshi2022.crpchessoflives.clone.gui;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.crpchessoflives.clone.entities.ShellEntity;
import com.xiaoshi2022.crpchessoflives.clone.storage.ClientShellData;
import com.xiaoshi2022.crpchessoflives.clone.storage.ShellState;
import com.xiaoshi2022.crpchessoflives.clone.utils.PositionReference;
import com.xiaoshi2022.crpchessoflives.clone.utils.ShellBundle;
import com.xiaoshi2022.crpchessoflives.clone.utils.ShellBundle.InventoryEntry;
import com.xiaoshi2022.crpchessoflives.clone.utils.ShellBundle.ShellEntry;
import com.xiaoshi2022.crpchessoflives.clone.utils.ShellBundle.StatEntry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

import static com.xiaoshi2022.crpchessoflives.CRPChessOfLives.MODID;
import static com.xiaoshi2022.crpchessoflives.clone.blockentities.renderer.ShellForgeBlockEntityRenderer.getPlayerShellFromCache;
import static com.xiaoshi2022.crpchessoflives.clone.utils.MathUtils.EMPTY_UUID;

public class ShellSelectionOverlay extends RadialMenuRenderer<ShellEntry> implements LayeredDraw.Layer {
    public static final ShellSelectionOverlay INSTANCE = new ShellSelectionOverlay();
    private static final Map<UUID, ShellEntity> SHELL_CACHE = new HashMap<>();
    private static final ResourceLocation INVENTORY_LOCATION = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/clean_inventory.png");

    // 自定义槽位 ID 和纹理映射
    private static final List<ResourceLocation> SLOT_IDS = List.of(
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
            ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")
    );

    // 食物图标 - Minecraft 1.21.1 使用这些 ResourceLocation
    private static final ResourceLocation FOOD_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation FOOD_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_half");
    private static final ResourceLocation FOOD_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation FOOD_EMPTY_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty_hunger");
    private static final ResourceLocation FOOD_HALF_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_half_hunger");
    private static final ResourceLocation FOOD_FULL_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full_hunger");

    // 护甲图标
    private static final ResourceLocation ARMOR_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation ARMOR_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_full");

    @Nullable
    private Consumer<ShellEntry> onSelectShell;
    @Nullable
    private Runnable onClose;
    @Nullable
    private ShellBundle displayedShell;

    @Override
    public void render(final GuiGraphics graphics, final DeltaTracker deltaTracker) {
        if (!this.isOpened()) {
            return;
        }

        super.render(graphics);

        @Nullable final ShellEntry shellEntry = super.lastIndexUnderMouse != -1 ? this.getEntries().get(super.lastIndexUnderMouse) : null;

        if (shellEntry == null) {
            return;
        }

        final PoseStack pose = graphics.pose();
        final float centerX = graphics.guiWidth() / 2f;
        final float centerY = graphics.guiHeight() / 2f;

        final int invWidth = 176;
        final int invHeight = 166;

        // Top left
        this.renderInventory(graphics, pose, centerX, centerY, invWidth, invHeight, shellEntry);

        // Bottom left
        this.renderStats(graphics, pose, centerX, centerY, invWidth, invHeight, shellEntry);
    }

    private void renderInventory(final GuiGraphics graphics,
                                 final PoseStack pose,
                                 final float centerX,
                                 final float centerY,
                                 final int invWidth,
                                 final int invHeight,
                                 final ShellEntry shellEntry) {
        if (shellEntry.inventory().isEmpty()) {
            return;
        }

        final var textureAtlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);

        pose.pushPose();
        pose.translate(centerX - (OUTER + 15f) - invWidth / 2f, centerY - invHeight / 2f / 2f, 0f);
        pose.scale(0.5f, 0.5f, 0.5f);

        graphics.blit(INVENTORY_LOCATION, 0, 0, 0, 0, invWidth, invHeight, 256, 256);

        for (int i = 0; i < 4; ++i) {
            final ItemStack armorStack = shellEntry.inventory().get().armor().get(4 - (i + 1));
            if (armorStack.isEmpty()) {
                final ResourceLocation emptySlot = SLOT_IDS.get(i);
                final TextureAtlasSprite sprite = textureAtlas.apply(emptySlot);
                if (sprite != null) {
                    graphics.blit(8, 8 + i * 18, 0, 16, 16, sprite);
                }
            } else {
                this.renderSlotContent(graphics, armorStack, 8, 8 + i * 18, invWidth);
            }
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                final ItemStack stack = shellEntry.inventory().get().items().get(j + (i + 1) * 9);
                this.renderSlotContent(graphics, stack, 8 + j * 18, 84 + i * 18, invWidth);
            }
        }

        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = shellEntry.inventory().get().items().get(i);
            this.renderSlotContent(graphics, stack, 8 + i * 18, 142, invWidth);
        }

        final ItemStack offHandStack = shellEntry.inventory().get().offhand().getFirst();
        if (offHandStack.isEmpty()) {
            final TextureAtlasSprite sprite = textureAtlas.apply(InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            graphics.blit(77, 62, 0, 16, 16, sprite);
        } else {
            this.renderSlotContent(graphics, offHandStack, 77, 62, invWidth);
        }

        pose.popPose();
    }

    private void renderStats(final GuiGraphics graphics,
                             final PoseStack pose,
                             final float centerX,
                             final float centerY,
                             final int invWidth,
                             final int invHeight,
                             final ShellEntry shellEntry) {
        if (shellEntry.stats().isEmpty()) {
            return;
        }

        final float bottomWidth = 82f;
        final float offsetX = (invWidth / 2f - bottomWidth) / 2f;
        final int currentHealth = Mth.ceil(shellEntry.stats().get().health());

        pose.pushPose();
        pose.translate(centerX - (OUTER + 15f) - invWidth / 2f + offsetX, centerY - invHeight / 2f / 2f + invHeight / 2f + 10f, 0f);
        this.renderArmor(graphics, shellEntry.stats().get(), 0, 0);
        this.renderHearts(graphics, 0, 10, 5, 20f, currentHealth, 0);
        this.renderFood(graphics, shellEntry.stats().get(), 0, 20);
        pose.popPose();
    }

    @Override
    public Component getTitle(final ShellEntry entry) {
        return Component.literal(entry.title());
    }

    @Override
    public List<ShellEntry> getEntries() {
        return this.displayedShell == null ? List.of() : this.displayedShell.getEntries();
    }

    @Nullable
    public ShellBundle getDisplayedShell() {
        return this.displayedShell;
    }

    @Nullable
    @Override
    public ShellEntity getPlayerShell(final ShellEntry entry) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || entry.shellUuid().isEmpty()) {
            return null;
        }

        UUID ownerUuid = entry.ownerUuid().orElse(null);
        if (ownerUuid == null || ownerUuid.equals(EMPTY_UUID)) {
            ownerUuid = ClientShellData.INSTANCE.findOwnerUuidByShellUuid(entry.shellUuid().get());
        }
        final UUID shellUuid = entry.shellUuid().get();
        if (ownerUuid == null || ownerUuid.equals(EMPTY_UUID) || shellUuid.equals(EMPTY_UUID)) {
            return null;
        }

        return getPlayerShellFromCache(mc.level, ownerUuid, shellUuid, SHELL_CACHE);
    }

    @Override
    public int getPlayerShellCreationProgress(final ShellEntry entry) {
        if (entry.shellUuid().isEmpty()) {
            return 0;
        }
        UUID ownerUuid = entry.ownerUuid().orElse(null);
        if (ownerUuid == null) {
            ownerUuid = ClientShellData.INSTANCE.findOwnerUuidByShellUuid(entry.shellUuid().get());
        }
        if (ownerUuid == null) {
            return 0;
        }
        final ShellState shellState = ClientShellData.INSTANCE.get(ownerUuid, entry.shellUuid().get());
        if (shellState == null) {
            return 0;
        }
        return shellState.shellCreationProgress();
    }

    public void keyPressed(final int key) {
        if (key == Minecraft.getInstance().options.keyUp.getKey().getValue()) {
            this.cancel();
        }
    }

    public void mouseClick(final double mouseX, final double mouseY, final int button, final int action) {
        if (action == GLFW.GLFW_RELEASE) {
            return;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            this.cancel();
        }

        final MousePos diffFromCenter = getDiffFromCenter(mouseX, mouseY);
        final double distanceFromCenter = Mth.length(diffFromCenter.x(), diffFromCenter.y());
        if (distanceFromCenter < 30 || distanceFromCenter > RadialMenuRenderer.OUTER + 30) {
            return;
        }

        final int selectionIndex = this.getElementUnderMouse();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (this.onSelectShell != null) {
                final ShellEntry shellEntry = this.getEntries().get(selectionIndex);
                if (this.getPlayerShellCreationProgress(shellEntry) != 100) {
                    return;
                }
                this.onSelectShell.accept(shellEntry);
            }

            this.close(true);
        }
    }

    private void renderSlotContent(final GuiGraphics graphics, final ItemStack stack, final int x, final int y, final int imageWidth) {
        final int seed = x + y * imageWidth;
        graphics.renderItem(stack, x, y, seed);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    /**
     * 渲染食物 - 从 Gui 类复制并修改
     */
    private void renderFood(final GuiGraphics graphics, final StatEntry stats, final int x, final int y) {
        final int foodLevel = stats.foodLevel();

        final boolean hasHunger = stats.activeEffects().containsKey(MobEffects.HUNGER);
        final ResourceLocation emptySprite = hasHunger ? FOOD_EMPTY_HUNGER_SPRITE : FOOD_EMPTY_SPRITE;
        final ResourceLocation halfSprite = hasHunger ? FOOD_HALF_HUNGER_SPRITE : FOOD_HALF_SPRITE;
        final ResourceLocation fullSprite = hasHunger ? FOOD_FULL_HUNGER_SPRITE : FOOD_FULL_SPRITE;

        RenderSystem.enableBlend();

        for (int i = 0; i < 10; i++) {
            final int foodIndex = i * 2 + 1;
            final int posX = x + i * 8;

            graphics.blitSprite(emptySprite, posX, y, 9, 9);

            if (foodIndex < foodLevel) {
                graphics.blitSprite(fullSprite, posX, y, 9, 9);
            } else if (foodIndex == foodLevel) {
                graphics.blitSprite(halfSprite, posX, y, 9, 9);
            }
        }

        RenderSystem.disableBlend();
    }

    /**
     * 渲染护甲 - 从 Gui 类复制并修改
     */
    private void renderArmor(final GuiGraphics guiGraphics, final StatEntry stats, final int x, final int y) {
        final int armorValue = stats.armorValue();
        if (armorValue > 0) {
            RenderSystem.enableBlend();

            for (int i = 0; i < 10; ++i) {
                final int armorIndex = i * 2 + 1;
                final int posX = x + i * 8;

                if (armorIndex < armorValue) {
                    guiGraphics.blitSprite(ARMOR_FULL_SPRITE, posX, y, 9, 9);
                } else if (armorIndex == armorValue) {
                    guiGraphics.blitSprite(ARMOR_HALF_SPRITE, posX, y, 9, 9);
                } else {
                    guiGraphics.blitSprite(ARMOR_EMPTY_SPRITE, posX, y, 9, 9);
                }
            }

            RenderSystem.disableBlend();
        }
    }

    /**
     * 渲染生命值 - 从 Gui 类复制并修改
     */
    private void renderHearts(
            final GuiGraphics guiGraphics,
            final int x,
            final int y,
            final int height,
            final float maxHealth,
            final int currentHealth,
            final int absorptionAmount) {
        final Gui.HeartType heartType = Gui.HeartType.NORMAL;
        final boolean hardcore = false;

        final int maxHealthHalves = Mth.ceil((double) maxHealth / 2.0);
        final int absorptionHearts = Mth.ceil((double) absorptionAmount / 2.0);
        final int totalHearts = maxHealthHalves + absorptionHearts;
        final int maxHealthFull = maxHealthHalves * 2;

        for (int heartIndex = totalHearts - 1; heartIndex >= 0; heartIndex--) {
            final int row = heartIndex / 10;
            final int column = heartIndex % 10;

            final int posX = x + column * 8;
            final int posY = y - row * height;

            // 渲染空心脏容器
            this.renderHeart(guiGraphics, Gui.HeartType.CONTAINER, posX, posY, hardcore, false, false);

            final int halfIndex = heartIndex * 2;

            // 渲染吸收心脏
            final boolean isAbsorptionHeart = heartIndex >= maxHealthHalves;
            if (isAbsorptionHeart) {
                final int absorptionHalfIndex = halfIndex - maxHealthFull;
                if (absorptionHalfIndex < absorptionAmount) {
                    final boolean isHalf = absorptionHalfIndex + 1 == absorptionAmount;
                    this.renderHeart(guiGraphics, heartType == Gui.HeartType.WITHERED ? heartType : Gui.HeartType.ABSORBING,
                            posX, posY, hardcore, false, isHalf);
                }
            }

            // 渲染普通心脏
            if (halfIndex < currentHealth) {
                final boolean isHalf = halfIndex + 1 == currentHealth;
                this.renderHeart(guiGraphics, heartType, posX, posY, hardcore, false, isHalf);
            }
        }
    }

    /**
     * 渲染单个心脏
     */
    private void renderHeart(final GuiGraphics guiGraphics,
                             final Gui.HeartType heartType,
                             final int x,
                             final int y,
                             final boolean hardcore,
                             final boolean halfHeart,
                             final boolean blinking) {
        RenderSystem.enableBlend();
        guiGraphics.blitSprite(heartType.getSprite(hardcore, blinking, halfHeart), x, y, 9, 9);
        RenderSystem.disableBlend();
    }

    public void open(final Player player, final PositionReference currentForgePos, final Consumer<ShellEntry> onSelectShell, final Runnable onClose) {
        final Minecraft mc = Minecraft.getInstance();
        final List<ShellEntry> shellEntries = new ArrayList<>();

        final var nearby = ClientShellData.INSTANCE.getNearbyCompletedAllOwners(currentForgePos);
        for (final var entry : nearby) {
            final UUID ownerUuid = entry.getKey();
            final ShellState shellState = entry.getValue();
            if (shellState == null || shellState.shellCreationProgress() < 100) {
                continue;
            }

            String ownerName;
            try {
                final var info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(ownerUuid) : null;
                if (info != null && info.getProfile() != null && info.getProfile().getName() != null) {
                    ownerName = info.getProfile().getName();
                } else if (shellState.playerData().contains("CustomName", Tag.TAG_STRING)) {
                    ownerName = Component.Serializer.fromJson(shellState.playerData().getString("CustomName"), mc.level.registryAccess()).getString();
                } else {
                    ownerName = ownerUuid.toString().substring(0, 8);
                }
            } catch (final Exception ignore) {
                ownerName = ownerUuid.toString().substring(0, 8);
            }

            final Inventory inventory = new Inventory(player);
            inventory.load(shellState.playerData().getList("Inventory", ListTag.TAG_COMPOUND));
            final InventoryEntry inventoryEntry = new InventoryEntry(inventory.items, inventory.armor, inventory.offhand);

            final FoodData foodData = new FoodData();
            foodData.readAdditionalSaveData(shellState.playerData());
            final AttributeMap attributes = new AttributeMap(DefaultAttributes.getSupplier(EntityType.PLAYER));
            attributes.load(shellState.playerData().getList("attributes", ListTag.TAG_COMPOUND));
            final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
            final ListTag effectsTag = shellState.playerData().getList("active_effects", ListTag.TAG_COMPOUND);
            for (int i = 0; i < effectsTag.size(); ++i) {
                final MobEffectInstance effectInstance = MobEffectInstance.load(effectsTag.getCompound(i));
                if (effectInstance != null) {
                    activeEffects.put(effectInstance.getEffect(), effectInstance);
                }
            }
            final StatEntry statEntry = new StatEntry(shellState.playerData().getFloat("Health"),
                    Mth.floor(attributes.getValue(Attributes.ARMOR)),
                    foodData.getFoodLevel(),
                    activeEffects);

            final String title;
            if (ownerUuid.equals(player.getUUID())) {
                title = ownerName;
            } else {
                title = ownerName + " ♦";
            }

            shellEntries.add(new ShellEntry(title, Optional.of(shellState.shellForgePos()),
                    Optional.of(inventoryEntry), Optional.of(statEntry),
                    Optional.of(shellState.shellUUID()), Optional.of(ownerUuid)));
        }

        if (shellEntries.isEmpty()) {
            shellEntries.add(new ShellEntry("", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        }
        this.displayedShell = new ShellBundle(player.getUUID(), shellEntries);

        this.onSelectShell = onSelectShell;
        this.onClose = onClose;
    }

    public boolean isOpened() {
        return this.displayedShell != null;
    }

    private void cancel() {
        if (this.onClose != null) {
            this.onClose.run();
        }

        this.close(true);
    }

    public void close(final boolean grabMouse) {
        if (grabMouse) {
            Minecraft.getInstance().mouseHandler.grabMouse();
        }
        this.displayedShell = null;
        this.clearState();
    }
}