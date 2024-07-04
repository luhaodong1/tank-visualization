package com.tankvisualizationbackend.service;


import com.tankvisualizationbackend.bean.Tank;
import com.tankvisualizationbackend.repository.TankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;

@Service
public class TankService {

    @Autowired
    private TankRepository tankRepository;

    public List<Tank> getAllTanks() {
        return tankRepository.findAll();
    }

    public Tank createTank(Tank tank) {
        return tankRepository.save(tank);
    }

    public void deleteTank(Long id) {
        tankRepository.deleteById(id);
    }


    @Transactional
    public void updateAllTankPositions(List<Tank> tanks) {
        tankRepository.saveAll(tanks);
    }

}



