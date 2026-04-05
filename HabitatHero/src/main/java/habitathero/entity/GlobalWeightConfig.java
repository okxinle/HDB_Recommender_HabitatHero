package habitathero.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "global_weight_config")
public class GlobalWeightConfig {

    @Id
    @Column(name = "config_key")
    private String configKey;

    @Column(name = "weight_value", nullable = false)
    private double weightValue;

    public GlobalWeightConfig() {}

    public GlobalWeightConfig(String configKey, double weightValue) {
        this.configKey = configKey;
        this.weightValue = weightValue;
    }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public double getWeightValue() { return weightValue; }
    public void setWeightValue(double weightValue) { this.weightValue = weightValue; }
}