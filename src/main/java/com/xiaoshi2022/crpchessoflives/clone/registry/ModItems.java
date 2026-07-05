package com.xiaoshi2022.crpchessoflives.clone.registry;

import com.xiaoshi2022.crpchessoflives.CRPChessOfLives;
import com.xiaoshi2022.crpchessoflives.clone.items.ItemWithOwner;
import com.xiaoshi2022.crpchessoflives.clone.items.SyringeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CRPChessOfLives.MODID);

    public static final DeferredItem<ItemWithOwner> DNA = ITEMS.registerItem("dna", ItemWithOwner::new, new Item.Properties());
    public static final DeferredItem<SyringeItem> EMPTY_SYRINGE = ITEMS.registerItem("empty_syringe", SyringeItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<SyringeItem> BLOOD_SYRINGE = ITEMS.registerItem("blood_syringe", SyringeItem::new, new Item.Properties());

    public static final DeferredItem<BlockItem> SHELL_FORGE = ITEMS.registerSimpleBlockItem("shell_forge", ModBlocks.SHELL_FORGE, new Item.Properties());
    public static final DeferredItem<BlockItem> CENTRIFUGE = ITEMS.registerSimpleBlockItem("centrifuge", ModBlocks.CENTRIFUGE, new Item.Properties());
}