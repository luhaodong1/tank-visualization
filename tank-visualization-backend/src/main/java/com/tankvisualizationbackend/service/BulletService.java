package com.tankvisualizationbackend.service;

import com.tankvisualizationbackend.bean.Bullet;
import com.tankvisualizationbackend.bean.Tank;
import com.tankvisualizationbackend.repository.BulletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BulletService {
    @Autowired
    private BulletRepository bulletRepository;

    public List<Bullet> getAllBullets() {
        return bulletRepository.findAll();
    }

    public void updateAllBullet(List<Bullet> bullets) {
        bulletRepository.saveAll(bullets);
    }

    public Bullet createBullet(Bullet bullet) {
        return bulletRepository.save(bullet);
    }

    public void deleteBullet(Long id) {
        bulletRepository.deleteById(id);
    }
}
