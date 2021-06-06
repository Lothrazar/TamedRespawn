package com.lothrazar.tamedrespawn;

import com.lothrazar.tamedrespawn.event.TamedEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(ModTamedRespawn.MODID)
public class ModTamedRespawn {

  public static final String MODID = "tamedrespawn";

  public ModTamedRespawn() {
    ConfigRegistry.setup(FMLPaths.CONFIGDIR.get().resolve(MODID + ".toml"));
    MinecraftForge.EVENT_BUS.register(new TamedEvents());
  }
}
