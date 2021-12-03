package com.lothrazar.tamedrespawn.event;

import com.lothrazar.tamedrespawn.ConfigRegistry;
import com.lothrazar.tamedrespawn.ModTamedRespawn;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
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
    Level world = entity.level;
    if (hasLives(entity) && entity instanceof TamableAnimal) {
      TamableAnimal tamed = (TamableAnimal) entity;
      LivingEntity owner = tamed.getOwner();
      if (tamed.isTame() && owner != null
          && haveSameDimension(tamed, owner)
          && owner instanceof Player) {
        event.setCanceled(true);
        Player player = (Player) owner;
        healAndRespawn(tamed, player);
        tamed.setOrderedToSit(true); // TODO: mappings setSitting
        this.sendDeathChat(player, ModTamedRespawn.MODID + ".tamed.message", tamed);
      }
    }
    else if (entity instanceof AbstractHorse) {
      AbstractHorse horse = (AbstractHorse) entity;
      if (horse.getOwnerUUID() != null) {
        Player player = world.getPlayerByUUID(horse.getOwnerUUID());
        if (player != null && haveSameDimension(horse, player)) {
          event.setCanceled(true);
          healAndRespawn(horse, player);
          this.sendDeathChat(player, ModTamedRespawn.MODID + ".horse.message", horse);
        }
      }
    }
  }

  private void sendDeathChat(Player player, String string, Entity ent) {
    String petname = ent.getDisplayName().getString();
    if (ConfigRegistry.DOCHAT.get()) {
      TranslatableComponent t = new TranslatableComponent(string);
      t.withStyle(ChatFormatting.LIGHT_PURPLE);
      if (petname != null) {
        t.append(" " + petname);
      }
      player.displayClientMessage(t, false);
      //lives remaining text
      t = new TranslatableComponent("tamedrespawn.howmany");
      t.withStyle(ChatFormatting.DARK_PURPLE);
      player.displayClientMessage((new TextComponent("[" + getLives(ent) + "]")).append(t).withStyle(ChatFormatting.DARK_PURPLE), false);
    }
  }

  private void sendGiveChat(Player player, String string, Entity ent) {
    if (ConfigRegistry.DOCHAT.get()) {
      TranslatableComponent t = new TranslatableComponent(string);
      t.withStyle(ChatFormatting.LIGHT_PURPLE);
      player.displayClientMessage(t, false);
      //lives remaining text
      t = new TranslatableComponent("tamedrespawn.howmany");
      t.withStyle(ChatFormatting.DARK_PURPLE);
      player.displayClientMessage((new TextComponent("[" + getLives(ent) + "]")).append(t).withStyle(ChatFormatting.DARK_PURPLE), false);
    }
  }

  @SubscribeEvent
  public void onEntityInteract(EntityInteract event) {
    Entity target = event.getTarget();
    if (ConfigRegistry.isEntityIgnored(target)) {
      return;
    }
    Player player = event.getPlayer();
    ItemStack held = event.getItemStack();
    if (!player.level.isClientSide && isLifeGain(held) && target instanceof LivingEntity) {
      gainLife((LivingEntity) target);
      this.sendGiveChat(player, ModTamedRespawn.MODID + ".tamed.gained", target);
    }
  }

  private boolean isLifeGain(ItemStack held) {
    return held.getItem().getRegistryName().toString().equalsIgnoreCase(ConfigRegistry.ITEMREVIEV.get());
  }

  private void healAndRespawn(LivingEntity tamed, Player owner) {
    consumeLife(tamed);
    if (ConfigRegistry.DOTP.get()) {
      BlockPos pos = owner.blockPosition();
      tamed.teleportTo(pos.getX(), pos.getY(), pos.getZ());
    }
    if (ConfigRegistry.DOHEAL.get()) {
      tamed.heal(tamed.getMaxHealth());
      tamed.setHealth(tamed.getMaxHealth());
      tamed.removeAllEffects();
      tamed.clearFire();
    }
    //apply potion effects - possibly empty
    for (String reg : ConfigRegistry.potionIds()) {
      MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(reg));
      if (eff != null) {
        tamed.addEffect(new MobEffectInstance(eff, 30, 1));
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

  public static String dimensionToString(Level world) {
    //example: returns "minecraft:overworld" resource location
    return world.dimension().location().toString();
  }

  private boolean haveSameDimension(LivingEntity tamed, LivingEntity owner) {
    return dimensionToString(tamed.level).equalsIgnoreCase(dimensionToString(owner.level));
  }
}
