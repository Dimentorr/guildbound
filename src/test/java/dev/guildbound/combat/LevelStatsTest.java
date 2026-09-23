package dev.guildbound.combat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LevelStatsTest {
 @Test void progressionHasStableBaseAndBoundedGrowth(){
  assertEquals(1000,LevelStats.mana(1));assertEquals(1000,LevelStats.stamina(1));
  assertEquals(3970,LevelStats.mana(100));assertEquals(2980,LevelStats.stamina(100));
  assertEquals(0,LevelStats.healthBonus(9));assertEquals(2,LevelStats.healthBonus(10));assertEquals(20,LevelStats.healthBonus(100));
 }
 @Test void regenerationUsesCurrentLevelAndLowerCapsClampWithoutRefilling(){
  var state=ResourceState.full();
  for(int i=0;i<5000;i++)state=state.tick(false,false,0,0,LevelStats.stamina(100),LevelStats.mana(100));
  assertEquals(2980,state.stamina());assertEquals(3970,state.mana());
  state=state.tick(false,false,0,0,LevelStats.stamina(2),LevelStats.mana(2));
  assertEquals(1020,state.stamina());assertEquals(1030,state.mana());
  var raised=ResourceState.full().tick(false,false,0,0,2980,3970);
  assertEquals(1010,raised.stamina());assertEquals(1001,raised.mana());
 }
 @Test void savedLargePoolsAndLegacyPoolsRoundTrip(){
  for(var state:new ResourceState[]{ResourceState.full(),new ResourceState(2980,3970,0,0,0,false)}){
   var codec=dev.guildbound.data.ResourceCodecs.CODEC;
   var json=codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,state).getOrThrow();
   assertEquals(state,codec.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow());
  }
 }
}
