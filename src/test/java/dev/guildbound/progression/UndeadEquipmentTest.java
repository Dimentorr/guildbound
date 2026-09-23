package dev.guildbound.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UndeadEquipmentTest {
    @Test void equipmentChangesOnlyAtNecromancerMilestones() {
        int[] levels={1,9,10,24,25,34,35,49,50,79,80,99,100};
        int[] tiers ={0,0, 1, 1, 3, 3, 4, 4, 5, 5, 7, 7,  8};
        for(int i=0;i<levels.length;i++) assertEquals(tiers[i],UndeadEquipment.tier(levels[i]),"level "+levels[i]);
    }
}
