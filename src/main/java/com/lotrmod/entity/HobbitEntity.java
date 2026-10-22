package com.lotrmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A hobbit of the Shire, after the original mod's {@code LOTREntityHobbit}: a
 * small, timid NPC that wanders near home, flees anything hostile and panics
 * when hurt. Hobbit holes spawn a married couple, named from the original's
 * name lists ({@link HobbitNames}), who keep to within a few dozen blocks of
 * their hole.
 *
 * <p>The original's faction alignment, speech, trading, marriage and children
 * have nothing here to hang off, so a hobbit is a name, a gender, a skin and a
 * home. Monsters stand in for the orcs, wargs and trolls it ran from.
 */
public class HobbitEntity extends PathfinderMob {

    /** 13 skin variants per gender exist in the ported texture pool (male_0..12 / female_0..12). */
    public static final int SKIN_VARIANT_COUNT = 13;

    private static final EntityDataAccessor<Boolean> DATA_FEMALE =
            SynchedEntityData.defineId(HobbitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SKIN_VARIANT =
            SynchedEntityData.defineId(HobbitEntity.class, EntityDataSerializers.INT);

    public HobbitEntity(EntityType<? extends HobbitEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FEMALE, false);
        builder.define(DATA_SKIN_VARIANT, 0);
    }

    public boolean isFemale() { return entityData.get(DATA_FEMALE); }
    public int getSkinVariant() { return entityData.get(DATA_SKIN_VARIANT); }

    public void setFemale(boolean female) {
        entityData.set(DATA_FEMALE, female);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        entityData.set(DATA_FEMALE, random.nextBoolean());
        entityData.set(DATA_SKIN_VARIANT, random.nextInt(SKIN_VARIANT_COUNT));
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Female", isFemale());
        tag.putInt("SkinVariant", getSkinVariant());
        // Vanilla doesn't save a mob's home, so a hobbit would forget its hole
        // the first time its chunk unloaded
        if (hasRestriction()) {
            BlockPos home = getRestrictCenter();
            tag.putIntArray("Home", new int[]{home.getX(), home.getY(), home.getZ()});
            tag.putInt("HomeRadius", (int) getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_FEMALE, tag.getBoolean("Female"));
        entityData.set(DATA_SKIN_VARIANT, Math.floorMod(tag.getInt("SkinVariant"), SKIN_VARIANT_COUNT));
        int[] home = tag.getIntArray("Home");
        if (home.length == 3) {
            restrictTo(new BlockPos(home[0], home[1], home[2]), tag.getInt("HomeRadius"));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 12.0f, 1.5, 1.8));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.1));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(13, new LookAtPlayerGoal(this, LivingEntity.class, 8.0f));
        this.goalSelector.addGoal(14, new RandomLookAroundGoal(this));
    }
}
