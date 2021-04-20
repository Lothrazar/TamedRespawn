package com.lothrazar.tamedrespawn;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class TamedEvents {

  private void healAndRespawn(LivingEntity tamed, PlayerEntity owner) {
    BlockPos pos = owner.getPosition();
    //    World world = tamed.world;
    tamed.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());
    tamed.heal(tamed.getMaxHealth());
    tamed.setHealth(tamed.getMaxHealth());
    tamed.clearActivePotions();
    tamed.extinguish();
    // TODO : CONFIG LIST
    //    Effect weak = ForgeRegistries.POTIONS.getValue(ResourceLocation.tryCreate("minecraft:weakness"));
    //    Effect reg = ForgeRegistries.POTIONS.getValue(ResourceLocation.tryCreate("minecraft:regeneration"));
    //    System.out.println("W" + weak);
    //Effects.REGENERATION.getRegistryName() 
    for (String reg : ConfigRegistry.potionIds()) {
      Effect eff = ForgeRegistries.POTIONS.getValue(ResourceLocation.tryCreate(reg));
      if (eff != null) {
        tamed.addPotionEffect(new EffectInstance(eff, 30, 1));
      }
    }
    //    if (ConfigRegistry.doRegen) {
    //    }
    //    if (ConfigRegistry.doWeakness) {
    //tamed.addPotionEffect(new EffectInstance(weak, 10, 3));
    //    }
    //    for (double i = 0; i < 10; i++) {
    //      double randx = MathHelper.nextDouble(world.rand, -1 * i / 10, i / 10);
    //      double randz = MathHelper.nextDouble(world.rand, -1 * i / 10, i / 10);
    //      world.playSound(owner, pos, SoundEvents.AMBIENT_CAVE, SoundCategory.NEUTRAL, 1, 1);
    //      world.spawnParticle(EnumParticleTypes.HEART,
    //          pos.getX(), pos.getY(), pos.getZ(),
    //          randx,
    //          0.5 + randx * randz,
    //          randz);
    //    }
  }

  @SubscribeEvent
  public void onLivingDeathEvent(LivingDeathEvent event) {
    LivingEntity entity = event.getEntityLiving();
    World world = entity.world;
    if (entity instanceof TameableEntity) {
      TameableEntity tamed = (TameableEntity) entity;
      LivingEntity owner = tamed.getOwner();
      if (tamed.isTamed() && owner != null
          && haveSameDimension(tamed, owner)
          && owner instanceof PlayerEntity) {
        event.setCanceled(true);
        PlayerEntity player = (PlayerEntity) owner;
        healAndRespawn(tamed, player);
        tamed.func_233687_w_(true); // TODO: mappings setSitting
        this.sendChat(player, ModTamedRespawn.MODID + ".tamed.message", tamed.getDisplayName().getString());
      }
    }
    else if (entity instanceof AbstractHorseEntity) {
      AbstractHorseEntity horse = (AbstractHorseEntity) entity;
      if (horse.getOwnerUniqueId() != null) {
        PlayerEntity player = world.getPlayerByUuid(horse.getOwnerUniqueId());
        if (player != null
            && haveSameDimension(horse, player)) {
          event.setCanceled(true);
          healAndRespawn(horse, player);
          this.sendChat(player, ModTamedRespawn.MODID + ".horse.message", horse.getDisplayName().getString());
        }
      }
    }
  }

  private void sendChat(PlayerEntity player, String string, String petname) {
    if (ConfigRegistry.DOCHAT.get()) {
      TranslationTextComponent t = new TranslationTextComponent(string);
      t.mergeStyle(TextFormatting.LIGHT_PURPLE);
      if (petname != null) {
        t.appendString(petname);
      }
      player.sendStatusMessage(t, true);
    }
  }

  public static String dimensionToString(World world) {
    //example: returns "minecraft:overworld" resource location
    return world.getDimensionKey().getLocation().toString();
  }

  private boolean haveSameDimension(AbstractHorseEntity tamed, LivingEntity owner) {
    return dimensionToString(tamed.world).equalsIgnoreCase(dimensionToString(owner.world));
  }

  private boolean haveSameDimension(TameableEntity tamed, LivingEntity owner) {
    return dimensionToString(tamed.world).equalsIgnoreCase(dimensionToString(owner.world));
  }
}
