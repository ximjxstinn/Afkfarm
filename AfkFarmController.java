package de.ximjxstinn.afkfarm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Herzstück der Mod: läuft jeden Client-Tick und kümmert sich um
 * Essen → Waffe prüfen → Mob angreifen (in dieser Reihenfolge).
 */
public class AfkFarmController {

    /** Essen, das nie automatisch gegessen wird (negative Effekte oder zu wertvoll). */
    private static final Set<Item> FOOD_BLACKLIST = Set.of(
            Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.PUFFERFISH,
            Items.CHICKEN, Items.SUSPICIOUS_STEW, Items.CHORUS_FRUIT,
            Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE
    );

    private final Random random = new Random();

    private boolean enabled = false;
    private int delayTicks = 0;

    // Essen
    private int eatingTicks = 0;
    private boolean eatingStarted = false;
    private int slotBeforeEat = -1;

    // Warnungen nur einmal anzeigen
    private boolean warnedNoFood = false;
    private boolean warnedNoWeapon = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle(MinecraftClient client) {
        if (enabled) disable(client, true);
        else enable(client);
    }

    public void enable(MinecraftClient client) {
        if (client.player == null) return;
        enabled = true;
        delayTicks = 0;
        warnedNoFood = false;
        warnedNoWeapon = false;
        actionbar(client, Text.literal("AFK-Farm: AN").formatted(Formatting.GREEN));
    }

    public void disable(MinecraftClient client, boolean message) {
        if (eatingTicks > 0) stopEating(client);
        enabled = false;
        if (message) actionbar(client, Text.literal("AFK-Farm: AUS").formatted(Formatting.RED));
    }

    public void tick(MinecraftClient client) {
        if (!enabled) return;
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) return;

        if (player.isDead()) {
            disable(client, false);
            actionbar(client, Text.literal("AFK-Farm aus – du bist gestorben").formatted(Formatting.RED));
            return;
        }

        // 1) Läuft gerade ein Essvorgang? Dann nur den weiterführen.
        if (eatingTicks > 0) {
            tickEating(client, player);
            return;
        }

        // 2) Hunger prüfen
        AfkConfig cfg = AfkFarmClient.CONFIG;
        if (cfg.autoEat && player.getHungerManager().getFoodLevel() < cfg.eatBelow && !player.isUsingItem()) {
            if (startEating(client, player)) return;
        }

