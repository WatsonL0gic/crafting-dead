/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.survival;

import java.util.Random;
import org.slf4j.Logger;
import com.craftingdead.core.event.GunEvent;
import com.craftingdead.core.event.LivingExtensionEvent;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.action.item.EntityItemAction;
import com.craftingdead.core.world.entity.extension.BasicLivingExtension;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.survival.client.ClientDist;
import com.craftingdead.survival.data.SurvivalItemTagsProvider;
import com.craftingdead.survival.data.SurvivalRecipeProvider;
import com.craftingdead.survival.data.loot.SurvivalLootTableProvider;
import com.craftingdead.survival.data.models.SurvivalModelProvider;
import com.craftingdead.survival.particles.SurvivalParticleTypes;
import com.craftingdead.survival.server.ServerDist;
import com.craftingdead.survival.world.action.SurvivalActionTypes;
import com.craftingdead.survival.world.effect.SurvivalMobEffects;
import com.craftingdead.survival.world.entity.SurvivalEntityTypes;
import com.craftingdead.survival.world.entity.SurvivalPlayerHandler;
import com.craftingdead.survival.world.entity.extension.DoctorZombieHandler;
import com.craftingdead.survival.world.entity.extension.GiantZombieHandler;
import com.craftingdead.survival.world.entity.extension.PoliceZombieHandler;
import com.craftingdead.survival.world.entity.extension.ZombieHandler;
import com.craftingdead.survival.world.entity.monster.DoctorZombieEntity;
import com.craftingdead.survival.world.entity.monster.FastZombie;
import com.craftingdead.survival.world.entity.monster.GiantZombie;
import com.craftingdead.survival.world.entity.monster.PoliceZombieEntity;
import com.craftingdead.survival.world.entity.monster.TankZombie;
import com.craftingdead.survival.world.entity.monster.WeakZombie;
import com.craftingdead.survival.world.item.SurvivalItems;
import com.craftingdead.survival.world.item.enchantment.SurvivalEnchantments;
import com.craftingdead.survival.world.level.block.SurvivalBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.data.ForgeBlockTagsProvider;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingPackSizeEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod(CraftingDeadSurvival.ID)
public class CraftingDeadSurvival {

  public static final String ID = "craftingdeadsurvival";

  private static final String H_CD_SERVER_CORE_ID = "hcdservercore";

  private static final Logger logger = LogUtils.getLogger();

  public static final ServerConfig serverConfig;
  public static final ForgeConfigSpec serverConfigSpec;

  static {
    var pair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
    serverConfigSpec = pair.getRight();
    serverConfig = pair.getLeft();
  }

  private static CraftingDeadSurvival instance;

  private final ModDist modDist;

  private final boolean immerseLoaded = ModList.get().isLoaded("craftingdeadimmerse");

  public CraftingDeadSurvival() {
    instance = this;

    this.modDist = DistExecutor.safeRunForDist(() -> ClientDist::new, () -> ServerDist::new);

    final var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    modEventBus.addListener(this::handleCommonSetup);
    modEventBus.addListener(this::handleEntityAttributeCreation);
    modEventBus.addListener(this::handleGatherData);

    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverConfigSpec);

    MinecraftForge.EVENT_BUS.register(this);

