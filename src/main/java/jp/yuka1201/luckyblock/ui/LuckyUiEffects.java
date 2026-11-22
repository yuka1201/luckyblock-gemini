package jp.yuka1201.luckyblock.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;

public final class LuckyUiEffects {
    public enum Category {
        RARE(
                "luckyblock.category.rare",
                "luckyblock.flavor.rare",
                ChatFormatting.LIGHT_PURPLE,
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.NOTCHED_10,
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE.value(), // 要検証: SoundEvents 取得形式
                1.05F
        ),
        JACKPOT(
                "luckyblock.category.jackpot",
                "luckyblock.flavor.jackpot",
                ChatFormatting.GOLD,
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.NOTCHED_6,
                SoundEvents.PLAYER_LEVELUP.value(),
                1.2F
        ),
        BAD(
                "luckyblock.category.bad",
                "luckyblock.flavor.bad",
                ChatFormatting.RED,
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS,
                SoundEvents.WITHER_AMBIENT.value(),
                0.75F
        ),
        FUN(
                "luckyblock.category.fun",
                "luckyblock.flavor.fun",
                ChatFormatting.AQUA,
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.NOTCHED_6,
                SoundEvents.EXPERIENCE_ORB_PICKUP.value(),
                1.45F
        );

        public final String titleKey;
        public final String flavorKey;
        public final ChatFormatting color;
        public final BossEvent.BossBarColor barColor;
        public final BossEvent.BossBarOverlay barOverlay;
        public final SoundEvent sound;
        public final float pitch;

        Category(
                String titleKey,
                String flavorKey,
                ChatFormatting color,
                BossEvent.BossBarColor barColor,
                BossEvent.BossBarOverlay barOverlay,
                SoundEvent sound,
                float pitch
        ) {
            this.titleKey = titleKey;
            this.flavorKey = flavorKey;
            this.color = color;
            this.barColor = barColor;
            this.barOverlay = barOverlay;
            this.sound = sound;
            this.pitch = pitch;
        }
    }

    private static final class PlayerStats {
        int totalOpened = 0;
        int jackpotCount = 0;
    }

    private static final class RouletteSession {
        final ServerLevel level;
        final BlockPos pos;
        final Player triggerPlayer;
        final Category finalCategory;
        final String eventNameKey;
        final Runnable onReveal;
        int tick = 0;
        final int totalTicks = 20;

        RouletteSession(
                ServerLevel level,
                BlockPos pos,
                Player triggerPlayer,
                Category finalCategory,
                String eventNameKey,
                Runnable onReveal
        ) {
            this.level = level;
            this.pos = pos;
            this.triggerPlayer = triggerPlayer;
            this.finalCategory = finalCategory;
            this.eventNameKey = eventNameKey;
            this.onReveal = onReveal;
        }
    }

    private static final class TimedBossBar {
        final ServerBossEvent bar;
        int remainingTicks;
        final int maxTicks;

        TimedBossBar(ServerBossEvent bar, int maxTicks) {
            this.bar = bar;
            this.remainingTicks = maxTicks;
            this.maxTicks = maxTicks;
        }
    }

    private static final String[] ROULETTE_SYMBOLS = {"？", "★", "✖", "♪", "✦"};
    private static final ChatFormatting[] ROULETTE_COLORS = {
            ChatFormatting.YELLOW,
            ChatFormatting.GOLD,
            ChatFormatting.RED,
            ChatFormatting.AQUA,
            ChatFormatting.LIGHT_PURPLE
    };

    private static final List<RouletteSession> ROULETTES = new ArrayList<>();
    private static final List<TimedBossBar> ACTIVE_BARS = new ArrayList<>();
    private static final Map<UUID, PlayerStats> STATS = new HashMap<>();
    private static boolean initialized = false;

