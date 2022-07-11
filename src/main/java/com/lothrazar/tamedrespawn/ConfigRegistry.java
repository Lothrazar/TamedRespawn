package com.lothrazar.tamedrespawn;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.lothrazar.tamedrespawn.util.UtilString;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.registries.ForgeRegistries;

public class ConfigRegistry {

  private static final ForgeConfigSpec.Builder CFG = new ForgeConfigSpec.Builder();
  private static ForgeConfigSpec COMMON_CONFIG;
  public static BooleanValue DOCHAT;
  private static ConfigValue<List<? extends String>> EFFECTS;
  private static ConfigValue<List<? extends String>> ENTITIES;
  public static BooleanValue DOHEAL;
  public static BooleanValue DOTP;
  static {
    initConfig();
  }

  private static void initConfig() {
    CFG.push(ModTamedRespawn.MODID);
    //
    DOCHAT = CFG.comment("\r\nSend chat popup on revival").define("doMessage", true);
    DOHEAL = CFG.comment("\r\nSend chat popup on revival").define("doHeal", true);
    DOTP = CFG.comment("\r\nSend chat popup on revival").define("doTeleport", true);
    //
    ArrayList<String> ptns = new ArrayList<String>();
    ptns.add("minecraft:regeneration");
    ptns.add("minecraft:weakness");
    EFFECTS = CFG.comment("\r\nPotions applied after tamed respawn").defineList("doPotions", ptns, it -> it instanceof String);
    ArrayList<String> ents = new ArrayList<String>();
    ents.add("minecraft:mule");
    ENTITIES = CFG.comment("\r\nEntities to block for respawning, each entry must look like \"minecraft:horse\".  "
        + "By default compatible tamed entities are always affected by this mod unless listed here")
        .defineList("ignoredEntities", ents, it -> it instanceof String);
    //
    CFG.pop();
    COMMON_CONFIG = CFG.build();
  }

  public static void setup(Path path) {
    final CommentedFileConfig configData = CommentedFileConfig.builder(path)
        .sync()
        .autosave()
        .writingMode(WritingMode.REPLACE)
        .build();
    configData.load();
    COMMON_CONFIG.setConfig(configData);
  }

  @SuppressWarnings("unchecked")
  public static boolean isEntityIgnored(Entity in) {
    ResourceLocation inId = ForgeRegistries.ENTITIES.getKey(in.getType());
    List<String> entities = (List<String>) ENTITIES.get();
    return UtilString.isInList(entities, inId);
  }

  @SuppressWarnings("unchecked")
  public static List<String> potionIds() {
    return (List<String>) EFFECTS.get();
  }
}
