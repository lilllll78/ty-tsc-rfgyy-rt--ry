package fr.chestesp;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auto farm des Respawn Anchors : va vers l'ancre la plus proche, la mine avec
 * la meilleure pioche de la hotbar, puis va ramasser l'item au sol.
 * Pas de pathfinding : marche en ligne droite + saute si bloqué.
 */
public final class AnchorFarm {

    private static final double REACH = 4.4;
    private static final double MAX_RANGE = 48.0;
    private static final double ITEM_RANGE = 16.0;
    private static final int ITEM_TIMEOUT = 200; // ticks avant d'abandonner un item inaccessible

    private static final Set<Integer> ignoredItems = new HashSet<>();
    private static int currentItem = -1;
    private static int currentItemTicks = 0;
    private static boolean keysHeld = false;

    private AnchorFarm() {}

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;

        if (!Config.anchorFarm || player == null || world == null
                || mc.interactionManager == null || mc.currentScreen != null) {
            releaseKeys(mc);
            return;
        }

        releaseKeys(mc);

        // 1) Item d'ancre au sol à ramasser en priorité
        ItemEntity item = findItem(world, player);
        if (item != null) {
            if (item.getId() != currentItem) {
                currentItem = item.getId();
                currentItemTicks = 0;
            }
            if (++currentItemTicks > ITEM_TIMEOUT) {
                ignoredItems.add(item.getId());
                currentItem = -1;
                return;
            }
            walkTo(mc, player, item.getPos());
            return;
        }
        currentItem = -1;

        // 2) Ancre la plus proche
        BlockPos target = null;
        double best = Double.MAX_VALUE;
        for (BlockPos p : ChestEspClient.anchors) {
            if (!world.getBlockState(p).isOf(Blocks.RESPAWN_ANCHOR)) continue;
            double d = p.toCenterPos().distanceTo(player.getPos());
            if (d < best && d <= MAX_RANGE) {
                best = d;
                target = p;
            }
        }
        if (target == null) return;

        Vec3d center = Vec3d.ofCenter(target);
        Vec3d eye = player.getEyePos();

        if (eye.distanceTo(center) > REACH) {
            walkTo(mc, player, center);
            return;
        }

        // 3) Miner
        BlockState state = world.getBlockState(target);
        int slot = findToolSlot(player, state);
        if (slot < 0) {
            player.sendMessage(Text.literal("AutoFarm : il faut une pioche diamant/netherite dans la hotbar"), true);
            Config.anchorFarm = false;
            return;
        }
        player.getInventory().selectedSlot = slot;

        lookAt(player, center);
        Direction side = Direction.getFacing(eye.x - center.x, eye.y - center.y, eye.z - center.z);
        mc.interactionManager.updateBlockBreakingProgress(target, side);
        player.swingHand(Hand.MAIN_HAND);
    }

    private static ItemEntity findItem(ClientWorld world, ClientPlayerEntity player) {
        Box box = player.getBoundingBox().expand(ITEM_RANGE);
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box,
                e -> e.getStack().isOf(Items.RESPAWN_ANCHOR) && !ignoredItems.contains(e.getId()));
        ItemEntity best = null;
        double bd = Double.MAX_VALUE;
        for (ItemEntity e : items) {
            double d = e.squaredDistanceTo(player);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private static int findToolSlot(ClientPlayerEntity player, BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 0f;
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isEmpty() || !s.isSuitableFor(state)) continue;
            float sp = s.getMiningSpeedMultiplier(state);
            if (sp > bestSpeed) {
                bestSpeed = sp;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static void walkTo(MinecraftClient mc, ClientPlayerEntity player, Vec3d dest) {
        Vec3d eye = player.getEyePos();
        double dx = dest.x - eye.x, dz = dest.z - eye.z;
        player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        player.setPitch(0f);
        mc.options.forwardKey.setPressed(true);
        mc.options.sprintKey.setPressed(true);
        if (player.horizontalCollision && player.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
        }
        keysHeld = true;
    }

    private static void lookAt(ClientPlayerEntity player, Vec3d target) {
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        player.setPitch((float) -Math.toDegrees(Math.atan2(dy, h)));
    }

    private static void releaseKeys(MinecraftClient mc) {
        if (!keysHeld) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        keysHeld = false;
    }
}
