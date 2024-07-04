package com.tankvisualizationbackend.controller;


import com.tankvisualizationbackend.bean.Tank;
import com.tankvisualizationbackend.service.TankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tanks")
public class TankController {

    @Autowired
    private TankService tankService;

    @GetMapping
    public List<Tank> getAllTanks() {
        return tankService.getAllTanks();
    }

    @PostMapping
    public Tank createTank(@RequestBody Tank tank) {
        return tankService.createTank(tank);
    }

    @DeleteMapping("/{id}")
    public void deleteTank(@PathVariable Long id) {
        tankService.deleteTank(id);
    }


}

