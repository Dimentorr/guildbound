package dev.guildbound.progression;

import com.mojang.serialization.JsonOps;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpansionTest {
    @Test void spellCircleSharesCooldownButMartialAbilitiesStayIndependent() {
        var timers=AbilityState.ready().start(HeroAbility.SONG_STRENGTH,300);
        assertEquals(300,timers.cooldown(HeroAbility.SONG_SPEED));
        assertEquals(300,timers.cooldown(HeroAbility.HEALING_FIELD));
        assertEquals(0,timers.cooldown(HeroAbility.TAUNT));
        var resources=dev.guildbound.combat.ResourceState.full();
        assertTrue(resources.canHeal());
        assertEquals(0,timers.tick(300).cooldown(HeroAbility.SONG_SPEED));
    }
    @Test void everySubclassSelectsOnlyItsOwnClassAndPersists() {
        for (var sub : HeroSubclass.values()) {
            var p = new CharacterProgress(List.of(new ClassTrack(sub.parent(), 5)), 0).specialize(sub);
            assertEquals(sub, p.tracks().getFirst().subclass());
            var json = dev.guildbound.data.ProgressCodecs.CODEC.encodeStart(JsonOps.INSTANCE, p).getOrThrow();
            assertEquals(p, dev.guildbound.data.ProgressCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
    }
    @Test void abilitiesRequireTheirSubclassAndLevelAndImproveThroughTalents() {
        for (var ability : HeroAbility.values()) {
            var p = new CharacterProgress(List.of(new ClassTrack(ability.subclass, ability.level)), 0);
            assertTrue(ability.available(p));
            assertFalse(ability.available(new CharacterProgress(List.of(new ClassTrack(ability.subclass.parent(), 100)), 0)));
            assertTrue(TalentState.empty().canBuy(p, ability.talent(), 0));
            if (ability == HeroAbility.ARCANE_BURST) assertEquals(0, ability.cooldown(2));
            else assertTrue(ability.cooldown(2) < ability.cooldown(0));
            assertTrue(ability.power(2) > ability.power(0));
        }
        assertFalse(TalentState.empty().canBuy(new CharacterProgress(List.of(new ClassTrack(HeroSubclass.SORCERER, 100)), 0), Talent.SPELL_WEAVING, 0));
    }
    @Test void switchingDoesNotResetSavedCooldowns() {
        var state = AbilityState.ready().start(HeroAbility.HOLY_FIELD, 500).start(HeroAbility.GREAT_HEAL, 180);
        var json = AbilityState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        assertEquals(state, AbilityState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(320, state.tick(180).cooldown(HeroAbility.HOLY_FIELD));
        assertEquals(320, state.tick(180).cooldown(HeroAbility.GREAT_HEAL));
        assertTrue(state.tick(500).remaining().isEmpty());
    }
    @Test void dailyRewardsCannotReplayAndDayCannotMoveBackwards() {
        var state = DailyJournal.empty().refresh(5, 5);
        for (int slot = 0; slot < 5; slot++) {
            state = state.accept(slot);
            for (int kill = 0; kill < state.target(slot); kill++) state = state.kill(state.targetType(slot), true);
            assertTrue(state.ready(slot));
            state = state.claim(slot);
            assertSame(state, state.claim(slot));
            assertSame(state, state.accept(slot));
        }
        assertEquals(31, state.claimed());
        assertSame(state, state.refresh(4, 100));
        assertSame(state, state.refresh(5, 100));
        var next = state.refresh(6, 100);
        assertEquals(0, next.claimed()); assertEquals(0, next.accepted());
        assertTrue(next.experience(4) > state.experience(4));
        assertTrue(next.target(4) > state.target(4));
        assertEquals("wither_skeleton", next.targetType(4));
    }
    @Test void dailyProgressRequiresAcceptanceAndMatchingEnemyAndPersists() {
        var state = DailyJournal.empty().refresh(0, 1);
        assertEquals(state, state.kill("zombie", true));
        state = state.accept(1).kill("skeleton", true);
        assertEquals(0, state.kills().get(1));
        state = state.kill("zombie", true);
        assertEquals(1, state.kills().get(1));
        var json = DailyJournal.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        assertEquals(state, DailyJournal.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertThrows(IllegalArgumentException.class, () -> DailyJournal.empty().accept(5));
    }
}
