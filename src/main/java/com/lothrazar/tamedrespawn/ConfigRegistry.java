package com.lothrazar.tamedrespawn;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class ConfigRegistry {

  private static final ForgeConfigSpec.Builder CFG = new ForgeConfigSpec.Builder();
  private static ForgeConfigSpec COMMON_CONFIG;
  public static BooleanValue DOCHAT;
  private static ConfigValue<List<? extends String>> EFFECTS;
  //  private static ConfigValue<List<? extends String>> ENTITIES;
  static {
    initConfig();
  }

  private static void initConfig() {
    CFG.push(ModTamedRespawn.MODID);
    //
    DOCHAT = CFG.comment("Send chat popup on revival").define("message", true);
    //
    ArrayList<String> ptns = new ArrayList<String>();
    ptns.add("minecraft:regeneration");
    ptns.add("minecraft:weakness");
    EFFECTS = CFG.comment("Potions applied after tamed respawn").defineList("potions", ptns, it -> it instanceof String);
    ArrayList<String> ents = new ArrayList<String>();
    //    ENTITIES = CFG.comment("Entities to block for respawning, each entry must look like \"minecraft:horse\".  (will fail unless entity exists and is tamed by a valid player)")
    //        .defineList("ignoredEntities", ents, it -> it instanceof String);
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
  public static List<String> potionIds() {
    return (List<String>) EFFECTS.get();
  }
  //    doWeakness = config.getBoolean("weakness", category, true, "Apply weakness on revival");
  //    doRegen = config.getBoolean("regen", category, true, "Apply regen on revival");
  //    doChat = config.getBoolean("chatMessages", category, true, "Send chat popup on revival");
}
