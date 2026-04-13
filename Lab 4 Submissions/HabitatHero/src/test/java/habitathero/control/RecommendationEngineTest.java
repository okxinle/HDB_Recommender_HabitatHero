package habitathero.control;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import habitathero.entity.StructuralConstraints;

public class RecommendationEngineTest {

    // Instantiate the engine (passing nulls for dependencies since we are only testing isolated math/logic methods)
    RecommendationEngine engine = new RecommendationEngine(null, null, null, null, null, null);

    /* ==========================================
     * BLACK BOX TESTING: Boundary Values
     * ========================================== */
    /* ==========================================
     * BLACK BOX TESTING: Boundary Values
     * ========================================== */
    @Test
    public void testScoreFromNoiseDb_BoundaryValues() {
        // 1. Capture the actual results for ALL 6 Boundary Values
        // Lower Boundary: 55.0
        double bv1Actual = engine.scoreFromNoiseDb(54.9);
        double bv2Actual = engine.scoreFromNoiseDb(55.0);
        double bv3Actual = engine.scoreFromNoiseDb(55.1);
        
        // Upper Boundary: 85.0
        double bv4Actual = engine.scoreFromNoiseDb(84.9);
        double bv5Actual = engine.scoreFromNoiseDb(85.0);
        double bv6Actual = engine.scoreFromNoiseDb(85.1);

        // 2. Print them vertically to the console for your Lab Report
        System.out.println("\n========== NOISE DB BOUNDARY TESTS ==========");
        
        System.out.println("Test: BV1 (Just-below quiet)");
        System.out.println("  - Input:    54.9");
        System.out.println("  - Expected: 1.0");
        System.out.println("  - Actual:   " + bv1Actual + "\n");

        System.out.println("Test: BV2 (On-boundary quiet)");
        System.out.println("  - Input:    55.0");
        System.out.println("  - Expected: 1.0");
        System.out.println("  - Actual:   " + bv2Actual + "\n");

        System.out.println("Test: BV3 (Just-above quiet)");
        System.out.println("  - Input:    55.1");
        System.out.println("  - Expected: ~0.996");
        System.out.println("  - Actual:   " + bv3Actual + "\n");

        System.out.println("Test: BV4 (Just-below loud)");
        System.out.println("  - Input:    84.9");
        System.out.println("  - Expected: ~0.003");
        System.out.println("  - Actual:   " + bv4Actual + "\n");

        System.out.println("Test: BV5 (On-boundary loud)");
        System.out.println("  - Input:    85.0");
        System.out.println("  - Expected: 0.0");
        System.out.println("  - Actual:   " + bv5Actual + "\n");

        System.out.println("Test: BV6 (Just-above loud)");
        System.out.println("  - Input:    85.1");
        System.out.println("  - Expected: 0.0");
        System.out.println("  - Actual:   " + bv6Actual);
        System.out.println("=============================================\n");

        // 3. Run the automated assertions
        // The delta (0.01) allows for slight floating-point math rounding differences
        assertEquals(1.0, bv1Actual);
        assertEquals(1.0, bv2Actual);
        assertEquals(0.996, bv3Actual, 0.01);
        assertEquals(0.003, bv4Actual, 0.01);
        assertEquals(0.0, bv5Actual);
        assertEquals(0.0, bv6Actual);
    }

