package com.tankvisualizationbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tankvisualizationbackend.bean.Bullet;
import com.tankvisualizationbackend.bean.Tank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TankWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public TankService tankService;
    @Autowired
    public BulletService bulletService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
//        System.out.println(data);
        String type = (String) data.get("type");
        if ("tankUpdate".equals(type)) {
            // 处理坦克更新逻辑
            List<Tank> tanks = objectMapper.convertValue(data.get("tanks"), new TypeReference<List<Tank>>() {
            });
            tankService.updateAllTankPositions(tanks);
        } else if ("createTank".equals(type)) {
            Tank tank = objectMapper.convertValue(data.get("tank"), Tank.class);
            Tank createdTank = tankService.createTank(tank);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "createTank");
            response.put("id", createdTank.getId());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else if ("initialize".equals(type)) {
            List<Tank> tanks = tankService.getAllTanks();
            List<Bullet> bullets = bulletService.getAllBullets();
            Map<String, Object> response = new HashMap<>();
            response.put("type", "initialize");
            response.put("tanks", tanks);
            response.put("bullets", bullets);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else if ("deleteTank".equals(type)) {
            Tank tank = objectMapper.convertValue(data.get("tank"), Tank.class);
            tankService.deleteTank(tank.getId());
        } else if ("bulletUpdate".equals(type)) {
            List<Bullet> bullets = objectMapper.convertValue(data.get("bullets"), new TypeReference<List<Bullet>>() {
            });
            bulletService.updateAllBullet(bullets);
        } else if ("createBullet".equals(type)) {
            Bullet bullet = objectMapper.convertValue(data.get("bullet"), Bullet.class);
            Bullet createdBullet = bulletService.createBullet(bullet);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "createBullet");
            response.put("id", createdBullet.getId());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else if ("deleteBullet".equals(type)) {
            Bullet bullet = objectMapper.convertValue(data.get("bullet"), Bullet.class);
            bulletService.deleteBullet(bullet.getId());
        }
    }
}
