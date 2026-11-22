package jp.yuka1201.luckyblock;

import java.util.function.Consumer;
import jp.yuka1201.luckyblock.block.LuckyBlock;
import jp.yuka1201.luckyblock.event.LuckyEventTable;
import jp.yuka1201.luckyblock.ui.LuckyUiEffects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class LuckyBlockMod implements ModInitializer {
    public static final String MOD_ID = "luckyblock";

    public static final Identifier LUCKY_BLOCK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "lucky_block"); // 要検証: 26.3 Identifier 生成メソッド名
    public static final ResourceKey<Block> LUCKY_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, LUCKY_BLOCK_ID);
    public static final ResourceKey<Item> LUCKY_BLOCK_ITEM_KEY = ResourceKey.create(Registries.ITEM, LUCKY_BLOCK_ID);

    public static Block LUCKY_BLOCK;
    public static Item LUCKY_BLOCK_ITEM;

    @Override
    public void onInitialize() {
        LUCKY_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                LUCKY_BLOCK_KEY,
                new LuckyBlock(
                        BlockBehaviour.Properties.of()
                                .setId(LUCKY_BLOCK_KEY)
                                .mapColor(MapColor.GOLD)
                                .strength(0.6F)
                                .sound(SoundType.GLASS)
                                .lightLevel(state -> 7)
                                .noLootTable()
                )
        );

        LUCKY_BLOCK_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                LUCKY_BLOCK_ITEM_KEY,
                new BlockItem(
                        LUCKY_BLOCK,
                        new Item.Properties()
                                .setId(LUCKY_BLOCK_ITEM_KEY)
                                .rarity(Rarity.EPIC)
                ) {
                    @Override
                    public void appendHoverText(
                            ItemStack stack,
                            Item.TooltipContext context,
                            TooltipDisplay tooltipDisplay, // 要検証: 26.3 appendHoverText 引数シグネチャ
                            Consumer<Component> tooltipAdder,
                            TooltipFlag flag
                    ) {
                        tooltipAdder.accept(Component.translatable("tooltip.luckyblock.lucky_block.border").withStyle(ChatFormatting.GOLD));
                        tooltipAdder.accept(Component.translatable("tooltip.luckyblock.lucky_block.line1").withStyle(ChatFormatting.YELLOW));
                        tooltipAdder.accept(Component.translatable("tooltip.luckyblock.lucky_block.line2").withStyle(ChatFormatting.GRAY));
                        tooltipAdder.accept(Component.translatable("tooltip.luckyblock.lucky_block.line3").withStyle(ChatFormatting.AQUA));
                        tooltipAdder.accept(Component.translatable("tooltip.luckyblock.lucky_block.border").withStyle(ChatFormatting.GOLD));
                    }
                }
        );

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(LUCKY_BLOCK_ITEM);
        });

        LuckyUiEffects.init();
        LuckyEventTable.init();
    }
}