    /* ==========================================
     * BLACK BOX TESTING: Equivalence Classes
     * ========================================== */
    @Test
    public void testValidateConstraints_EquivalenceClasses() {
        System.out.println("\n========== STRUCTURAL CONSTRAINTS EC TESTS ==========");

        // ---------------------------------------------------------
        // EC1: All Valid Inputs (Valid Flat, Valid Towns)
        // ---------------------------------------------------------
       StructuralConstraints ec1Constraints = new StructuralConstraints();
        ec1Constraints.setPreferredFlatType("4 Room"); 
        ec1Constraints.setPreferredTowns(List.of("Bishan", "Ang Mo Kio"));
        
        System.out.println("Test: EC1 (All Valid Inputs)");
        System.out.println("  - Inputs:");
        System.out.println("      * Flat Type: '4 Room'");
        System.out.println("      * Towns:     ['Bishan', 'Ang Mo Kio']");
        System.out.println("  - Expected: \n      No Exception Thrown");
        
        // Oracle for EC1: It should run silently without crashing
        try {
            engine.validateStructuralConstraints(ec1Constraints);
            System.out.println("  - Actual:   \n      No Exception Thrown\n");
        } catch (Exception e) {
            // If it crashes, print the EXACT reason HDBDataConstants rejected it!
            fail("EC1 failed unexpectedly. Reason: " + e.getMessage()); 
        }
        // ---------------------------------------------------------
        // EC2: Invalid Flat Type, Valid Town
        // ---------------------------------------------------------
        StructuralConstraints ec2Constraints = new StructuralConstraints();
        ec2Constraints.setPreferredFlatType("10-ROOM"); // The ONE Invalid Input
        ec2Constraints.setPreferredTowns(List.of("BISHAN")); // Valid Baseline
        
        System.out.println("Test: EC2 (Invalid Flat Type)");
        System.out.println("  - Inputs:");
        System.out.println("      * Flat Type: '10-ROOM'");
        System.out.println("      * Towns:     ['BISHAN']");
        System.out.println("  - Expected: \n      IllegalArgumentException (Invalid flat type)");
        
        // Oracle for EC2: Expect an exception
        Exception ec2Exception = assertThrows(IllegalArgumentException.class, () -> {
            engine.validateStructuralConstraints(ec2Constraints);
        });
        
        System.out.println("  - Actual:   \n      " + ec2Exception.getClass().getSimpleName() + " \n      (" + ec2Exception.getMessage() + ")\n");
        assertTrue(ec2Exception.getMessage().contains("Invalid flat type"));

        // ---------------------------------------------------------
        // EC3: Valid Flat Type, Invalid Town
        // ---------------------------------------------------------
        // ---------------------------------------------------------
        // EC3: Valid Flat Type, Invalid Town
        // ---------------------------------------------------------
        StructuralConstraints ec3Constraints = new StructuralConstraints();
        ec3Constraints.setPreferredFlatType("4 Room"); 
        ec3Constraints.setPreferredTowns(List.of("Gotham")); // The ONE Invalid Input
        
        System.out.println("Test: EC3 (Invalid Town)");
        System.out.println("  - Inputs:");
        System.out.println("      * Flat Type: '4 Room'");
        System.out.println("      * Towns:     ['Gotham']");
        System.out.println("  - Expected: \n      IllegalArgumentException (Invalid town)");

        // Oracle for EC3: Expect an exception
        Exception ec3Exception = assertThrows(IllegalArgumentException.class, () -> {
            engine.validateStructuralConstraints(ec3Constraints);
        });
        
        // Print the Actual result, using \n to wrap the long message to the next line
        System.out.println("  - Actual:   \n      " + ec3Exception.getClass().getSimpleName() + " \n      (" + ec3Exception.getMessage() + ")");
        System.out.println("=====================================================\n");
        
        // Run the automated check
        assertTrue(ec3Exception.getMessage().contains("Invalid town"));
    }

    /* ==========================================
     * WHITE BOX TESTING: Control Flow (Basis Paths)
     * ========================================== */
    @Test
    public void testClamp01_AllPaths() {
        double negativeActual = engine.clamp01(-0.5);
        double overMaxActual = engine.clamp01(1.5);
        double validActual = engine.clamp01(0.5);

        System.out.println("============= CLAMP01 BASIS PATHS =============");
        System.out.println("Path 1 (Negative Input)");
        System.out.println("  - Input:    -0.5");
        System.out.println("  - Expected: 0.0");
        System.out.println("  - Actual:   " + negativeActual + "\n");

        System.out.println("Path 2 (Over Max Input)");
        System.out.println("  - Input:    1.5");
        System.out.println("  - Expected: 1.0");
        System.out.println("  - Actual:   " + overMaxActual + "\n");

        System.out.println("Path 3 (Within Bounds)");
        System.out.println("  - Input:    0.5");
        System.out.println("  - Expected: 0.5");
        System.out.println("  - Actual:   " + validActual);
        System.out.println("===============================================\n");

        assertEquals(0.0, negativeActual);
        assertEquals(1.0, overMaxActual);
        assertEquals(0.5, validActual);
    }

    /* ==========================================
     * WHITE BOX TESTING: Control Flow (Basis Paths)
     * Target: calculateLeaseScore()
     * ========================================== */
    @Test
    public void testCalculateLeaseScore_AllPaths() {
        // 1. Capture the actual results
        double path1Actual = engine.calculateLeaseScore(30);
        double path2Actual = engine.calculateLeaseScore(99);
        double path3Actual = engine.calculateLeaseScore(60);

        // 2. Print them vertically to the console for your Lab Report
        System.out.println("============= LEASE SCORE BASIS PATHS =============");
        System.out.println("Path 1 (Low Lease)");
        System.out.println("  - Input:    30");
        System.out.println("  - Expected: 0.0");
        System.out.println("  - Actual:   " + path1Actual + "\n");

        System.out.println("Path 2 (High Lease)");
        System.out.println("  - Input:    99");
        System.out.println("  - Expected: 1.0");
        System.out.println("  - Actual:   " + path2Actual + "\n");

        System.out.println("Path 3 (Within Bounds)");
        System.out.println("  - Input:    60");
        System.out.println("  - Expected: ~0.363");
        System.out.println("  - Actual:   " + path3Actual);
        System.out.println("===================================================\n");

        // 3. Run the automated assertions
        // The delta (0.01) allows for floating-point math rounding differences on Path 3
        assertEquals(0.0, path1Actual);
        assertEquals(1.0, path2Actual);
        assertEquals(0.363, path3Actual, 0.01);
    }
}