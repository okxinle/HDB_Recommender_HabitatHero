package repository;

import entity.HDBBlock;
import java.util.Arrays;
import java.util.List;

public class DummyHDBRepository implements IHDBRepository {

    @Override
    public List<HDBBlock> getAllBlocks() {
        // HDBBlock(blockId, postalCode, town, estimatedPrice,
        //          remainingLeaseYears, westSunStatus, noiseRiskLevel, futureRiskFlag)
        HDBBlock tampines    = new HDBBlock(1, "520101", "Tampines",      450_000.0, 75, false, "LOW",    false);
        HDBBlock jurongWest  = new HDBBlock(2, "640222", "Jurong West",   380_000.0, 60, true,  "MEDIUM", false);
        HDBBlock bishan      = new HDBBlock(3, "570335", "Bishan",        520_000.0, 85, false, "LOW",    true);
        HDBBlock woodlands   = new HDBBlock(4, "730410", "Woodlands",     340_000.0, 50, false, "HIGH",   false);
        HDBBlock queenstown  = new HDBBlock(5, "140055", "Queenstown",    610_000.0, 90, true,  "LOW",    false);
        return Arrays.asList(tampines, jurongWest, bishan, woodlands, queenstown);
    }
}

