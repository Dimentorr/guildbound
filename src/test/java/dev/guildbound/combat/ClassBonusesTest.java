package dev.guildbound.combat;

import dev.guildbound.progression.CharacterProgress;
import dev.guildbound.progression.ClassTrack;
import dev.guildbound.progression.HeroSubclass;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassBonusesTest {
    @Test void onlyRogueAndRangerCanDodgeInAir() {
        assertFalse(ClassBonuses.canDodgeFrom(CharacterProgress.empty(), true));
        for (var subclass : HeroSubclass.values()) {
            var progress = profile(subclass, 1);
            assertTrue(ClassBonuses.canDodgeFrom(progress, true));
            assertEquals(subclass.parent() == dev.guildbound.progression.HeroClass.RANGER || subclass.parent() == dev.guildbound.progression.HeroClass.ROGUE,
                    ClassBonuses.canDodgeFrom(progress, false));
        }
    }
    private static CharacterProgress profile(HeroSubclass subclass, int level) {
        return new CharacterProgress(List.of(new ClassTrack(subclass, level)), 0);
    }

    @Test void unregisteredPlayersKeepVanillaMovementAndCombat() {
        var p = CharacterProgress.empty();
        assertEquals(0, ClassBonuses.speed(p));
        assertEquals(0, ClassBonuses.bowDamage(p));
        assertEquals(0, ClassBonuses.resistance(p));
        assertEquals(0, ClassBonuses.weaponSpeed(p));
        assertEquals(0, ClassBonuses.concealment(p));
        assertEquals(0, ClassBonuses.armorPenalty(p, 4));
    }

    @Test void specialtyUnlocksAtItsOwnThirdLevel() {
        var second = profile(HeroSubclass.HUNTER, 2);
        var third = profile(HeroSubclass.HUNTER, 3);
        assertEquals(.15, ClassBonuses.bowDamage(second));
        assertEquals(.25, ClassBonuses.bowDamage(third));
        var mixed = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.HUNTER, 1),
                new ClassTrack(HeroSubclass.SORCERER, 3)), 0);
        assertEquals(.15, ClassBonuses.bowDamage(mixed));
        assertEquals(5, ClassBonuses.healing(mixed));
    }

    @Test void warriorIsImmuneToHeavyArmorPenaltyOnly() {
        var warrior = profile(HeroSubclass.GUARDIAN, 1);
        assertEquals(0, ClassBonuses.armorPenalty(warrior, 4));
        assertEquals(.10, ClassBonuses.resistance(warrior));
        assertEquals(.15, ClassBonuses.weaponSpeed(warrior));
        assertEquals(0, ClassBonuses.bowDamage(warrior));
        var mage = profile(HeroSubclass.SORCERER, 1);
        assertEquals(0, ClassBonuses.armorPenalty(mage, 0));
        assertEquals(.04, ClassBonuses.armorPenalty(mage, 1));
        assertEquals(.16, ClassBonuses.armorPenalty(mage, 4));
    }

    @Test void overlappingMovementAndStealthUseStrongestClassInsteadOfStacking() {
        var mixed = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.HUNTER, 3),
                new ClassTrack(HeroSubclass.DUELIST, 3)), 0);
        assertEquals(.20, ClassBonuses.speed(mixed));
        assertEquals(.35, ClassBonuses.concealment(mixed));
        assertEquals(0, ClassBonuses.resistance(mixed));
        assertEquals(0, ClassBonuses.weaponSpeed(mixed));
    }

    @Test void mageHealingImprovesWithoutChangingManaCost() {
        assertEquals(4, ClassBonuses.healing(profile(HeroSubclass.SORCERER, 2)));
        assertEquals(5, ClassBonuses.healing(profile(HeroSubclass.SORCERER, 3)));
        assertEquals(750, ResourceState.full().heal().mana());
    }

    @Test void daggerTrainingUsesRogueLevelAndDoesNotApplyToSwords() {
        var rogue = profile(HeroSubclass.DUELIST, 2);
        assertEquals(.20, ClassBonuses.attackSpeed(rogue, true, false));
        assertEquals(.30, ClassBonuses.attackSpeed(profile(HeroSubclass.DUELIST, 3), true, false));
        assertEquals(0, ClassBonuses.attackSpeed(rogue, false, true));
        assertEquals(0, ClassBonuses.attackSpeed(rogue, false, false));
        assertEquals(0, ClassBonuses.attackSpeed(CharacterProgress.empty(), true, true));
    }

    @Test void daggerTagTakesPriorityOverSwordTagWithoutStacking() {
        var warrior = profile(HeroSubclass.GUARDIAN, 3);
        assertEquals(0, ClassBonuses.attackSpeed(warrior, true, true));
        assertEquals(.20, ClassBonuses.attackSpeed(warrior, false, true));
        var mixed = new CharacterProgress(List.of(new ClassTrack(HeroSubclass.GUARDIAN, 3),
                new ClassTrack(HeroSubclass.DUELIST, 1)), 0);
        assertEquals(.20, ClassBonuses.attackSpeed(mixed, true, true));
        assertEquals(.20, ClassBonuses.attackSpeed(mixed, false, true));
        assertEquals(0, ClassBonuses.attackSpeed(mixed, false, false));
    }
}