    SurvivalEnchantments.deferredRegister.register(modEventBus);
    SurvivalActionTypes.deferredRegister.register(modEventBus);
    SurvivalItems.deferredRegister.register(modEventBus);
    SurvivalMobEffects.deferredRegister.register(modEventBus);
    SurvivalEntityTypes.deferredRegister.register(modEventBus);
    SurvivalParticleTypes.deferredRegister.register(modEventBus);
    SurvivalBlocks.deferredRegister.register(modEventBus);
  }

  public ModDist getModDist() {
    return this.modDist;
  }

  public boolean isImmerseLoaded() {
    return this.immerseLoaded;
  }

  public static CraftingDeadSurvival instance() {
    return instance;
  }

  // ================================================================================
  // Mod Events
  // ================================================================================

  private void handleCommonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(Ingredient.of(ModItems.SYRINGE.get()),
        Ingredient.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE),
        new ItemStack(SurvivalItems.CURE_SYRINGE.get())));

    registerEntitySpawnPlacements();
  }

  private static void registerEntitySpawnPlacements() {
    SpawnPlacements.register(SurvivalEntityTypes.FAST_ZOMBIE.get(),
        SpawnPlacements.Type.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        CraftingDeadSurvival::checkZombieSpawnRules);

    SpawnPlacements.register(SurvivalEntityTypes.TANK_ZOMBIE.get(),
        SpawnPlacements.Type.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        CraftingDeadSurvival::checkZombieSpawnRules);

    SpawnPlacements.register(SurvivalEntityTypes.WEAK_ZOMBIE.get(),
        SpawnPlacements.Type.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        CraftingDeadSurvival::checkZombieSpawnRules);
  }

  private void handleEntityAttributeCreation(EntityAttributeCreationEvent event) {
    event.put(SurvivalEntityTypes.DOCTOR_ZOMBIE.get(),
        DoctorZombieEntity.createAttributes().build());
    event.put(SurvivalEntityTypes.FAST_ZOMBIE.get(),
        FastZombie.createAttributes().build());
    event.put(SurvivalEntityTypes.GIANT_ZOMBIE.get(),
        GiantZombie.createAttributes().build());
    event.put(SurvivalEntityTypes.POLICE_ZOMBIE.get(),
        PoliceZombieEntity.createAttributes().build());
    event.put(SurvivalEntityTypes.TANK_ZOMBIE.get(),
        TankZombie.createAttributes().build());
    event.put(SurvivalEntityTypes.WEAK_ZOMBIE.get(),
        WeakZombie.createAttributes().build());
  }

  private void handleGatherData(GatherDataEvent event) {
    var generator = event.getGenerator();
    if (event.includeServer()) {
      generator.addProvider(new SurvivalItemTagsProvider(generator,
          new ForgeBlockTagsProvider(generator, event.getExistingFileHelper()),
          event.getExistingFileHelper()));
      generator.addProvider(new SurvivalRecipeProvider(generator));
      generator.addProvider(new SurvivalLootTableProvider(generator));
    }

    if (event.includeClient()) {
      generator.addProvider(new SurvivalModelProvider(generator));
    }
  }

  // ================================================================================
  // Common Forge Events
  // ================================================================================

  @SubscribeEvent
  public void handleLivingPackSize(LivingPackSizeEvent event) {
    if (event.getEntity() instanceof Zombie) {
      event.setMaxPackSize(12);
      event.setResult(Event.Result.ALLOW);
    }
  }

  @SubscribeEvent
  public void handleSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
    var level = event.getEntity().getLevel();
    if (!level.isClientSide() && event.getEntity() instanceof Zombie zombie) {

      zombie.getAttribute(Attributes.ATTACK_KNOCKBACK)
          .setBaseValue(serverConfig.zombieAttackKnockback.get());

      if (zombie.getType() == EntityType.ZOMBIE) {
        zombie.getAttribute(Attributes.MAX_HEALTH)
            .setBaseValue(serverConfig.advancedZombieMaxHealth.get());
        zombie.getAttribute(Attributes.ATTACK_DAMAGE)
            .setBaseValue(serverConfig.advancedZombieAttackDamage.get());
      }

      zombie.getAttribute(Attributes.ARMOR)
          .addPermanentModifier(new AttributeModifier("Armor bonus",
              2, AttributeModifier.Operation.ADDITION));

      var extension = LivingExtension.getOrThrow(zombie);
      if (extension == null) {
        logger.warn("LivingExtension capability is not present on {}", this.toString());
        return;
      }

      extension.setEquipmentDropChance(Equipment.Slot.CLOTHING,
          serverConfig.zombieClothingDropChance.get().floatValue());
      extension.setEquipmentDropChance(Equipment.Slot.HAT,
          serverConfig.zombieHatDropChance.get().floatValue());
      zombie.setDropChance(EquipmentSlot.MAINHAND,
          serverConfig.zombieHandDropChance.get().floatValue());
      zombie.setDropChance(EquipmentSlot.OFFHAND,
          serverConfig.zombieHandDropChance.get().floatValue());
    }
  }

  @SubscribeEvent
  public void handlePerformAction(LivingExtensionEvent.PerformAction<EntityItemAction<?>> event) {
    var action = event.getAction();
    var target = action.getSelectedTarget();
    if (!event.getLiving().level().isClientSide()
        && action.type() == ActionTypes.USE_SYRINGE.get()) {
      SurvivalActionTypes.USE_SYRINGE_ON_ZOMBIE.get()
          .createEntityAction(event.getLiving(), target, action.getHand())
          .ifPresent(newAction -> {
            event.setCanceled(true);
            event.getLiving().performAction(newAction, true);
          });
    }
  }

  @SubscribeEvent
  public void handleMissingBlockMappings(RegistryEvent.MissingMappings<Block> event) {
    event.getMappings(H_CD_SERVER_CORE_ID).forEach(mapping -> {
      Block newBlock = mapping.registry.getValue(new ResourceLocation(ID, mapping.key.getPath()));
      if (newBlock != null) {
        mapping.remap(newBlock);
      }
    });
  }

  @SubscribeEvent
  public void handleMissingItemMappings(RegistryEvent.MissingMappings<Item> event) {
    event.getMappings(H_CD_SERVER_CORE_ID).forEach(mapping -> {
      Item newItem = mapping.registry.getValue(new ResourceLocation(ID, mapping.key.getPath()));
      if (newItem != null) {
        mapping.remap(newItem);
      }
    });
  }

  @SubscribeEvent
  public void handleAttachLivingExtensions(LivingExtensionEvent.Load event) {
    if (event.getLiving() instanceof PlayerExtension<?> player) {
      player.registerHandler(SurvivalPlayerHandler.TYPE, new SurvivalPlayerHandler(player));
    } else if (event.getLiving().entity() instanceof Zombie zombie) {
      @SuppressWarnings("unchecked")
      var extension = (BasicLivingExtension<Zombie>) event.getLiving();
      ZombieHandler handler;
      if (zombie.getType() == SurvivalEntityTypes.DOCTOR_ZOMBIE.get()) {
        handler = new DoctorZombieHandler(extension);
      } else if (zombie.getType() == SurvivalEntityTypes.GIANT_ZOMBIE.get()) {
        handler = new GiantZombieHandler(extension);
      } else if (zombie.getType() == SurvivalEntityTypes.POLICE_ZOMBIE.get()) {
        handler = new PoliceZombieHandler(extension);
      } else {
        handler = new ZombieHandler(extension);
      }

      extension.registerHandler(ZombieHandler.TYPE, handler);
    }
  }

  @SubscribeEvent
  public void handleGunHitEntity(GunEvent.EntityHit event) {
    event.target().getCapability(LivingExtension.CAPABILITY)
        .resolve()
        .flatMap(living -> living.getHandler(SurvivalPlayerHandler.TYPE))
        .ifPresent(playerHandler -> {
          float enchantmentPct =
              EnchantmentHelper.getItemEnchantmentLevel(SurvivalEnchantments.INFECTION.get(),
                  event.getItemStack())
                  / (float) SurvivalEnchantments.INFECTION.get().getMaxLevel();
          playerHandler.infect(enchantmentPct);
        });
  }

  @SubscribeEvent
  public void handleBiomeLoading(BiomeLoadingEvent event) {
    if (!serverConfig.zombiesEnabled.get()) {
      return;
    }

    var iterator = event.getSpawns().getSpawner(MobCategory.MONSTER).listIterator();
    while (iterator.hasNext()) {
      var spawnEntry = iterator.next();
      if (spawnEntry.type == EntityType.ZOMBIE) {
        iterator.remove();
        if (serverConfig.advancedZombiesEnabled.get()) {
          iterator.add(new MobSpawnSettings.SpawnerData(
              EntityType.ZOMBIE,
              serverConfig.advancedZombieSpawnWeight.get(),
              serverConfig.advancedZombieMinSpawn.get(),
              serverConfig.advancedZombieMaxSpawn.get()));
        }

        if (serverConfig.fastZombiesEnabled.get()) {
          iterator.add(new MobSpawnSettings.SpawnerData(
              SurvivalEntityTypes.FAST_ZOMBIE.get(),
              serverConfig.fastZombieSpawnWeight.get(),
              serverConfig.fastZombieMinSpawn.get(),
              serverConfig.fastZombieMaxSpawn.get()));
        }
        if (serverConfig.tankZombiesEnabled.get()) {
          iterator.add(new MobSpawnSettings.SpawnerData(
              SurvivalEntityTypes.TANK_ZOMBIE.get(),
              serverConfig.tankZombieSpawnWeight.get(),
              serverConfig.tankZombieMinSpawn.get(),
              serverConfig.tankZombieMaxSpawn.get()));
        }
        if (serverConfig.weakZombiesEnabled.get()) {
          iterator.add(new MobSpawnSettings.SpawnerData(
              SurvivalEntityTypes.WEAK_ZOMBIE.get(),
              serverConfig.weakZombieSpawnWeight.get(),
              serverConfig.weakZombieMinSpawn.get(),
              serverConfig.weakZombieMaxSpawn.get()));
        }
      }
    }
  }

  public static boolean checkZombieSpawnRules(EntityType<? extends Monster> type,
      ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, Random random) {
    return level.getBrightness(LightLayer.BLOCK, pos) <= 8
        && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
  }
}
