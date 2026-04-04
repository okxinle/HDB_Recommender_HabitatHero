package habitathero.control;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import habitathero.GeoSpatialAnalysis.src.HDBBuildingMgr;
import habitathero.GeoSpatialAnalysis.src.TransportLineMgr;

@Configuration
public class GeoSpatialManagerConfig {

    @Bean
    public HDBBuildingMgr hdbBuildingMgr() {
        return HDBBuildingMgr.getInstance();
    }

    @Bean
    public TransportLineMgr transportLineMgr() {
        return TransportLineMgr.getInstance();
    }
}
