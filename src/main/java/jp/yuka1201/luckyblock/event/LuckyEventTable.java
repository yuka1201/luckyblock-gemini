package jp.yuka1201.luckyblock.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jp.yuka1201.luckyblock.LuckyBlockMod;
import jp.yuka1201.luckyblock.ui.LuckyUiEffects;
import jp.yuka1201.luckyblock.ui.LuckyUiEffects.Category;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class LuckyEventTable {
    private static final List<WeightedEntry> ENTRIES = new ArrayList<>();
    private static final List<DelayedTask> DELAYED_TASKS = new ArrayList<>();
    private static int totalWeight = 0;
    private static boolean initialized = false;

    private record WeightedEntry(
            int weight,
            Category category,
            String eventNameKey,
            LuckyEvent event
    ) {
    }

    private static final class DelayedTask {
        final ServerLevel level;
        int remainingTicks;
        final Runnable action;

        DelayedTask(ServerLevel level, int remainingTicks, Runnable action) {
            this.level = level;
            this.remainingTicks = remainingTicks;
            this.action = action;
        }
    }

    private LuckyEventTable() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 激レア (4%)
        register(2, Category.RARE, "luckyblock.event.lucky_chain", LuckyEventTable::luckyChain);
        register(2, Category.RARE, "luckyblock.event.golden_vault", LuckyEventTable::goldenVault);

        // 大当たり (32%)
        register(4, Category.JACKPOT, "luckyblock.event.treasure_shower", LuckyEventTable::treasureShower);
        register(4, Category.JACKPOT, "luckyblock.event.random_gear", LuckyEventTable::randomGear);
        register(4, Category.JACKPOT, "luckyblock.event.xp_rain", LuckyEventTable::xpRain);
        register(4, Category.JACKPOT, "luckyblock.event.potion_shower", LuckyEventTable::potionShower);
        register(3, Category.JACKPOT, "luckyblock.event.snack_box", LuckyEventTable::snackBox);
        register(3, Category.JACKPOT, "luckyblock.event.helper_golem", LuckyEventTable::helperGolem);
        register(3, Category.JACKPOT, "luckyblock.event.elytra_pack", LuckyEventTable::elytraPack);
        register(4, Category.JACKPOT, "luckyblock.event.ore_pack", LuckyEventTable::orePack);
        register(3, Category.JACKPOT, "luckyblock.event.healing_spring", LuckyEventTable::healingSpring);

        // ハズレ (32%)
        register(4, Category.BAD, "luckyblock.event.mini_monsters", LuckyEventTable::miniMonsters);
        register(3, Category.BAD, "luckyblock.event.pitfall", LuckyEventTable::pitfall);
        register(3, Category.BAD, "luckyblock.event.primed_tnt", LuckyEventTable::primedTnt);
        register(3, Category.BAD, "luckyblock.event.cobweb_trap", LuckyEventTable::cobwebTrap);
        register(3, Category.BAD, "luckyblock.event.hunger_slow_curse", LuckyEventTable::hungerSlowCurse);
        register(3, Category.BAD, "luckyblock.event.lightning_alert", LuckyEventTable::lightningAlert);
        register(3, Category.BAD, "luckyblock.event.fake_chest", LuckyEventTable::fakeChest);
        register(4, Category.BAD, "luckyblock.event.slime_party", LuckyEventTable::slimeParty);
        register(3, Category.BAD, "luckyblock.event.darkness_flash", LuckyEventTable::darknessFlash);
        register(3, Category.BAD, "luckyblock.event.safe_lava", LuckyEventTable::safeLava);

        // ネタ (32%)
        register(4, Category.FUN, "luckyblock.event.cake_tower", LuckyEventTable::cakeTower);
        register(4, Category.FUN, "luckyblock.event.sky_fireworks", LuckyEventTable::skyFireworks);
        register(4, Category.FUN, "luckyblock.event.flying_animals", LuckyEventTable::flyingAnimals);
        register(4, Category.FUN, "luckyblock.event.wool_shower", LuckyEventTable::woolShower);
        register(3, Category.FUN, "luckyblock.event.note_live", LuckyEventTable::noteLive);
        register(3, Category.FUN, "luckyblock.event.pumpkin_curse", LuckyEventTable::pumpkinCurse);
        register(3, Category.FUN, "luckyblock.event.snow_golems", LuckyEventTable::snowGolems);
        register(4, Category.FUN, "luckyblock.event.rainbow_sheep", LuckyEventTable::rainbowSheep);
        register(3, Category.FUN, "luckyblock.event.mystery_sign", LuckyEventTable::mysterySign);

        ServerTickEvents.END_SERVER_TICK.register(server -> tickDelayedTasks());
    }

    private static void register(int weight, Category category, String eventNameKey, LuckyEvent event) {
        ENTRIES.add(new WeightedEntry(weight, category, eventNameKey, event));
        totalWeight += weight;
    }

    public static void triggerRandomEvent(ServerLevel level, BlockPos pos, Player player) {
        if (!initialized) {
            init();
        }
        if (totalWeight <= 0) {
            return;
        }

        int roll = level.getRandom().nextInt(totalWeight);
        int cursor = 0;

        for (WeightedEntry entry : ENTRIES) {
            cursor += entry.weight;
            if (roll < cursor) {
                LuckyUiEffects.startGachaSequence(
                        level,
                        pos,
                        player,
                        entry.category,
                        entry.eventNameKey,
                        () -> entry.event.execute(level, pos, player)
                );
                return;
            }
        }
    }

    private static void tickDelayedTasks() {
        Iterator<DelayedTask> it = DELAYED_TASKS.iterator();
        while (it.hasNext()) {
            DelayedTask task = it.next();
            task.remainingTicks--;
            if (task.remainingTicks <= 0) {
                task.action.run();
                it.remove();
            }
        }
    }

    private static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        DELAYED_TASKS.add(new DelayedTask(level, Math.max(1, delayTicks), action));
    }

    private static void dropBurstItem(ServerLevel level, Vec3 origin, ItemStack stack, RandomSource random) {
        ItemEntity entity = new ItemEntity(level, origin.x, origin.y, origin.z, stack);
        entity.setDefaultPickUpDelay();
        entity.setDeltaMovement(
                (random.nextDouble() - 0.5) * 0.45,
                0.28 + random.nextDouble() * 0.25,
                (random.nextDouble() - 0.5) * 0.45
        );
        level.addFreshEntity(entity);
    }

    private static ItemStack createColoredFirework(RandomSource random, int... primaryColors) {
        List<Integer> colors = new ArrayList<>();
        for (int c : primaryColors) {
            colors.add(c);
        }
        FireworkExplosion.Shape shape = random.nextBoolean()
                ? FireworkExplosion.Shape.STAR
                : FireworkExplosion.Shape.LARGE_BALL;
        FireworkExplosion explosion = new FireworkExplosion(shape, colors, List.of(), true, false); // 要検証
        Fireworks fireworks = new Fireworks(1, List.of(explosion)); // 要検証
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponents.FIREWORKS, fireworks);
        return stack;
    }

    private static void spawnFirework(ServerLevel level, Vec3 pos, ItemStack rocketStack, RandomSource random) {
        FireworkRocketEntity rocket = new FireworkRocketEntity(
                level,
                pos.x + (random.nextDouble() - 0.5) * 2.5,
                pos.y + 0.5,
                pos.z + (random.nextDouble() - 0.5) * 2.5,
                rocketStack
        );
        rocket.setDeltaMovement(
                (random.nextDouble() - 0.5) * 0.08,
                0.55 + random.nextDouble() * 0.15,
                (random.nextDouble() - 0.5) * 0.08
        );
        level.addFreshEntity(rocket);
    }

    // 1. ラッキーの連鎖
    private static void luckyChain(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        BlockState luckyState = LuckyBlockMod.LUCKY_BLOCK.defaultBlockState();
        List<BlockPos> candidates = new ArrayList<>();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos candidate = pos.offset(dx, dy, dz);
                    if (level.getBlockState(candidate).isAir()) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        int placed = 0;
        while (!candidates.isEmpty() && placed < 3) {
            BlockPos chosen = candidates.remove(random.nextInt(candidates.size()));
            level.setBlock(chosen, luckyState, Block.UPDATE_ALL);
            placed++;
        }
        if (placed == 0) {
            dropBurstItem(level, Vec3.atCenterOf(pos), new ItemStack(LuckyBlockMod.LUCKY_BLOCK_ITEM, 2), random);
        }
    }

    // 2. 黄金の宝物庫
    private static void goldenVault(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        dropBurstItem(level, origin, new ItemStack(Items.NETHERITE_INGOT, 1), random);
        dropBurstItem(level, origin, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1), random);
        dropBurstItem(level, origin, new ItemStack(Items.DIAMOND_BLOCK, 1), random);
        ItemStack rainbow = createColoredFirework(random, 0xFF5D8F, 0xFFD700, 0x4DA3FF, 0x38E8B0);
        for (int i = 0; i < 4; i++) {
            spawnFirework(level, origin, rainbow, random);
        }
    }

    // 3. お宝シャワー
    private static void treasureShower(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        for (int i = 0; i < 6; i++) dropBurstItem(level, origin, new ItemStack(Items.DIAMOND), random);
        for (int i = 0; i < 8; i++) dropBurstItem(level, origin, new ItemStack(Items.EMERALD), random);
        for (int i = 0; i < 3; i++) dropBurstItem(level, origin, new ItemStack(Items.GOLDEN_APPLE), random);
        ItemStack goldRocket = createColoredFirework(random, 0xFFD700, 0xFFF275, 0xFFFFFF);
        for (int i = 0; i < 3; i++) spawnFirework(level, origin, goldRocket, random);
    }

    // 4. ランダム装備セット
    private static void randomGear(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        ItemStack[] pool = {
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.DIAMOND_HELMET),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.IRON_SWORD),
                new ItemStack(Items.SHIELD)
        };
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            ItemStack item = pool[random.nextInt(pool.length)].copy();
            item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); // 要検証
            dropBurstItem(level, origin, item, random);
        }
    }

    // 5. 経験値オーブの雨
    private static void xpRain(ServerLevel level, BlockPos pos, Player player) {
        Vec3 origin = Vec3.atCenterOf(pos);
        for (int i = 0; i < 12; i++) {
            ExperienceOrb.award(level, origin, 25); // 要検証: ExperienceOrb.award シグネチャ
        }
    }

    // 6. ポーションシャワー
    private static void potionShower(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        if (player != null) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1)); // 要検証: MobEffects 定数名
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 400, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
        dropBurstItem(level, origin, new ItemStack(Items.GOLDEN_CARROT, 4), random);
        dropBurstItem(level, origin, new ItemStack(Items.HONEY_BOTTLE, 2), random);
    }

    // 7. おやつボックス
    private static void snackBox(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        dropBurstItem(level, origin, new ItemStack(Items.CAKE, 1), random);
        dropBurstItem(level, origin, new ItemStack(Items.COOKIE, 12), random);
        dropBurstItem(level, origin, new ItemStack(Items.SWEET_BERRIES, 8), random);
        dropBurstItem(level, origin, new ItemStack(Items.GOLDEN_CARROT, 4), random);
    }

    // 8. お助けゴーレム召喚
    private static void helperGolem(ServerLevel level, BlockPos pos, Player player) {
        IronGolem golem = new IronGolem(EntityTypes.IRON_GOLEM, level); // 要検証
        golem.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        golem.setPlayerCreated(true);
        level.addFreshEntity(golem);
    }

    // 9. エリトラ体験パック
    private static void elytraPack(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        dropBurstItem(level, origin, new ItemStack(Items.ELYTRA, 1), random);
        dropBurstItem(level, origin, new ItemStack(Items.FIREWORK_ROCKET, 16), random);
    }

    // 10. 幸運の鉱石パック
    private static void orePack(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        dropBurstItem(level, origin, new ItemStack(Items.RAW_IRON, 12), random);
        dropBurstItem(level, origin, new ItemStack(Items.RAW_GOLD, 8), random);
        dropBurstItem(level, origin, new ItemStack(Items.REDSTONE, 16), random);
        dropBurstItem(level, origin, new ItemStack(Items.LAPIS_LAZULI, 16), random);
        dropBurstItem(level, origin, new ItemStack(Items.AMETHYST_SHARD, 8), random);
    }

    // 11. ヒーリングの泉
    private static void healingSpring(ServerLevel level, BlockPos pos, Player player) {
        if (player != null) {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().eat(20, 20.0F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        }
        level.sendParticles(ParticleTypes.HEART, true, false, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 24, 0.9, 0.8, 0.9, 0.05);
    }

    // 12. ミニモンスター隊
    private static void miniMonsters(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Component guardName = Component.translatable("entity.luckyblock.lucky_guard");
        for (int i = 0; i < 3; i++) {
            Zombie zombie = new Zombie(EntityTypes.ZOMBIE, level); // 要検証
            zombie.setBaby(true);
            zombie.setCustomName(guardName);
            zombie.setCustomNameVisible(true);
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLD_HELMET));
            zombie.setPos(pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0, pos.getY(), pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0);
            level.addFreshEntity(zombie);
        }
        Skeleton skeleton = new Skeleton(EntityTypes.SKELETON, level); // 要検証
        skeleton.setCustomName(guardName);
        skeleton.setCustomNameVisible(true);
        skeleton.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(skeleton);
    }

    // 13. 足元落とし穴
    private static void pitfall(ServerLevel level, BlockPos pos, Player player) {
        BlockPos center = player != null ? player.blockPosition() : pos;
        int depth = 5;
        for (int yStep = 1; yStep <= depth; yStep++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos target = center.offset(dx, -yStep, dz);
                    BlockState current = level.getBlockState(target);
                    if (current.getDestroySpeed(level, target) < 0.0F || level.getBlockEntity(target) != null) continue;
                    level.setBlock(target, (yStep == depth ? Blocks.WATER : Blocks.AIR).defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    // 14. 着火済みTNT
    private static void primedTnt(ServerLevel level, BlockPos pos, Player player) {
        PrimedTnt tnt = new PrimedTnt(EntityTypes.TNT, level); // 要検証
        tnt.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        tnt.setFuse(60);
        level.addFreshEntity(tnt);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.2F, 1.0F);
    }

    // 15. クモの巣トラップ
    private static void cobwebTrap(ServerLevel level, BlockPos pos, Player player) {
        BlockPos center = player != null ? player.blockPosition() : pos;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos target = center.offset(dx, 0, dz);
                if (level.getBlockState(target).isAir()) {
                    level.setBlock(target, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    // 16. 空腹＆鈍足呪い
    private static void hungerSlowCurse(ServerLevel level, BlockPos pos, Player player) {
        if (player != null) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
        }
    }

    // 17. 落雷注意報
    private static void lightningAlert(ServerLevel level, BlockPos pos, Player player) {
        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level); // 要検証
        bolt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(bolt);
    }

    // 18. ニセモノ宝箱
    private static void fakeChest(ServerLevel level, BlockPos pos, Player player) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(13, new ItemStack(Items.ROTTEN_FLESH, 1));
        }
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.9F, 1.1F);
    }

    // 19. スライム大分裂
    private static void slimeParty(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            Slime slime = new Slime(EntityTypes.SLIME, level); // 要検証
            slime.setSize(2, true);
            slime.setPos(pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0, pos.getY() + 0.2, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0);
            level.addFreshEntity(slime);
        }
    }

    // 20. 暗闇フラッシュ
    private static void darknessFlash(ServerLevel level, BlockPos pos, Player player) {
        if (player != null) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
        }
    }

    // 21. マグマ注意（安全版）
    private static void safeLava(ServerLevel level, BlockPos pos, Player player) {
        BlockPos lavaPos = pos.east();
        if (level.getBlockState(lavaPos).isAir() || level.getBlockState(lavaPos).canBeReplaced()) {
            level.setBlock(lavaPos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
        }
        dropBurstItem(level, Vec3.atCenterOf(pos), new ItemStack(Items.WATER_BUCKET, 1), level.getRandom());
    }

    // 22. ケーキタワー召喚
    private static void cakeTower(ServerLevel level, BlockPos pos, Player player) {
        for (int dy = 0; dy < 3; dy++) {
            BlockPos target = pos.above(dy);
            BlockState current = level.getBlockState(target);
            if (current.isAir() || current.canBeReplaced()) {
                level.setBlock(target, Blocks.CAKE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.sendParticles(ParticleTypes.HEART, true, false, pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, 18, 0.8, 1.0, 0.8, 0.05);
    }

    // 23. 天空花火大会（25tick遅延で焼きジャガイモ1個）
    private static void skyFireworks(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        int[] palette = {0xFF5D8F, 0x0074E2, 0x2FBF71, 0xFFC93D, 0x8B5CF6};
        for (int i = 0; i < 6; i++) {
            int c1 = palette[random.nextInt(palette.length)];
            int c2 = palette[random.nextInt(palette.length)];
            ItemStack rocket = createColoredFirework(random, c1, c2);
            schedule(level, i * 4, () -> spawnFirework(level, origin, rocket, random));
        }
        schedule(level, 25, () -> dropBurstItem(level, origin.add(0.0, 0.6, 0.0), new ItemStack(Items.BAKED_POTATO), random));
    }

    // 24. 空飛ぶブタ／ニワトリ軍団
    private static void flyingAnimals(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        MobEffectInstance levitation = new MobEffectInstance(MobEffects.LEVITATION, 160, 1, false, true);
        for (int i = 0; i < 3; i++) {
            Pig pig = new Pig(EntityTypes.PIG, level); // 要検証
            pig.setPos(pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.5, pos.getY() + 0.2, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.5);
            pig.addEffect(new MobEffectInstance(levitation));
            level.addFreshEntity(pig);
        }
        for (int i = 0; i < 3; i++) {
            Chicken chicken = new Chicken(EntityTypes.CHICKEN, level); // 要検証
            chicken.setPos(pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.5, pos.getY() + 0.2, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.5);
            chicken.addEffect(new MobEffectInstance(levitation));
            level.addFreshEntity(chicken);
        }
    }

    // 25. カラフル羊毛シャワー
    private static void woolShower(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        Vec3 origin = Vec3.atCenterOf(pos);
        Item[] wools = {Items.RED_WOOL, Items.BLUE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL, Items.PURPLE_WOOL, Items.PINK_WOOL};
        for (Item w : wools) {
            dropBurstItem(level, origin, new ItemStack(w, 4), random);
        }
    }

    // 26. 音符ライブ会場
    private static void noteLive(ServerLevel level, BlockPos pos, Player player) {
        float[] pitches = {0.7F, 0.85F, 1.0F, 1.15F, 1.35F, 1.5F};
        for (int i = 0; i < pitches.length; i++) {
            float p = pitches[i];
            schedule(level, i * 4, () -> {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, 1.2F, p); // 要検証
                level.sendParticles(ParticleTypes.NOTE, true, false, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.7, 0.5, 0.7, 0.2);
            });
        }
    }

    // 27. カボチャ頭の呪い
    private static void pumpkinCurse(ServerLevel level, BlockPos pos, Player player) {
        ItemStack pumpkin = new ItemStack(Items.CARVED_PUMPKIN);
        if (player != null && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, pumpkin);
        } else {
            dropBurstItem(level, Vec3.atCenterOf(pos), pumpkin, level.getRandom());
        }
    }

    // 28. スノーゴーレム部隊
    private static void snowGolems(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            SnowGolem golem = new SnowGolem(EntityTypes.SNOW_GOLEM, level); // 要検証
            golem.setPos(pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0, pos.getY(), pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0);
            level.addFreshEntity(golem);
        }
    }

    // 29. レインボー羊パレード
    private static void rainbowSheep(ServerLevel level, BlockPos pos, Player player) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            Sheep sheep = new Sheep(EntityTypes.SHEEP, level); // 要検証
            sheep.setCustomName(Component.literal("jeb_"));
            sheep.setPos(pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0, pos.getY(), pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0);
            level.addFreshEntity(sheep);
        }
    }

    // 30. 謎の看板メッセージ
    private static void mysterySign(ServerLevel level, BlockPos pos, Player player) {
        String[] messages = {"あたり…かも？", "サキ特製！", "また割ってね♪", "バスク最強！"};
        String msg = messages[level.getRandom().nextInt(messages.length)];
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText().setMessage(1, Component.literal(msg)); // 要検証
            sign.setText(text, true);
        }
    }
}
