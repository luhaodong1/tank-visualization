package com.tankvisualizationbackend.bean;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "tank")
public class Tank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String style;
    private String direction;
    private int x;
    private int y;

    private String canDrag;
    private String isBlinking;
    private String hasBeenDragged;

    public Tank() {
        // 无参构造函数
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCanDrag() {
        return canDrag;
    }

    public void setCanDrag(String canDrag) {
        this.canDrag = canDrag;
    }

    public String getIsBlinking() {
        return isBlinking;
    }

    public void setIsBlinking(String isBlinking) {
        this.isBlinking = isBlinking;
    }

    public String getHasBeenDragged() {
        return hasBeenDragged;
    }

    public void setHasBeenDragged(String hasBeenDragged) {
        this.hasBeenDragged = hasBeenDragged;
    }
}

