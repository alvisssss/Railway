package com.railwayteam.railways.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.railwayteam.railways.registry.CRBogeyStyles;
import com.railwayteam.railways.registry.CRSounds;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSounds;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageSounds.class)
public class MixinCarriageSounds {
    @Shadow(remap = false)
    LerpedFloat seatCrossfade;

    @Unique
    private boolean railways$skip;

    @Unique
    private boolean railways$isHandcar;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void skipIfInvisible(CarriageContraptionEntity entity, CallbackInfo ci) {
        railways$skip = entity.getCarriage().bogeys.both((b) -> b == null || b.getStyle() == CRBogeyStyles.INVISIBLE || b.getStyle() == CRBogeyStyles.INVISIBLE_MONOBOGEY);
        railways$isHandcar = entity.getCarriage().bogeys.both((b) -> b == null || b.getStyle() == CRBogeyStyles.HANDCAR);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void actuallySkip(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
        if (railways$skip)
            ci.cancel();
    }

    @WrapOperation(method = {
        "tick",
        "submitSharedSoundVolume"
    }, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/AllSoundEvents$SoundEntry;getMainEvent()Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent useCogRumble(AllSoundEvents.SoundEntry instance, Operation<SoundEvent> original) {
        if (railways$isHandcar && instance == AllSoundEvents.TRAIN)
            return CRSounds.HANDCAR_COGS.get();
        return original.call(instance);
    }

    @SuppressWarnings("unused")
    @WrapWithCondition(method = "tick", at = {
        @At(value = "INVOKE", target = "Lcom/simibubi/create/AllSoundEvents$SoundEntry;playAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;FFZ)V", ordinal = 0),
        @At(value = "INVOKE", target = "Lcom/simibubi/create/AllSoundEvents$SoundEntry;playAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;FFZ)V", ordinal = 1)
    }, remap = true)
    private boolean shouldPlaySteamSound(AllSoundEvents.SoundEntry instance, Level world, Vec3 pos, float volume, float pitch, boolean fade) {
        return instance != AllSoundEvents.STEAM || !railways$isHandcar;
    }

    @SuppressWarnings("unused")
    @WrapWithCondition(method = "tick", at = {
        @At(value = "INVOKE", target = "Lcom/simibubi/create/AllSoundEvents$SoundEntry;playAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;FFZ)V", ordinal = 2)
    }, remap = true)
    private boolean shouldPlaySteamReleaseSound(AllSoundEvents.SoundEntry instance, Level world, Vec3 pos, float volume, float pitch, boolean fade) {
        return instance != AllSoundEvents.STEAM || !railways$isHandcar;
    }

    @SuppressWarnings("unused")
    @WrapWithCondition(method = "tick", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V")
    }, remap = true)
    private boolean shouldPlaySteamReleaseSound3(Level instance, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {
        return !railways$isHandcar;
    }

    @WrapOperation(method = "finalizeSharedVolume", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/CarriageSounds$LoopingSound;setVolume(F)V", ordinal = 0))
    private void handcarNoCrossfade(@Coerce /* CarriageSounds.LoopingSound */ AbstractTickableSoundInstance instance,
                                    float volume, Operation<Void> original) {
        if (railways$isHandcar) {
            float crossfade = seatCrossfade.getValue();
            volume = (1 - (crossfade * .125f)) * volume * 1024;
        }
        original.call(instance, volume);
    }
}
