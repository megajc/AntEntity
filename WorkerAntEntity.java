package com.giner.modginer.entities;

import com.giner.modginer.init.ModEntityClass;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.UUID;

public class WorkerAntEntity extends AnimalEntity implements IAngerable{


    private static final DataParameter<Integer> HONEY = EntityDataManager.defineId(WorkerAntEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> DATA_REMAINING_ANGER_TIME = EntityDataManager.defineId(WorkerAntEntity.class, DataSerializers.INT);
    private static final RangedInteger PERSISTENT_ANGER_TIME = TickRangeConverter.rangeOfSeconds(20, 39);
    private UUID persistentAngerTarget;

    public WorkerAntEntity(EntityType<? extends WorkerAntEntity> type, World worldIn) {
        super(type, worldIn);
    }

    public static AttributeModifierMap.MutableAttribute setCustomAttributes() {
        return MobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new TemptGoal(this, 1.0D, Ingredient.of(Items.SUGAR), false));
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal());
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, GiantWeevilEntity.class, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.addGoal(9, new ResetAngerGoal<>(this, true));
        this.targetSelector.addGoal(10, new NearestAttackableTargetGoal<>(this, HoneyQueenEntity.class, false));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HONEY, 0);
        this.entityData.define(DATA_REMAINING_ANGER_TIME, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("HONEY")){
            this.setHoney(compound.getInt("HONEY"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("HONEY", this.getHoney());
    }

    public int getHoney(){
        return this.getEntityData().get(HONEY);
    }

    public void addHoney(){
        this.getEntityData().set(HONEY, (this.getEntityData().get(HONEY) + 1));
    }

    public void restHoney(){
        this.getEntityData().set(HONEY, (this.getEntityData().get(HONEY) - 1));
    }

    public void setHoney(int hon){
        this.getEntityData().set(HONEY, (hon));
    }

    @Override
    public ActionResultType mobInteract(PlayerEntity pl, Hand hand) {
        ItemStack item = pl.getItemInHand(Hand.MAIN_HAND);
        if (!this.isBaby() && pl.getItemInHand(Hand.MAIN_HAND).getItem().equals(Items.SUGAR)){
            this.addHoney();
            if (this.random.nextBoolean()){
                this.addHoney();
            }
            if (this.getHoney() > 5){
                this.setHoney(5);
            } else {
                this.playSound(SoundEvents.HORSE_EAT, 0.15F, 1.0F);
                this.playSound(SoundEvents.BOTTLE_FILL, 0.15F, 1.0F);
                this.usePlayerItem(pl, item);
                if (this.level.isClientSide) {
                    return ActionResultType.CONSUME;
                }
            }
        } else if (!this.isBaby() && pl.getItemInHand(Hand.MAIN_HAND).getItem().equals(Items.GLASS_BOTTLE) && this.getHoney() > 0){
            this.restHoney();
            this.usePlayerItem(pl, item);
            this.playSound(SoundEvents.BOTTLE_FILL, 0.15F, 1.0F);
            pl.addItem(Items.HONEY_BOTTLE.getDefaultInstance());
            return ActionResultType.CONSUME;
        } else{
            super.mobInteract(pl, hand);
        }
        return ActionResultType.sidedSuccess(this.level.isClientSide);
    }

    @Override
    protected int getExperienceReward(PlayerEntity player) {
        return 1 + this.getCommandSenderWorld().random.nextInt(2);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PUFFER_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.STONE_BREAK ;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        //VANILLA VALUES
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    @Override
    public boolean doHurtTarget(Entity p_70652_1_) {
        boolean flag = p_70652_1_.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, p_70652_1_);
        }

        return flag;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    @Override
    public void setRemainingPersistentAngerTime(int p_230260_1_) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, p_230260_1_);
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() { return this.persistentAngerTarget; }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID p_230259_1_) { this.persistentAngerTarget = p_230259_1_; }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.randomValue(this.random));
    }

    //GOALS/////////////////////////////////////////
    class MeleeAttackGoal extends net.minecraft.entity.ai.goal.MeleeAttackGoal {
        public MeleeAttackGoal() {
            super(WorkerAntEntity.this, 1.25D, true);
        }

        protected void checkAndPerformAttack(LivingEntity entity, double p_190102_2_) {
            if(entity.getType() != ModEntityClass.HONEY_QUEEN_ANT.get()){
                super.checkAndPerformAttack(entity, p_190102_2_);
            }
        }

        public void stop() {
            super.stop();
        }

        protected double getAttackReachSqr(LivingEntity p_179512_1_) {
            return (double)(4.0F + p_179512_1_.getBbWidth());
        }
    }
}