        // 3) Angreifen
        tickAttack(client, player);
    }

    // ------------------------------------------------------------------
    // Angriff
    // ------------------------------------------------------------------

    private void tickAttack(MinecraftClient client, ClientPlayerEntity player) {
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }
        if (!ensureWeapon(client, player)) return;

        AfkConfig cfg = AfkFarmClient.CONFIG;
        LivingEntity target = cfg.autoAim ? findNearestTarget(client, player) : crosshairTarget(client);
        if (target == null) return;

        if (cfg.autoAim && !rotateTowards(player, target)) return; // erst hindrehen, dann schlagen

        // Nur mit vollem Angriffs-Cooldown schlagen = voller Schaden + normales Verhalten
        if (player.getAttackCooldownProgress(0.5F) < 1.0F) return;

        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        delayTicks = cfg.extraDelayMax > 0 ? random.nextInt(cfg.extraDelayMax + 1) : 0;
    }

    private boolean isValidTarget(Entity entity, ClientPlayerEntity player) {
        if (!(entity instanceof LivingEntity living) || entity == player) return false;
        if (!living.isAlive() || !(entity instanceof Monster)) return false;
        return !(AfkFarmClient.CONFIG.ignoreNamed && living.hasCustomName());
    }

    private LivingEntity crosshairTarget(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit && isValidTarget(entityHit.getEntity(), client.player)) {
            return (LivingEntity) entityHit.getEntity();
        }
        return null;
    }

    private LivingEntity findNearestTarget(MinecraftClient client, ClientPlayerEntity player) {
        double range = AfkFarmClient.CONFIG.range;
        Vec3d eye = player.getEyePos();
        Box searchBox = player.getBoundingBox().expand(range + 1.0);

        List<LivingEntity> candidates = client.world.getEntitiesByClass(
                LivingEntity.class, searchBox, e -> isValidTarget(e, player));

        return candidates.stream()
                .filter(e -> distanceToBox(eye, e.getBoundingBox()) <= range)
                .filter(player::canSee)
                .min(Comparator.comparingDouble(e -> distanceToBox(eye, e.getBoundingBox())))
                .orElse(null);
    }

    private static double distanceToBox(Vec3d point, Box box) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX);
        double y = MathHelper.clamp(point.y, box.minY, box.maxY);
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);
        return point.distanceTo(new Vec3d(x, y, z));
    }

    /**
     * Dreht den Spieler weich (nicht ruckartig) zum Ziel.
     * @return true, wenn er schon (fast) draufschaut
     */
    private boolean rotateTowards(ClientPlayerEntity player, LivingEntity target) {
        Vec3d eye = player.getEyePos();
        Vec3d aim = target.getBoundingBox().getCenter();
        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        float yawDiff = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchDiff = targetPitch - player.getPitch();

        // max. Drehung pro Tick mit etwas Zufall – sieht menschlicher aus
        float maxStep = 25.0F + random.nextFloat() * 15.0F;
        float yawStep = MathHelper.clamp(yawDiff, -maxStep, maxStep);
        float pitchStep = MathHelper.clamp(pitchDiff, -maxStep, maxStep);

        player.setYaw(player.getYaw() + yawStep);
        player.setPitch(MathHelper.clamp(player.getPitch() + pitchStep, -90.0F, 90.0F));

        return Math.abs(yawDiff - yawStep) < 12.0F && Math.abs(pitchDiff - pitchStep) < 12.0F;
    }

    // ------------------------------------------------------------------
    // Waffe / Haltbarkeit
    // ------------------------------------------------------------------

    private static boolean isWeapon(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
    }

    private static boolean durabilityOk(ItemStack stack) {
        if (!stack.isDamageable()) return true;
        int left = stack.getMaxDamage() - stack.getDamage();
        return left > AfkFarmClient.CONFIG.minDurability;
    }

    /** @return true, wenn mit dem aktuellen Item angegriffen werden darf */
    private boolean ensureWeapon(MinecraftClient client, ClientPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        if (durabilityOk(held)) {
            warnedNoWeapon = false;
            return true;
        }

        // Haltbarkeit zu niedrig → Ersatz in der Hotbar suchen
        if (AfkFarmClient.CONFIG.switchWeapon) {
            int slot = findWeaponSlot(player);
            if (slot >= 0) {
                player.getInventory().setSelectedSlot(slot);
                actionbar(client, Text.literal("Waffe fast kaputt – wechsle auf Slot " + (slot + 1)).formatted(Formatting.YELLOW));
                delayTicks = 2; // kurz warten, bis der Slot-Wechsel beim Server ist
                return false;
            }
        }

        if (!warnedNoWeapon) {
            warnedNoWeapon = true;
            actionbar(client, Text.literal("Waffe fast kaputt – Angriff pausiert!").formatted(Formatting.RED));
        }
        return false;
    }

    private static int findWeaponSlot(ClientPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!durabilityOk(stack)) continue;
            if (stack.isIn(ItemTags.SWORDS)) return i; // Schwert bevorzugt (schnellerer Cooldown)
            if (axeSlot < 0 && stack.isIn(ItemTags.AXES)) axeSlot = i;
        }
        return axeSlot;
    }

    // ------------------------------------------------------------------
    // Essen
    // ------------------------------------------------------------------

    private boolean startEating(MinecraftClient client, ClientPlayerEntity player) {
        int slot = findFoodSlot(player);
        if (slot < 0) {
            if (!warnedNoFood) {
                warnedNoFood = true;
                actionbar(client, Text.literal("Kein Essen in der Hotbar!").formatted(Formatting.GOLD));
            }
            return false;
        }
        warnedNoFood = false;

        PlayerInventory inv = player.getInventory();
        slotBeforeEat = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        client.options.useKey.setPressed(true);
        eatingTicks = 1;
        eatingStarted = false;
        return true;
    }

    private void tickEating(MinecraftClient client, ClientPlayerEntity player) {
        eatingTicks++;
        client.options.useKey.setPressed(true); // Rechtsklick "gedrückt halten"

        if (player.isUsingItem()) {
            eatingStarted = true;
        } else if (!eatingStarted && eatingTicks == 4) {
            // Fallback, falls z. B. ein Menü offen ist und Vanilla den Klick nicht auslöst
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        }

        boolean finished = eatingStarted && !player.isUsingItem();
        boolean timeout = eatingTicks > 80 || (!eatingStarted && eatingTicks > 12);
        if (finished || timeout) stopEating(client);
    }

    private void stopEating(MinecraftClient client) {
        client.options.useKey.setPressed(false);
        if (client.player != null && slotBeforeEat >= 0) {
            client.player.getInventory().setSelectedSlot(slotBeforeEat);
        }
        slotBeforeEat = -1;
        eatingTicks = 0;
        eatingStarted = false;
        delayTicks = 2;
    }

    private static int findFoodSlot(ClientPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        int best = -1;
        int bestNutrition = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || FOOD_BLACKLIST.contains(stack.getItem())) continue;
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food != null && food.nutrition() > bestNutrition) {
                best = i;
                bestNutrition = food.nutrition();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------

    private static void actionbar(MinecraftClient client, Text text) {
        if (client.player != null) client.player.sendMessage(text, true);
    }
}
