package com.tankvisualizationbackend.repository;


import com.tankvisualizationbackend.bean.Tank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface TankRepository extends JpaRepository<Tank, Long> {
    // 根据 ID 删除坦克
    void deleteById(Long id);
}

