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

package com.craftingdead.core.network.message.play;

import com.craftingdead.core.world.entity.extension.PlayerExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DamageHandcuffsMessage() {

  public void encode(FriendlyByteBuf buf) {
  }

  public static DamageHandcuffsMessage decode(FriendlyByteBuf buf) {
    return new DamageHandcuffsMessage();
  }

  public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> {
      ServerPlayer player = context.getSender();
      if (player != null) {
        var playerExtension = PlayerExtension.getOrThrow(player);
        if (playerExtension.isHandcuffed()) {
          playerExtension.handcuffInteract();
        }
      }
    });
    context.setPacketHandled(true);
  }
}

