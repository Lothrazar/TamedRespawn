package com.lothrazar.tamedrespawn.event;

import com.lothrazar.tamedrespawn.ConfigRegistry;
import com.lothrazar.tamedrespawn.ModTamedRespawn;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class TamedEvents {

  private static final int MAX_LIVES = 64;
  private static final String NBT_LIVES = ModTamedRespawn.MODID + ".lives";

  @SubscribeEvent
  public void onLivingDeathEvent(LivingDeathEvent event) {
    LivingEntity entity = event.getEntityLiving();
    if (ConfigRegistry.isEntityIgnored(entity)) {
      return;
    }
    World world = entity.world;
    if (hasLives(entity) && entity instanceof TameableEntity) {
      TameableEntity tamed = (TameableEntity) entity;
      LivingEntity owner = tamed.getOwner();
      if (tamed.isTamed() && owner != null
          && haveSameDimension(tamed, owner)
          && owner instanceof PlayerEntity) {
        event.setCanceled(true);
        PlayerEntity player = (PlayerEntity) owner;
        healAndRespawn(tamed, player);
        tamed.func_233687_w_(true); // TODO: mappings setSitting
        this.sendDeathChat(player, ModTamedRespawn.MODID + ".tamed.message", tamed);
      }
    }
    else if (entity instanceof AbstractHorseEntity) {
      AbstractHorseEntity horse = (AbstractHorseEntity) entity;
      if (horse.getOwnerUniqueId() != null) {
        PlayerEntity player = world.getPlayerByUuid(horse.getOwnerUniqueId());
        if (player != null && haveSameDimension(horse, player)) {
          event.setCanceled(true);
          healAndRespawn(horse, player);
          this.sendDeathChat(player, ModTamedRespawn.MODID + ".horse.message", horse);
        }
      }
    }
  }

  private void sendDeathChat(PlayerEntity player, String string, Entity ent) {
    String petname = ent.getDisplayName().getString();
    if (ConfigRegistry.DOCHAT.get()) {
      TranslationTextComponent t = new TranslationTextComponent(string);
      t.mergeStyle(TextFormatting.LIGHT_PURPLE);
      if (petname != null) {
        t.appendString(" " + petname);
      }
      player.sendStatusMessage(t, false);
      //lives remaining text
      t = new TranslationTextComponent("tamedrespawn.howmany");
      t.mergeStyle(TextFormatting.DARK_PURPLE);
      player.sendStatusMessage((new StringTextComponent("[" + getLives(ent) + "]")).append(t).mergeStyle(TextFormatting.DARK_PURPLE), false);
    }
  }

  private void sendGiveChat(PlayerEntity player, String string, Entity ent) {
    if (ConfigRegistry.DOCHAT.get()) {
      TranslationTextComponent t = new TranslationTextComponent(string);
      t.mergeStyle(TextFormatting.LIGHT_PURPLE);
      player.sendStatusMessage(t, false);
      //lives remaining text
      t = new TranslationTextComponent("tamedrespawn.howmany");
      t.mergeStyle(TextFormatting.DARK_PURPLE);
      player.sendStatusMessage((new StringTextComponent("[" + getLives(ent) + "]")).append(t).mergeStyle(TextFormatting.DARK_PURPLE), false);
    }
  }

  @SubscribeEvent
  public void onEntityInteract(EntityInteract event) {
    Entity target = event.getTarget();
    if (ConfigRegistry.isEntityIgnored(target)) {
      return;
    }
    PlayerEntity player = event.getPlayer();
    ItemStack held = event.getItemStack();
    if (!player.world.isRemote && isLifeGain(held) && target instanceof LivingEntity) {
      gainLife((LivingEntity) target);
      this.sendGiveChat(player, ModTamedRespawn.MODID + ".tamed.gained", target);
    }
  }

  private boolean isLifeGain(ItemStack held) {
    return held.getItem().getRegistryName().toString().equalsIgnoreCase(ConfigRegistry.ITEMREVIEV.get());
  }

  private void healAndRespawn(LivingEntity tamed, PlayerEntity owner) {
    consumeLife(tamed);
    if (ConfigRegistry.DOTP.get()) {
      BlockPos pos = owner.getPosition();
      tamed.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());
    }
    if (ConfigRegistry.DOHEAL.get()) {
      tamed.heal(tamed.getMaxHealth());
      tamed.setHealth(tamed.getMaxHealth());
      tamed.clearActivePotions();
      tamed.extinguish();
    }
    //apply potion effects - possibly empty
    for (String reg : ConfigRegistry.potionIds()) {
      Effect eff = ForgeRegistries.POTIONS.getValue(ResourceLocation.tryCreate(reg));
      if (eff != null) {
        tamed.addPotionEffect(new EffectInstance(eff, 30, 1));
      }
    }
  }

  private void consumeLife(LivingEntity tamed) {
    int lives = getLives(tamed);
    if (lives > 0) {
      setLife(tamed, lives - 1);
    }
  }

  private void gainLife(LivingEntity tamed) {
    int lives = getLives(tamed);
    if (lives < MAX_LIVES) {
      setLife(tamed, lives + 1);
    }
  }

  private boolean hasLives(Entity tamed) {
    return getLives(tamed) > 0;
  }

  private int getLives(Entity tamed) {
    int lives = tamed.getPersistentData().getInt(NBT_LIVES);
    return lives;
  }

  private void setLife(Entity tamed, int lv) {
    tamed.getPersistentData().putInt(NBT_LIVES, lv);
  }

  public static String dimensionToString(World world) {
    //example: returns "minecraft:overworld" resource location
    return world.getDimensionKey().getLocation().toString();
  }

  private boolean haveSameDimension(LivingEntity tamed, LivingEntity owner) {
    return dimensionToString(tamed.world).equalsIgnoreCase(dimensionToString(owner.world));
  }
}