    private LuckyUiEffects() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> tickAll());
    }

    public static void startGachaSequence(
            ServerLevel level,
            BlockPos pos,
            Player player,
            Category category,
            String eventNameKey,
            Runnable onReveal
    ) {
        if (!initialized) {
            init();
        }
        ROULETTES.add(new RouletteSession(level, pos, player, category, eventNameKey, onReveal));
    }

    private static void tickAll() {
        Iterator<RouletteSession> rIt = ROULETTES.iterator();
        while (rIt.hasNext()) {
            RouletteSession s = rIt.next();
            if (s.tick < s.totalTicks) {
                if (s.tick % 4 == 0) {
                    int idx = (s.tick / 4) % ROULETTE_SYMBOLS.length;
                    sendRouletteFrame(s.level, s.pos, ROULETTE_SYMBOLS[idx], ROULETTE_COLORS[idx], 0.9F + idx * 0.12F);
                }
                s.tick++;
            } else {
                revealResult(s);
                rIt.remove();
            }
        }

        Iterator<TimedBossBar> bIt = ACTIVE_BARS.iterator();
        while (bIt.hasNext()) {
            TimedBossBar tb = bIt.next();
            tb.remainingTicks--;
            if (tb.remainingTicks <= 0) {
                tb.bar.removeAllPlayers();
                tb.bar.setVisible(false);
                bIt.remove();
            } else {
                tb.bar.setProgress((float) tb.remainingTicks / (float) tb.maxTicks);
            }
        }
    }

    private static void sendRouletteFrame(ServerLevel level, BlockPos pos, String symbol, ChatFormatting color, float pitch) {
        Component title = Component.literal("【 " + symbol + " 】").withStyle(color, ChatFormatting.BOLD);
        Component subtitle = Component.translatable("luckyblock.ui.rolling").withStyle(ChatFormatting.GRAY);

        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7F, pitch);

        for (ServerPlayer sp : level.players()) {
            if (sp.blockPosition().distSqr(pos) <= 32.0 * 32.0) {
                sp.connection.send(new ClientboundSetTitlesAnimationPacket(0, 8, 2)); // 要検証
                sp.connection.send(new ClientboundSetTitleTextPacket(title));
                sp.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            }
        }
    }

    private static void revealResult(RouletteSession s) {
        PlayerStats stats = null;
        if (s.triggerPlayer != null) {
            stats = STATS.computeIfAbsent(s.triggerPlayer.getUUID(), id -> new PlayerStats());
            stats.totalOpened++;
            if (s.finalCategory == Category.JACKPOT || s.finalCategory == Category.RARE) {
                stats.jackpotCount++;
            }
        }

        spawnCategoryParticles(s.level, s.pos, s.finalCategory, s.level.getRandom());
        s.level.playSound(null, s.pos, s.finalCategory.sound, SoundSource.PLAYERS, 1.3F, s.finalCategory.pitch);

        Component title = Component.translatable(s.finalCategory.titleKey)
                .withStyle(s.finalCategory.color, ChatFormatting.BOLD);
        Component subtitle = Component.translatable(s.eventNameKey)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);

        String countSuffix = stats != null ? " (通算 " + stats.totalOpened + "個目)" : "";
        Component actionBar = Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(s.finalCategory.titleKey).withStyle(s.finalCategory.color))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(s.eventNameKey).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(countSuffix).withStyle(ChatFormatting.GRAY));

        Component bossTitle = Component.empty()
                .append(Component.translatable(s.finalCategory.titleKey).withStyle(s.finalCategory.color, ChatFormatting.BOLD))
                .append(Component.literal("  ─  ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(s.eventNameKey).withStyle(ChatFormatting.WHITE));

        ServerBossEvent bossBar = new ServerBossEvent(bossTitle, s.finalCategory.barColor, s.finalCategory.barOverlay);
        bossBar.setProgress(1.0F);

        for (ServerPlayer sp : s.level.players()) {
            if (sp.blockPosition().distSqr(s.pos) <= 32.0 * 32.0) {
                sp.connection.send(new ClientboundSetTitlesAnimationPacket(4, 34, 10));
                sp.connection.send(new ClientboundSetTitleTextPacket(title));
                sp.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                sp.sendOverlayMessage(actionBar);
                bossBar.addPlayer(sp);
                sendChatCard(sp, s.finalCategory, s.eventNameKey, stats);
            }
        }

        ACTIVE_BARS.add(new TimedBossBar(bossBar, 65));
        s.onReveal.run();
    }

    private static void sendChatCard(ServerPlayer sp, Category category, String eventNameKey, PlayerStats stats) {
        Component border = Component.literal("✦ ━━━━━━━━ LUCKY BLOCK ━━━━━━━━ ✦").withStyle(category.color);
        Component line1 = Component.empty()
                .append(Component.literal("  結果: ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(category.titleKey).withStyle(category.color, ChatFormatting.BOLD))
                .append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(eventNameKey).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        Component line2 = Component.empty()
                .append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(category.flavorKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        sp.sendSystemMessage(border); // 要検証: 26.3 ServerPlayer チャット送信メソッド
        sp.sendSystemMessage(line1);
        sp.sendSystemMessage(line2);
        if (stats != null) {
            sp.sendSystemMessage(
                    Component.literal("  [記録] 通算開封: " + stats.totalOpened + "回 / 大当たり系: " + stats.jackpotCount + "回")
                            .withStyle(ChatFormatting.DARK_AQUA)
            );
        }
    }

    private static void spawnCategoryParticles(ServerLevel level, BlockPos pos, Category category, RandomSource random) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        switch (category) {
            case RARE -> {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, true, false, cx, cy, cz, 55, 1.0, 1.1, 1.0, 0.35);
                level.sendParticles(ParticleTypes.END_ROD, true, false, cx, cy + 0.4, cz, 40, 0.9, 1.0, 0.9, 0.14);
            }
            case JACKPOT -> {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, true, false, cx, cy, cz, 30, 0.7, 0.8, 0.7, 0.25);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, true, false, cx, cy + 0.3, cz, 22, 0.8, 0.6, 0.8, 0.05);
            }
            case BAD -> {
                level.sendParticles(ParticleTypes.SMOKE, true, false, cx, cy + 0.2, cz, 26, 0.7, 0.6, 0.7, 0.06);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, true, false, cx, cy + 0.5, cz, 14, 0.7, 0.5, 0.7, 0.02);
            }
            case FUN -> {
                level.sendParticles(ParticleTypes.NOTE, true, false, cx, cy + 0.4, cz, 24, 0.9, 0.7, 0.9, 0.12);
                level.sendParticles(ParticleTypes.FIREWORK, true, false, cx, cy + 0.3, cz, 20, 0.7, 0.7, 0.7, 0.15);
            }
        }
        if (random.nextBoolean()) {
            level.sendParticles(ParticleTypes.POOF, true, false, cx, cy, cz, 10, 0.4, 0.4, 0.4, 0.04);
        }
    }
}
