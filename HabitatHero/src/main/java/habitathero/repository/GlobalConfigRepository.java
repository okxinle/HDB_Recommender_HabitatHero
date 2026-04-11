package habitathero.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import habitathero.entity.GlobalWeightConfig;

public interface GlobalConfigRepository extends JpaRepository<GlobalWeightConfig, String> {}
