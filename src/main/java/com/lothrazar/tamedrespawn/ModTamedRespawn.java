package com.lothrazar.tamedrespawn;

import org.apache.logging.log4j.Logger;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = ModTamedRespawn.MODID, certificateFingerprint = "@FINGERPRINT@", updateJSON = "https://raw.githubusercontent.com/Lothrazar/TamedRespawn/master/update.json")
public class ModTamedRespawn {

  public static final String MODID = "tamedrespawn";
  private static Logger logger;
  private Configuration config;
  private boolean doWeakness;
  private boolean doChat;
  private boolean doRegen;
  private boolean blockBreedingHorses;
  private boolean blockBreedingPets;

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    logger = event.getModLog();
    config = new Configuration(event.getSuggestedConfigurationFile());
    config.load();
    String category = MODID;
    doWeakness = config.getBoolean("weakness", category, true, "Apply weakness on revival");
    doRegen = config.getBoolean("regen", category, true, "Apply regen on revival");
    doChat = config.getBoolean("chatMessages", category, true, "Send chat popup on revival");
    blockBreedingHorses = config.getBoolean("blockBreedingHorses", category, true, "If true no breeding will happen");
    blockBreedingPets = config.getBoolean("blockBreedingPets", category, true, "If true no breeding will happen");
    config.save();
    MinecraftForge.EVENT_BUS.register(this);
  }

  private void healAndRespawn(EntityLivingBase tamed, EntityPlayer owner) {
    BlockPos pos = owner.getPosition();
    tamed.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());
    tamed.heal(tamed.getMaxHealth());
    tamed.setHealth(tamed.getMaxHealth());
    tamed.clearActivePotions();
    tamed.extinguish();
    if (this.doRegen) {
      tamed.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 30, 1));
    }
    if (this.doWeakness) {
      tamed.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 10, 3));
    }
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
  public void onBabyEntitySpawnEvent(BabyEntitySpawnEvent event) {
    //then you cannot breed horses or wolves
    if (blockBreedingHorses && event.getChild() instanceof AbstractHorse) {
      event.setCanceled(true);
    }
    if (blockBreedingPets && event.getChild() instanceof EntityTameable) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onLivingDeathEvent(LivingDeathEvent event) {
    EntityLivingBase entity = event.getEntityLiving();
    World world = entity.world;
    if (entity instanceof EntityTameable) {
      EntityTameable tamed = (EntityTameable) entity;
      EntityLivingBase owner = tamed.getOwner();
      if (tamed.isTamed() && owner != null
          && owner.dimension == tamed.dimension
          && owner instanceof EntityPlayer) {
        event.setCanceled(true);
        EntityPlayer player = (EntityPlayer) owner;
        healAndRespawn(tamed, player);
        //        tamed.setSitting(true);
        if (doChat) {
          player.sendStatusMessage(new TextComponentTranslation(I18n.translateToLocal(MODID + ".tamed.message")), true);
        }
      }
    }
    else if (entity instanceof AbstractHorse) {
      AbstractHorse horse = (AbstractHorse) entity;
      if (horse.getOwnerUniqueId() != null) {
        EntityPlayer player = world.getPlayerEntityByUUID(horse.getOwnerUniqueId());
        if (player != null
            && player.dimension == horse.dimension) {
          event.setCanceled(true);
          healAndRespawn(horse, player);
          if (doChat) {
            player.sendStatusMessage(new TextComponentTranslation(I18n.translateToLocal(MODID + ".horse.message")), true);
          }
        }
      }
    }
  }
}
