package com.lothrazar.tamedrespawn;

import org.apache.logging.log4j.Logger;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = ModMain.MODID)
public class ModMain {

  public static final String MODID = "tamedrespawn";
  private static Logger logger;

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    MinecraftForge.EVENT_BUS.register(this);
  }

  private void healAndRespawn(EntityLivingBase tamed, EntityLivingBase owner) {
    BlockPos pos = owner.getPosition();
    World world = tamed.world;
    tamed.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());
    tamed.heal(tamed.getMaxHealth());
    tamed.setHealth(tamed.getMaxHealth());
    tamed.clearActivePotions();
    tamed.extinguish();

    for (double i = 0; i < 10; i++) {
      double randx = MathHelper.nextDouble(world.rand, -1 * i / 10, i / 10);
      double randz = MathHelper.nextDouble(world.rand, -1 * i / 10, i / 10);
      world.spawnParticle(EnumParticleTypes.HEART,
          pos.getX(), pos.getY(), pos.getZ(),
          randx,
          0.5 + randx * randz,
          randz);
    }
  }
  @SubscribeEvent
  public void onLivingDeathEvent(LivingDeathEvent event) {
    EntityLivingBase entity = event.getEntityLiving();
    World world = entity.world;
    if (entity instanceof EntityTameable) {
      EntityTameable tamed = (EntityTameable) entity;
      if (tamed.isTamed() && tamed.getOwner() != null) {
        event.setCanceled(true);
        healAndRespawn(tamed, tamed.getOwner());
        tamed.getOwner().sendMessage(new TextComponentTranslation(MODID + ".tamed.message"));
      }
    }
    else if (entity instanceof AbstractHorse) {
      AbstractHorse horse = (AbstractHorse) entity;
      if (horse.getOwnerUniqueId() != null) {
        EntityPlayer player = world.getPlayerEntityByUUID(horse.getOwnerUniqueId());
        if (player != null) {
          event.setCanceled(true);
          healAndRespawn(horse, player);
          player.sendMessage(new TextComponentTranslation(MODID + ".horse.message"));
        }
      }
    }
    else {
      //logger.info("NOT TAMED " + entity.getName());
    }
  }
}
