package habitathero.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import habitathero.entity.PointOfInterest;

@Repository
public interface PoiRepository extends JpaRepository<PointOfInterest, Long> {

    @Query(value = """
            SELECT COUNT(*)
            FROM point_of_interest p
            WHERE p.category = :category
              AND (
                    6371.0 * 2 * ASIN(
                        SQRT(
                            POWER(SIN(RADIANS((p.latitude - :lat) / 2.0)), 2)
                            + COS(RADIANS(:lat)) * COS(RADIANS(p.latitude))
                            * POWER(SIN(RADIANS((p.longitude - :lon) / 2.0)), 2)
                        )
                    )
                ) <= :radiusKm
            """, nativeQuery = true)
    int countNearbyPOIs(@Param("lat") double lat,
                        @Param("lon") double lon,
                        @Param("category") String category,
                        @Param("radiusKm") double radiusKm);

    @Query(value = """
            SELECT p.*
            FROM point_of_interest p
            WHERE p.category = :category
              AND (
                    6371.0 * 2 * ASIN(
                        SQRT(
                            POWER(SIN(RADIANS((p.latitude - :lat) / 2.0)), 2)
                            + COS(RADIANS(:lat)) * COS(RADIANS(p.latitude))
                            * POWER(SIN(RADIANS((p.longitude - :lon) / 2.0)), 2)
                        )
                    )
                ) <= :radiusKm
            ORDER BY (
                    6371.0 * 2 * ASIN(
                        SQRT(
                            POWER(SIN(RADIANS((p.latitude - :lat) / 2.0)), 2)
                            + COS(RADIANS(:lat)) * COS(RADIANS(p.latitude))
                            * POWER(SIN(RADIANS((p.longitude - :lon) / 2.0)), 2)
                        )
                    )
                ) ASC
            """, nativeQuery = true)
    java.util.List<PointOfInterest> findNearbyPOIs(@Param("lat") double lat,
                                                   @Param("lon") double lon,
                                                   @Param("category") String category,
                                                   @Param("radiusKm") double radiusKm);
}
