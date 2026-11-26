package com.dlog.asset.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, LocalDate> {

    List<AssetSnapshot> findAllByOrderBySnapDateAsc();
}
