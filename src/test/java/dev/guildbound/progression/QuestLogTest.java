package dev.guildbound.progression;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestLogTest {
    @Test void carriedContractsLeaveOnlyFreePlacesForOffers() {
        var old=DailyJournal.empty().refresh(1,1);
        var today=old.refresh(2,1);
        var log=QuestLog.empty();
        for(int slot=0;slot<3;slot++)log=log.add(AcceptedQuest.offer(old,slot));
        assertEquals(java.util.List.of(0,1),log.offerSlots(today));
        log=log.add(AcceptedQuest.offer(today,0));today=today.accept(0);
        assertEquals(java.util.List.of(1),log.offerSlots(today));
        log=log.add(AcceptedQuest.offer(today,1));today=today.accept(1);
        assertEquals(5,log.activeEntries().size());assertTrue(log.offerSlots(today).isEmpty());
        assertTrue(log.offerSlots(today.refresh(3,1)).isEmpty());
    }
    @Test void claimingOldContractOpensOnePlaceWithoutReofferingAcceptedSlots() {
        var old=DailyJournal.empty().refresh(1,1);
        var today=old.refresh(2,1).accept(0);
        var log=QuestLog.empty().add(AcceptedQuest.offer(today,0));
        for(int slot=0;slot<4;slot++)log=log.add(AcceptedQuest.offer(old,slot));
        log=log.remove(old.day(),0);
        assertEquals(java.util.List.of(1),log.offerSlots(today));
        assertEquals(5,log.activeEntries().size()+log.offerSlots(today).size());
    }
    @Test void legacyOverflowIsPreservedAndPromotedWithoutHiddenProgress() {
        var log=QuestLog.empty();
        for(int day=0;day<7;day++)log=log.add(new AcceptedQuest(day,0,0,0,0));
        log=log.kill("zombie",true,200000);
        assertEquals(7,log.entries().size());assertEquals(5,log.activeEntries().size());
        assertEquals(0,log.entries().get(5).kills());
        assertTrue(log.offerSlots(DailyJournal.empty().refresh(9,1)).isEmpty());
        var json=QuestLog.CODEC.encodeStart(JsonOps.INSTANCE,log).getOrThrow();
        log=QuestLog.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().remove(0,0);
        assertEquals(5,log.activeEntries().getLast().day());
        log=log.kill("zombie",true,200001);
        assertEquals(1,log.find(5,0).orElseThrow().kills());
        assertEquals(0,log.find(6,0).orElseThrow().kills());
    }
    @Test void regularQuestSurvivesDawnAndOfflineDaysWithFrozenReward() {
        var board=DailyJournal.empty().refresh(2,5);
        var q=AcceptedQuest.offer(board,1);
        var log=QuestLog.empty().migrate(board).add(q).kill("zombie",true,60000);
        var nextBoard=board.refresh(40,100);
        log=log.expire(40*24000);
        assertEquals(1,log.entries().size());assertEquals(1,log.entries().getFirst().kills());
        assertEquals(q.experience(),log.entries().getFirst().experience());
        assertTrue(nextBoard.experience(1)>q.experience());
        var json=QuestLog.CODEC.encodeStart(JsonOps.INSTANCE,log).getOrThrow();
        assertEquals(log,QuestLog.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
    }
    @Test void urgentDeadlineIncludesTurnInAndExpiresAtExactBoundary() {
        var board=DailyJournal.empty().refresh(2,1);
        var q=AcceptedQuest.offer(board,4);
        assertTrue(q.timed());assertEquals(72000,q.deadline());
        assertEquals(board.experience(4)*2,q.experience());assertEquals(board.emeralds(4)*2,q.emeralds());
        var log=QuestLog.empty().migrate(board).add(q);
        for(int i=0;i<q.target();i++)log=log.kill("creeper",true,71999);
        assertTrue(log.entries().getFirst().ready());assertFalse(log.entries().getFirst().expired(71999));
        assertTrue(log.expire(72000).entries().isEmpty());
        assertFalse(AcceptedQuest.offer(board.refresh(3,1),4).timed());
    }
    @Test void migrationNeverRetroactivelyAddsDeadlineOrDuplicatesOldQuest() {
        var old=DailyJournal.empty().refresh(2,1).accept(4).kill("creeper",true);
        var log=QuestLog.empty().migrate(old);
        assertFalse(log.entries().getFirst().timed());assertEquals(1,log.entries().getFirst().kills());
        assertEquals(old.experience(4),log.entries().getFirst().experience());
        assertEquals(log,log.migrate(old));assertEquals(log,log.expire(1000000));
    }
    @Test void SameSlotOnDifferentDaysIsNotTheSameContract() {
        var a=AcceptedQuest.offer(DailyJournal.empty().refresh(1,1),0);
        var b=AcceptedQuest.offer(DailyJournal.empty().refresh(2,1),0);
        var log=QuestLog.empty().add(a).add(b).add(a).kill("zombie",true,60000);
        assertEquals(2,log.entries().size());assertTrue(log.entries().stream().allMatch(q->q.kills()==1));
        assertEquals(1,log.remove(1,0).entries().size());assertTrue(log.remove(1,0).find(2,0).isPresent());
    }
}
