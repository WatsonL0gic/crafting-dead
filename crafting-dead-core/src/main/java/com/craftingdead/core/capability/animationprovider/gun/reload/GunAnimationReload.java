package com.craftingdead.core.capability.animationprovider.gun.reload;

import com.craftingdead.core.capability.animationprovider.gun.GunAnimation;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class GunAnimationReload extends GunAnimation {

  protected float rotation1 = 0F;
  protected float lastRotation1 = 0F;
  protected float maxRotation1 = 20;

  protected float trans1 = -1F;
  protected float lastTrans1 = 0F;
  protected float maxTrans1 = 0.3F;

  protected boolean up = true;

  /**
   * When set to false, the clip will be loaded into the gun
   */
  public boolean ejectingClip = false;

  public GunAnimationReload() {
    this.ejectingClip = true;
    trans1 = 0F;
  }

  public GunAnimationReload(boolean par1) {
    this.ejectingClip = par1;
    trans1 = this.ejectingClip ? 0F : -1F;
  }

  public void setEjectingClip(boolean par1) {
    this.ejectingClip = par1;
    trans1 = this.ejectingClip ? 0F : -1F;
  }

  @Override
  public void onUpdate(Minecraft par1, LivingEntity par2, ItemStack par3, float progress) {
    if (progress >= 2.0F / 3.0F) {
      up = false;
    }

    lastRotation1 = rotation1;
    lastTrans1 = trans1;

    float roation1Speed = 5F;
    float transSpeed = 0.1F;

    if (this.ejectingClip) {
      trans1 -= transSpeed;
    } else {
      trans1 += transSpeed;

      if (trans1 > 0) {
        trans1 = 0;
      }
    }

    if (up) {
      rotation1 += roation1Speed;
    } else {
      rotation1 -= roation1Speed;
    }

    if (rotation1 > maxRotation1) {
      rotation1 = maxRotation1;
    }

    if (rotation1 < 0) {
      rotation1 = 0;
    }
  }

  @Override
  public void doRender(ItemStack par1, float par2, MatrixStack matrixStack) {
    float progress = (lastRotation1 + (rotation1 - lastRotation1) * par2);
    matrixStack.rotate(new Vector3f(4.0F, 0.0F, 1.0F).rotationDegrees(-progress));
    matrixStack.rotate(Vector3f.XP.rotationDegrees(progress));
  }

  @Override
  public void doRenderAmmo(ItemStack par1, float par2, MatrixStack matrixStack) {
    if (this.ejectingClip) {
      float transprogress = lastTrans1 + (trans1 - lastTrans1) * par2;
      matrixStack.translate(0, -transprogress, 0);
    } else {
      float transprogress = lastTrans1 + (trans1 - lastTrans1) * par2;
      matrixStack.translate(transprogress, -transprogress, 0);
    }
  }

  @Override
  public void onAnimationStopped(ItemStack par1) {}

  @Override
  public float getMaxAnimationTick() {
    return 30;
  }

  @Override
  public void doRenderHand(ItemStack par1, float partialTicks, boolean rightHand,
      MatrixStack matrixStack) {
    if (rightHand) {
      float progress = (lastRotation1 + (rotation1 - lastRotation1) * partialTicks);
      matrixStack.rotate(Vector3f.ZP.rotationDegrees(-progress * 0.4F));
    } else {
      float transprogress = lastTrans1 + (trans1 - lastTrans1) * partialTicks;
      matrixStack.translate(-transprogress, transprogress, transprogress);
    }
  }

  @Override
  protected boolean isAcceptedTransformType(ItemCameraTransforms.TransformType transformType) {
    return (transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND
        || transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND) ? false
            : super.isAcceptedTransformType(transformType);
  }
}
