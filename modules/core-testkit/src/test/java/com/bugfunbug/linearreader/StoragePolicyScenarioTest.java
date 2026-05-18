package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.config.LinearConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoragePolicyScenarioTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        LinearTestSupport.resetState();
    }

    @AfterEach
    void tearDown() {
        LinearTestSupport.resetState();
    }

    @Test
    void heavyWritesThenQuietRecoveryShiftFromThroughputBackToMaintenance() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("heavy-quiet"), true)) {
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(20), tick -> tick
                    .backlog(20, 0)
                    .write("hot-a", 16_384)
                    .write("hot-b", 8_192));

            StoragePolicyScenarioHarness.State pressure = scenario.state();
            assertTrue(pressure.compressionLevel() < LinearConfig.getCompressionLevel());
            assertTrue(pressure.flushBudgetPerTick() > LinearConfig.getRegionsPerSaveTick());
            assertFalse(pressure.maintenanceAllowed());
            assertTrue(pressure.quietnessScore() < 0.50D);

            scenario.quietMinutes(12);

            StoragePolicyScenarioHarness.State recovered = scenario.state();
            assertEquals(LinearConfig.getCompressionLevel(), recovered.compressionLevel());
            assertTrue(recovered.maintenanceAllowed());
            assertTrue(recovered.maintenanceBudgetFiles() > 0);
            assertTrue(recovered.quietnessScore() >= 0.75D);
        }
    }

    @Test
    void chronicHighLoadServerStaysThroughputBiased() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("chronic-high-load"), true)) {
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForMinutes(18), tick -> tick
                    .backlog(28, 2)
                    .write("alpha", 16_384)
                    .write("beta", 16_384)
                    .write("gamma", 8_192));

            StoragePolicyScenarioHarness.State state = scenario.state();
            assertEquals("chronic-high-load", state.loadProfile());
            assertEquals("throughput", state.compressionMode());
            assertFalse(state.maintenanceAllowed());
            assertTrue(state.maintenanceBudgetFiles() <= 1);
            assertTrue(state.compressionLevel() < LinearConfig.getCompressionLevel());
        }
    }

    @Test
    void chronicLowLoadServerUnlocksLargeMaintenanceWindow() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("chronic-low-load"), true)) {
            scenario.runTicks(2, tick -> tick
                    .write("cold-debt", 4_096)
                    .flush("cold-debt", 120_000_000L, 4L * 1024L * 1024L, 1L * 1024L * 1024L, 2));

            scenario.quietMinutes(18);

            StoragePolicyScenarioHarness.State state = scenario.state();
            assertEquals("chronic-low-load", state.loadProfile());
            assertEquals("efficiency", state.compressionMode());
            assertTrue(state.maintenanceAllowed());
            assertTrue(state.maintenanceBudgetFiles() >= 8);
            assertTrue(state.residentHotSet() < 24);
        }
    }

    @Test
    void cacheChurnSuppressesMaintenanceBudgetEvenWhenServerIsQuiet() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("cache-churn"), true)) {
            scenario.runTicks(4, tick -> {
                for (int index = 0; index < 4; index++) {
                    String region = "debt-" + index;
                    tick.write(region, 8_192)
                            .flush(region, 120_000_000L, 4L * 1024L * 1024L, 1L * 1024L * 1024L, 2);
                }
            });
            scenario.quietMinutes(12);
            StoragePolicyScenarioHarness.State baseline = scenario.state();

            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(120), tick -> {
                for (int index = 0; index < 4; index++) {
                    String region = "debt-" + index;
                    tick.residentReload(region);
                    tick.residentEviction(region, 32L * 1024L * 1024L);
                }
            });

            StoragePolicyScenarioHarness.State churned = scenario.state();
            assertTrue(baseline.maintenanceBudgetFiles() > 0);
            assertTrue(churned.cacheChurnScore() >= 1.0D);
            assertTrue(churned.maintenanceBudgetFiles() < baseline.maintenanceBudgetFiles());
        }
    }

    @Test
    void pinHeavyPressureShrinksResidentTargetsAndMaintenanceBudget() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("pin-heavy"), true)) {
            scenario.runTicks(4, tick -> {
                for (int index = 0; index < 4; index++) {
                    String region = "pinned-" + index;
                    tick.write(region, 8_192)
                            .flush(region, 120_000_000L, 4L * 1024L * 1024L, 1L * 1024L * 1024L, 2);
                }
            });
            scenario.quietMinutes(18);
            StoragePolicyScenarioHarness.State baseline = scenario.state();

            scenario.pinnedRegions(64);
            scenario.runTicks(40, tick -> tick.pins(64));

            StoragePolicyScenarioHarness.State pinned = scenario.state();
            assertEquals(64, pinned.pinnedRegionCount());
            assertTrue(pinned.residentHotSet() < baseline.residentHotSet());
            assertTrue(pinned.residentTargetBytes() < baseline.residentTargetBytes());
            assertTrue(pinned.maintenanceBudgetFiles() < baseline.maintenanceBudgetFiles());
        }
    }

    @Test
    void sustainedDirtyBuildupQueuesPressureFlushForOldColdRegionButNotFreshHotRegion() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("pressure-flush"), true)) {
            scenario.write("cold-dirty", 4_096);

            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(25), tick -> tick
                    .backlog(24, 2)
                    .write("hot-dirty", 4_096)
                    .read("hot-dirty"));

            StoragePolicyScenarioHarness.State state = scenario.state();
            assertTrue(state.pressureScore() >= 0.60D);
            assertTrue(scenario.shouldConsiderPressureFlush("cold-dirty"));
            assertFalse(scenario.shouldQueueBackgroundFlush("cold-dirty"));
            assertFalse(scenario.shouldConsiderPressureFlush("hot-dirty"));
            assertTrue(StoragePolicyManager.pressureFlushDirtyRegionLimit(256, 24, 2)
                    < LinearConfig.getPressureFlushMaxDirtyRegions());
        }
    }

    @Test
    void realRegionBackupDebtGrowsDuringLoadAndClearsAfterQuietRefresh() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("real-backup-debt"), true)) {
            scenario.realWrite("backed-up", 256);
            scenario.realFlush("backed-up", true);
            scenario.awaitBackupTasks();
            scenario.runTicks(1, null);

            scenario.runTicks(40, tick -> tick
                    .backlog(18, 0)
                    .realWrite("backed-up", 512));
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(5), tick -> tick.backlog(18, 0));

            assertTrue(scenario.backupDebt("backed-up") > 1.0D);
            assertTrue(scenario.dirtyDebt("backed-up") > 0.0D);

            scenario.quietMinutes(8);
            StoragePolicyScenarioHarness.State quiet = scenario.state();
            assertTrue(StoragePolicyManager.shouldScheduleBackupRefresh(quiet.backupDebtScore()));

            scenario.realFlush("backed-up", true);
            scenario.awaitBackupTasks();
            scenario.runTicks(1, null);

            assertEquals(0.0D, scenario.backupDebt("backed-up"));
            assertEquals(0.0D, scenario.dirtyDebt("backed-up"));

            Path backup = tempDir.resolve("real-backup-debt/region/backups/r.0.0.linear.bak");
            assertTrue(Files.exists(backup));
        }
    }

    @Test
    void realColdRegionGetsHigherRecompressPriorityThanRecentlyHotRegion() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("real-priority"), true)) {
            scenario.realWrite("cold", 256);
            scenario.realFlush("cold", false);

            scenario.quietMinutes(10);

            scenario.realWrite("hot", 256);
            scenario.realFlush("hot", false);
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(30), tick -> tick.read("hot"));

            assertTrue(scenario.recompressPriority("cold") > scenario.recompressPriority("hot"));
        }
    }

    @Test
    void backupRefreshesWhenOverdueEvenBelowNormalChangeThreshold() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("backup-max-age"), true)) {
            scenario.realWrite("overdue", 256);
            scenario.realFlush("overdue", true);
            scenario.awaitBackupTasks();
            scenario.runTicks(1, null);

            scenario.quietMinutes(31);
            scenario.realWrite("overdue", 32);
            scenario.quietMinutes(2);

            assertTrue(scenario.backupDebt("overdue") > 0.0D);
            assertTrue(scenario.backupDebt("overdue") < 1.5D);

            scenario.realFlush("overdue", true);
            scenario.awaitBackupTasks();
            scenario.runTicks(1, null);

            assertEquals(0.0D, scenario.backupDebt("overdue"));
        }
    }

    @Test
    void residentTrimPrefersColdIdleRegionsOverHotRecentlyAccessedOnes() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("resident-trim"), true)) {
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(30), tick -> tick
                    .read("hot-a")
                    .read("hot-b")
                    .read("warm"));
            scenario.quietMinutes(12);

            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(10), tick -> tick
                    .read("hot-a")
                    .read("hot-b"));

            long nowNs = scenario.state().nowNs();
            long staleAccessNs = nowNs - 90_000_000_000L;
            long recentAccessNs = nowNs - 5_000_000_000L;
            long residentBytes = 48L * 1024L * 1024L;

            double coldPriority = scenario.residentTrimPriority("cold", staleAccessNs, residentBytes);
            double warmPriority = scenario.residentTrimPriority("warm", staleAccessNs, residentBytes);
            double hotPriority = scenario.residentTrimPriority("hot-a", recentAccessNs, residentBytes);

            assertTrue(StoragePolicyManager.shouldStartResidentTrim(
                    StoragePolicyManager.residentBudgetBytes() + (64L * 1024L * 1024L),
                    Long.MAX_VALUE,
                    nowNs
            ));
            assertTrue(coldPriority > warmPriority);
            assertTrue(warmPriority > hotPriority);
            assertTrue(scenario.shouldTrimResidentRegion("cold", true, false, false, false, staleAccessNs));
            assertFalse(scenario.shouldTrimResidentRegion("hot-a", true, false, false, false, recentAccessNs));
        }
    }

    @Test
    void pregenModeDisablesMaintenanceAndClampsPressureFlushAggressively() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("pregen"), true)) {
            scenario.runTicks(4, tick -> tick
                    .write("pregen-region", 8_192)
                    .flush("pregen-region", 120_000_000L, 4L * 1024L * 1024L, 1L * 1024L * 1024L, 2));
            scenario.quietMinutes(12);
            StoragePolicyScenarioHarness.State baseline = scenario.state();

            scenario.pregenActive(true);
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(30), tick -> tick
                    .pregen(true)
                    .backlog(18, 2)
                    .write("pregen-hot", 8_192));

            StoragePolicyScenarioHarness.State pregen = scenario.state();
            assertTrue(baseline.maintenanceBudgetFiles() > 0);
            assertEquals(0, pregen.maintenanceBudgetFiles());
            assertFalse(pregen.maintenanceAllowed());
            assertTrue(pregen.compressionLevel() <= 2);
            assertEquals(4, StoragePolicyManager.pressureFlushDirtyRegionLimit(256, 18, 2));
        }
    }

    @Test
    void pressureFlushPriorityOrdersColdOldRegionsAheadOfWarmAndFreshHotOnes() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("pressure-ordering"), true)) {
            scenario.write("cold-old", 4_096);
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(8), tick -> tick.backlog(20, 2));

            scenario.write("warm-mid", 4_096);
            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(6), tick -> tick.backlog(20, 2));

            scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(4), tick -> tick
                    .backlog(20, 2)
                    .write("hot-fresh", 4_096)
                    .read("hot-fresh"));

            double coldPriority = scenario.pressureFlushPriority("cold-old");
            double warmPriority = scenario.pressureFlushPriority("warm-mid");
            double hotPriority = scenario.pressureFlushPriority("hot-fresh");

            assertTrue(coldPriority > warmPriority);
            assertTrue(warmPriority > hotPriority);
            assertTrue(scenario.shouldConsiderPressureFlush("cold-old"));
            assertTrue(scenario.shouldConsiderPressureFlush("warm-mid"));
            assertFalse(scenario.shouldConsiderPressureFlush("hot-fresh"));
        }
    }

    @Test
    void burstyReplayDoesNotFlapCompressionAndLoadProfileEveryFewSamples() throws IOException {
        try (StoragePolicyScenarioHarness scenario = StoragePolicyScenarioHarness.create(tempDir.resolve("bursty-stability"), true)) {
            List<String> samples = new ArrayList<>();
            for (int cycle = 0; cycle < 6; cycle++) {
                for (int step = 0; step < 3; step++) {
                    scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(10), tick -> tick
                            .backlog(14, 1)
                            .write("burst", 8_192)
                            .write("burst-2", 4_096));
                    samples.add(snapshotKey(scenario.state()));
                }
                for (int step = 0; step < 9; step++) {
                    scenario.runTicks(StoragePolicyScenarioHarness.ticksForSeconds(10), tick -> tick.backlog(0, 0));
                    samples.add(snapshotKey(scenario.state()));
                }
            }

            int transitions = 0;
            String previous = null;
            for (String sample : samples) {
                if (!sample.equals(previous)) {
                    transitions++;
                    previous = sample;
                }
            }

            StoragePolicyScenarioHarness.State state = scenario.state();
            assertTrue(state.loadProfile().equals("balanced") || state.loadProfile().equals("bursty"));
            assertTrue(transitions <= 18);
        }
    }

    private static String snapshotKey(StoragePolicyScenarioHarness.State state) {
        return state.compressionMode() + "|" + state.loadProfile() + "|" + state.maintenanceAllowed();
    }
}
