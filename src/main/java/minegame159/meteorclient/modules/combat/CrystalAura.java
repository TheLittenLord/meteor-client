package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 31/04/2020
//Updated by squidoodly 19/06/2020
//Updated by squidoodly 24/07/2020
//Updated by squidoodly 26-28/07/2020

import com.google.common.collect.Streams;
import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CrystalAura extends ToggleModule {
    public enum Mode{
        safe,
        suicide
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The distance in a single direction the crystals get placed.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The distance in a single direction the crystals get broken.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("The way crystals are placed")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Mode> breakMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("break-mode")
            .description("The way crystals are broken.")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to crystals for you.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> spoofChange = sgGeneral.add(new BoolSetting.Builder()
            .name("spoof-change")
            .description("Spoofs item change to crystal.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage the crystal will place")
            .defaultValue(5.5)
            .build()
    );

    private final Setting<Double> maxDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("max-damage")
            .description("The maximum self-damage allowed")
            .defaultValue(3)
            .build()
    );

    private final Setting<Boolean> strict = sgPlace.add(new BoolSetting.Builder()
            .name("strict")
            .description("Helps compatibility with some servers.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minHealth = sgPlace.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health you have to be for it to place")
            .defaultValue(15)
            .build()
    );

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Attack through walls")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
            .name("place")
            .description("Allow it to place cystals")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay ticks between placements.")
            .defaultValue(2)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> smartDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("smart-delay")
            .description("Reduces crystal consumption when doing large amounts of damage.(Can tank performance on lower end PCs)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> facePlace = sgGeneral.add(new BoolSetting.Builder()
            .name("face-place")
            .description("Will face place when target is below a certain health or their armour is low.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> facePlaceHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("face-place-health")
            .description("The health required to face place")
            .defaultValue(5)
            .min(1)
            .max(20)
            .build()
    );

    private final Setting<Double> facePlaceDurability = sgGeneral.add(new DoubleSetting.Builder()
            .name("face-place-durability")
            .description("The durability required to face place (in percent)")
            .defaultValue(2)
            .min(1)
            .max(100)
            .sliderMax(100)
            .build()
    );

    private final Setting<Boolean> spamFacePlace = sgGeneral.add(new BoolSetting.Builder()
            .name("spam-face-place")
            .description("Places faster when someone is below the face place health (requires smart delay)")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> healthDifference = sgGeneral.add(new DoubleSetting.Builder()
            .name("damage-increase")
            .description("The damage increase for smart delay to work.")
            .defaultValue(5)
            .min(0)
            .max(20)
            .build()
    );

    private final Setting<Boolean> antiWeakness = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-weakness")
            .description("Switches to tools when you have weakness")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
            .name("render")
            .description("Render a box where it is placing a crystal.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> renderColor = sgGeneral.add(new ColorSetting.Builder()
            .name("render-color")
            .description("Render color.")
            .defaultValue(new Color(25, 225, 225))
            .build()
    );

    private final Color sideColor = new Color();

    public CrystalAura() {
        super(Category.Combat, "crystal-aura", "Places and breaks end crystals automatically");
    }

    private int preSlot;
    private int delayLeft = delay.get();
    private Vec3d bestBlock;
    private double bestDamage;
    private BlockPos playerPos;
    private Vec3d pos;
    private double lastDamage = 0;
    private boolean shouldFacePlace = false;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    @Override
    public void onActivate() {
        preSlot = -1;
    }

    @Override
    public void onDeactivate() {
        if (preSlot != -1) mc.player.inventory.selectedSlot = preSlot;

        for (RenderBlock renderBlock : renderBlocks) {
            renderBlockPool.free(renderBlock);
        }
        renderBlocks.clear();
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        for (Iterator<RenderBlock> it = renderBlocks.iterator(); it.hasNext();) {
            RenderBlock renderBlock = it.next();

            if (renderBlock.shouldRemove()) {
                it.remove();
                renderBlockPool.free(renderBlock);
            }
        }

        delayLeft --;
        shouldFacePlace = false;
        if (getTotalHealth(mc.player) <= minHealth.get() && mode.get() != Mode.suicide) return;
        Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof EndCrystalEntity)
                .filter(entity -> entity.distanceTo(mc.player) <= breakRange.get())
                .filter(Entity::isAlive)
                .filter(entity -> ignoreWalls.get() || mc.player.canSee(entity))
                .filter(entity -> !(breakMode.get() == Mode.safe)
                        || (getTotalHealth(mc.player) - DamageCalcUtils.crystalDamage(mc.player, entity.getPos()) > minHealth.get()
                        && DamageCalcUtils.crystalDamage(mc.player, entity.getPos()) < maxDamage.get()))
                .min(Comparator.comparingDouble(o -> o.distanceTo(mc.player)))
                .ifPresent(entity -> {
                    int preSlot = mc.player.inventory.selectedSlot;
                    if(mc.player.getActiveStatusEffects().containsKey(StatusEffects.WEAKNESS) && antiWeakness.get()){
                        for(int i = 0; i < 9; i++){
                            if(mc.player.inventory.getStack(i).getItem() instanceof SwordItem || mc.player.inventory.getStack(i).getItem() instanceof AxeItem){
                                mc.player.inventory.selectedSlot = i;
                            }
                        }
                    }

                    Vec3d vec1 = entity.getPos();
                    PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(Utils.getNeededYaw(vec1), Utils.getNeededPitch(vec1), mc.player.isOnGround());
                    mc.player.networkHandler.sendPacket(packet);

                    mc.interactionManager.attackEntity(mc.player, entity);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.player.inventory.selectedSlot = preSlot;
                });
        if (!smartDelay.get() && delayLeft > 0) return;
        if (place.get()) {
            Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream()
                    .filter(FriendManager.INSTANCE::attack)
                    .filter(entityPlayer -> !entityPlayer.getDisplayName().equals(mc.player.getDisplayName()))
                    .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) <= 10)
                    .filter(entityPlayer -> !entityPlayer.isCreative() && !entityPlayer.isSpectator() && entityPlayer.getHealth() > 0)
                    .collect(Collectors.toList())
                    .iterator();

            AbstractClientPlayerEntity target;
            if (validEntities.hasNext()) {
                target = validEntities.next();
            } else {
                return;
            }
            for (AbstractClientPlayerEntity i = null; validEntities.hasNext(); i = validEntities.next()) {
                if (i == null) continue;
                if (mc.player.distanceTo(i) < mc.player.distanceTo(target)) {
                    target = i;
                }
            }
            if (autoSwitch.get() && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
                int slot = InvUtils.findItemWithCount(Items.END_CRYSTAL).slot;
                if (slot != -1 && slot < 9) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = slot;
                }
            }
            Hand hand = Hand.MAIN_HAND;
            if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL)
                hand = Hand.OFF_HAND;
            else if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                return;
            }
            findValidBlocks(target);
            if (bestBlock == null) return;
            if (facePlace.get() && Math.sqrt(target.squaredDistanceTo(bestBlock)) <= 2) {
                if (target.getHealth() + target.getAbsorptionAmount() < facePlaceHealth.get())
                    shouldFacePlace = true;
                else {
                    Iterator<ItemStack> armourItems = target.getArmorItems().iterator();
                    for (ItemStack itemStack = null; armourItems.hasNext(); itemStack = armourItems.next()){
                        if (itemStack == null) continue;
                        if (!itemStack.isEmpty() && (((double)(itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage()) * 100) <= facePlaceDurability.get()){
                            shouldFacePlace = true;
                        }
                    }
                }
            }
            if (bestBlock != null && (bestDamage >= minDamage.get() || shouldFacePlace)) {
                if (!smartDelay.get()) {
                    delayLeft = delay.get();
                    placeBlock(bestBlock, hand);
                }else if (smartDelay.get() && (delayLeft <= 0 || bestDamage - lastDamage > healthDifference.get()
                        || (spamFacePlace.get() && shouldFacePlace))) {
                    lastDamage = bestDamage;
                    placeBlock(bestBlock, hand);
                    if (delayLeft <= 0) delayLeft = 10;
                }
            }
            if (spoofChange.get() && preSlot != mc.player.inventory.selectedSlot)
                mc.player.inventory.selectedSlot = preSlot;
        }
    }, EventPriority.HIGH);

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (render.get()) {
            sideColor.set(renderColor.get());
            sideColor.a = 45;

            for (RenderBlock renderBlock : renderBlocks) {
                renderBlock.render();
            }
        }
    });

    private void placeBlock(Vec3d block, Hand hand){
        float yaw = mc.player.yaw;
        float pitch = mc.player.pitch;
        Vec3d vec1 = block.add(0.5, 0.5, 0.5);
        PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(Utils.getNeededYaw(vec1), Utils.getNeededPitch(vec1), mc.player.isOnGround());
        mc.player.networkHandler.sendPacket(packet);

        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(block, Direction.UP, new BlockPos(block), false));
        mc.player.swingHand(Hand.MAIN_HAND);
        packet = new PlayerMoveC2SPacket.LookOnly(yaw, pitch, mc.player.isOnGround());
        mc.player.networkHandler.sendPacket(packet);
        mc.player.yaw = yaw;
        mc.player.pitch = pitch;

        if (render.get()) {
            RenderBlock renderBlock = renderBlockPool.get();
            renderBlock.reset(block);
            renderBlocks.add(renderBlock);
        }
    }

    private void findValidBlocks(AbstractClientPlayerEntity target){
        bestBlock = null;
        playerPos = mc.player.getBlockPos();
        for(double i = playerPos.getX() - placeRange.get(); i < playerPos.getX() + placeRange.get(); i++){
            for(double j = playerPos.getZ() - placeRange.get(); j < playerPos.getZ() + placeRange.get(); j++){
                for(double k = playerPos.getY() - 3; k < playerPos.getY() + 3; k++){
                    pos = new Vec3d(i, k, j);
                    if((mc.world.getBlockState(new BlockPos(pos)).getBlock() == Blocks.BEDROCK
                            || mc.world.getBlockState(new BlockPos(pos)).getBlock() == Blocks.OBSIDIAN)
                            && isEmpty(new BlockPos(pos.add(0, 1, 0)))){
                        if (!strict.get()) {
                            if (bestBlock == null) {
                                bestBlock = pos;
                                bestDamage = DamageCalcUtils.crystalDamage(target, bestBlock.add(0.5, 1, 0.5));
                            }
                            if (bestDamage < DamageCalcUtils.crystalDamage(target, pos.add(0.5, 1, 0.5))
                                    && (DamageCalcUtils.crystalDamage(mc.player, pos.add(0.5,1, 0.5)) < maxDamage.get() || mode.get() == Mode.suicide)) {
                                bestBlock = pos;
                                bestDamage = DamageCalcUtils.crystalDamage(target, bestBlock.add(0.5, 1, 0.5));
                            }
                        } else if (strict.get() && isEmpty(new BlockPos(pos.add(0, 2, 0)))) {
                            if (bestBlock == null) {
                                bestBlock = pos;
                                bestDamage = DamageCalcUtils.crystalDamage(target, bestBlock.add(0.5, 1, 0.5));
                            }
                            if (bestDamage
                                    < DamageCalcUtils.crystalDamage(target, pos.add(0.5, 1, 0.5))
                                    && (DamageCalcUtils.crystalDamage(mc.player, pos.add( 0.5, 1, 0.5)) < maxDamage.get()) || mode.get() == Mode.suicide) {
                                bestBlock = pos;
                                bestDamage = DamageCalcUtils.crystalDamage(target, bestBlock.add(0.5, 1, 0.5));
                            }
                        }
                    }
                }
            }
        }
    }

    private float getTotalHealth(PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    private boolean isEmpty(BlockPos pos) {
        return mc.world.isAir(pos) && mc.world.getOtherEntities(null, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D)).isEmpty();
    }

    private class RenderBlock {
        private int x, y, z;
        private int timer;

        public void reset(Vec3d pos) {
            x = MathHelper.floor(pos.getX());
            y = MathHelper.floor(pos.getY());
            z = MathHelper.floor(pos.getZ());
            timer = 4;
        }

        public boolean shouldRemove() {
            if (timer <= 0) return true;
            timer--;
            return false;
        }

        public void render() {
            ShapeBuilder.boxSides(x, y, z, x+1, y+1, z+1, sideColor);
            ShapeBuilder.boxEdges(x, y, z, x+1, y+1, z+1, renderColor.get());
        }
    }
}
