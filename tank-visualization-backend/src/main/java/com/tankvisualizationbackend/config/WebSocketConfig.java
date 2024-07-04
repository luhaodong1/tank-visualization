package com.tankvisualizationbackend.config;


import com.tankvisualizationbackend.service.TankWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器，设置 WebSocket 端点，并允许所有源连接
        registry.addHandler(tankWebSocketHandler(), "/tank-ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public TankWebSocketHandler tankWebSocketHandler() {
        // 定义 WebSocket 处理器的 bean
        return new TankWebSocketHandler();
    }
}

