<template>
  <div class="control-container">
    <button @click="openCreateTankDialog">创建</button>
    <button @click="confirmDeleteSelectedTank" :disabled="!selectedTank">删除</button>
    <button @click="toggleGame">{{ gameRunning ? '暂停' : '开始' }}</button>
  </div>

  <div id="canvas-container">
    <div v-if="showCreateTankDialog" class="dialog">
      <label for="tankName">请给坦克命名:</label>
      <input type="text" v-model="newTankName" id="tankName">
      
      <div class="tank-options">
        <label>选择坦克:</label>
        <div class="tank-option" v-for="tank in tankStyles" :key="tank" @click="selectTankStyle(tank)">
          <img :src="`/tank_assets/${tank}`" :class="{ selected: newTankStyle === tank }" />
        </div>
      </div>

      <button @click="createNewTank">创建</button>
      <button @click="closeCreateTankDialog">取消</button>
    </div>

    <div v-if="showDeleteConfirmation" class="dialog">
      <p>是否确定删除这个坦克？</p>
      <p>名字: {{ selectedTank.name }}</p>
      <p><img :src="`/tank_assets/${selectedTank.style}`" /></p>
      <button @click="deleteSelectedTank">Yes</button>
      <button @click="closeDeleteConfirmation">No</button>
    </div>

    <div v-if="!gameRunning" class="overlay">
      <button class="start-button" @click="toggleGame">开始</button>
    </div>

    <canvas ref="tankCanvas" @mousedown="startDrag" @mousemove="drag" @mouseup="endDrag" @mouseleave="endDrag" @click="handleCanvasClick"></canvas>
  </div>
</template>

<script>
import webSocketService from '../network/websocketService';

export default {
  name: 'TankControl',
  data() {
    return {
      tanks: [], // 保存所有坦克的数组
      bullets: [], // 保存所有炮弹的数组
      explosions: [], // 保存所有爆炸的数组
      selectedTank: null, // 当前选中的坦克
      canvas: null, // 画布元素
      context: null, // 画布上下文
      showCreateTankDialog: false, // 是否显示创建坦克对话框
      showDeleteConfirmation: false, // 是否显示删除确认对话框
      newTankName: '', // 新坦克的名称
      newTankStyle: '', // 新坦克的样式
      tankStyles: ["tank_blue.png", "tank_green.png", "tank_dark.png", "tank_red.png", "tank_sand.png"],
      isDragging: false, // 是否正在拖拽
      dragOffsetX: 0, // 拖拽偏移量X
      dragOffsetY: 0, // 拖拽偏移量Y
      nextTankId: 1, // 下一个坦克的唯一编号
      blinkInterval: null, // 坦克闪烁的定时器
      blinkSpeed: 250, // 闪烁速度参数（毫秒）
      movementInterval: null, // 随机移动坦克的定时器
      bulletSpeed: 40, // 炮弹速度
      gameRunning: false,
      keysPressed: {}, // 记录当前按下的按键
      lastShootTime: 0, // 记录上一次射击的时间
      shootInterval: 200, // 控制射击间隔时间（毫秒）
      moveStep: 2,
    };
  },
  mounted() {
    this.canvas = this.$refs.tankCanvas;
    this.context = this.canvas.getContext('2d');
    this.canvas.width = 800;
    this.canvas.height = 600;
    window.addEventListener('keydown', this.handleKeyDown);
    window.addEventListener('keyup', this.handleKeyUp);

    // 连接WebSocket并设置消息处理程序
    webSocketService.connect();
    webSocketService.setMessageHandler(this.handleWebSocketMessage);

    // 初始化时从后端获取坦克数据和子弹数据
    webSocketService.initialize((tanks, bullets) => {
      this.tanks = tanks;  // 后端已经包含canDrag, isBlinking, hasBeenDragged属性
      this.bullets = bullets; // 获取所有子弹数据
      this.drawTanks();
    });
    this.pauseGame();
    this.startKeyPressHandling(); // 开始按键处理循环
    // this.startRandomMovement();
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeyDown);
    window.removeEventListener('keyup', this.handleKeyUp);
    if (this.blinkInterval) {
      clearInterval(this.blinkInterval);
    }
    if (this.movementInterval) {
      clearInterval(this.movementInterval);
    }
    webSocketService.disconnect();
  },
  methods: {
    openCreateTankDialog() {
      this.showCreateTankDialog = true;
    },
    closeCreateTankDialog() {
      this.showCreateTankDialog = false;
      this.newTankName = '';
      this.newTankStyle = '';
    },
    selectTankStyle(tank) {
      this.newTankStyle = tank;
    },
    createNewTank() {
      if (this.newTankName.trim() === '' || this.newTankStyle === '') {
        alert('请确认命名并选择坦克！');
        return;
      }
      const x = this.canvas.width / 2;
      const y = this.canvas.height - 20; // 放置在画布下方正中间
      const tank = { name: this.newTankName, x, y, style: this.newTankStyle, direction: 'down', canDrag: true, isBlinking: true, hasBeenDragged: false };

      webSocketService.sendMessage({ type: 'createTank', tank }, (responseTank) => {
        console.log(responseTank);
        // 回调函数中更新前端的坦克对象
        tank.id = responseTank.id;
        this.tanks.push(tank);
        this.drawTanks();
      });
      this.closeCreateTankDialog();
    },
    confirmDeleteSelectedTank() {
      if (this.selectedTank) {
        this.showDeleteConfirmation = true;
      }
    },
    closeDeleteConfirmation() {
      this.showDeleteConfirmation = false;
    },
    deleteSelectedTank() {
      if (this.selectedTank) {
        const tank = this.selectedTank;
        const index = this.tanks.indexOf(this.selectedTank);
        if (index !== -1) {
          this.tanks.splice(index, 1);
          this.selectedTank = null;
          this.drawTanks();
          webSocketService.sendMessage({ type: 'deleteTank', tank });
        }
        this.showDeleteConfirmation = false;
      }
    },
    toggleGame() {
      this.gameRunning = !this.gameRunning;
      if (this.gameRunning) {
        this.startGame();
      } else {
        this.pauseGame();
      }
    },
    startGame() {
      this.startRandomMovement();
      this.startMovingBullets();  // 改成 startMovingBullets
      this.drawTanks();
    },
    pauseGame() {
      if (this.movementInterval) {
        clearInterval(this.movementInterval);
      }
      if (this.bulletInterval) {
        clearInterval(this.bulletInterval);  // 停止子弹移动
      }
    },
    drawTanks() {
      this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
      
      // 绘制坦克
      this.tanks.forEach(tank => {
        if (tank.isBlinking == true) {
          if (Math.floor(Date.now() / this.blinkSpeed) % 2 === 0) {
            return; // 闪烁效果
          }
        }

        this.context.save();
        this.context.translate(tank.x, tank.y);
        this.context.rotate(this.getRotationAngle(tank.direction));
        const image = new Image();
        image.src = `/tank_assets/${tank.style}`;
        this.context.drawImage(image, -image.width / 2, -image.height / 2);
        
        if (this.selectedTank === tank) {
          this.context.strokeStyle = 'red';
          this.context.lineWidth = 2;
          this.context.strokeRect(-image.width / 2, -image.height / 2, image.width, image.height);
        }

        this.context.restore();

        if (tank.canDrag == true) {
          this.context.save();
          this.context.translate(tank.x, tank.y);
          this.context.strokeStyle = 'blue';
          this.context.strokeRect(-image.width / 2, -image.height / 2, image.width, image.height);
          this.context.restore();
        }
      });

      // 绘制炮弹
      this.bullets.forEach(bullet => {
        this.context.save();
        this.context.translate(bullet.x, bullet.y);
        this.context.rotate(this.getRotationAngle(bullet.direction) + Math.PI); // 旋转180度
        const bulletImage = new Image();
        bulletImage.src = `/tank_assets/${bullet.style}`;
        this.context.drawImage(bulletImage, -bulletImage.width / 2, -bulletImage.height / 2);
        this.context.restore();
      });

      // 绘制爆炸效果
      this.explosions.forEach(explosion => {
        const explosionImage = new Image();
        explosionImage.src = `/tank_assets/${explosion.frame}`;
        this.context.drawImage(explosionImage, explosion.x - explosionImage.width / 2, explosion.y - explosionImage.height / 2);
      });
    },
    getRotationAngle(direction) {
      switch (direction) {
        case 'up':
          return Math.PI; // 调整为180度
        case 'right':
          return -Math.PI / 2; // 调整为-90度
        case 'down':
          return 0; // 调整为0度
        case 'left':
          return Math.PI / 2; // 调整为90度
      }
    },
    startDrag(event) {
      const rect = this.canvas.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      this.tanks.forEach(tank => {
        const image = new Image();
        image.src = `/tank_assets/${tank.style}`;
        if (tank.canDrag==true && x >= tank.x - image.width / 2 && x <= tank.x + image.width / 2 &&
            y >= tank.y - image.height / 2 && y <= tank.y + image.height / 2) {
          this.selectedTank = tank;
          this.isDragging = true;
          this.dragOffsetX = x - tank.x;
          this.dragOffsetY = y - tank.y;
        }
      });
    },
    drag(event) {
      if (this.isDragging && this.selectedTank && this.selectedTank.canDrag==true) {
        const rect = this.canvas.getBoundingClientRect();
        const x = event.clientX - rect.left;
        const y = event.clientY - rect.top;
        const image = new Image();
        image.src = `/tank_assets/${this.selectedTank.style}`;
        this.selectedTank.x = Math.min(this.canvas.width - image.width / 2, Math.max(image.width / 2, x - this.dragOffsetX));
        this.selectedTank.y = Math.min(this.canvas.height - image.height / 2, Math.max(image.height / 2, y - this.dragOffsetY));
        this.drawTanks();
      }
    },
    endDrag() {
      if (this.selectedTank && this.isDragging) {
        this.selectedTank.canDrag = false;
        this.selectedTank.isBlinking = false; // 停止闪烁
        this.selectedTank.hasBeenDragged = true; // 标记坦克已被拖动过
        this.sendTankUpdate(); // 更新后发送位置到后端
      }
      this.isDragging = false;
      this.drawTanks();
    },
    handleCanvasClick(event) {
      const rect = this.canvas.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      let clickedOnTank = false;
      this.tanks.forEach(tank => {
        const image = new Image();
        image.src = `/tank_assets/${tank.style}`;
        if (x >= tank.x - image.width / 2 && x <= tank.x + image.width / 2 &&
            y >= tank.y - image.height / 2 && y <= tank.y + image.height / 2) {
          this.selectedTank = tank;
          clickedOnTank = true;
        }
      });
      if (!clickedOnTank) {
        this.selectedTank = null;
      }
      this.drawTanks();
    },
    handleKeyDown(event) {
      this.keysPressed[event.key] = true;
    },
    handleKeyUp(event) {
      this.keysPressed[event.key] = false;
    },
    startKeyPressHandling() {
      const loop = () => {
        if (this.selectedTank) {
          const now = Date.now();

          if (this.keysPressed['ArrowUp']) {
            this.moveTank(this.selectedTank, 'up');
          }
          if (this.keysPressed['ArrowRight']) {
            this.moveTank(this.selectedTank, 'right');
          }
          if (this.keysPressed['ArrowDown']) {
            this.moveTank(this.selectedTank, 'down');
          }
          if (this.keysPressed['ArrowLeft']) {
            this.moveTank(this.selectedTank, 'left');
          }
          if (this.keysPressed[' '] && now - this.lastShootTime > this.shootInterval) {
            this.shoot();
            this.lastShootTime = now;
          }

          this.sendTankUpdate();
          this.drawTanks();
        }
        requestAnimationFrame(loop);
      };
      requestAnimationFrame(loop);
    },
    moveTank(tank, direction) {
      const image = new Image();
      image.src = `/tank_assets/${tank.style}`;
      switch (direction) {
        case 'up':
          tank.direction = 'up';
          tank.y = Math.max(image.height / 2, tank.y - this.moveStep);
          break;
        case 'right':
          tank.direction = 'right';
          tank.x = Math.min(this.canvas.width - image.width / 2, tank.x + this.moveStep);
          break;
        case 'down':
          tank.direction = 'down';
          tank.y = Math.min(this.canvas.height - image.height / 2, tank.y + this.moveStep);
          break;
        case 'left':
          tank.direction = 'left';
          tank.x = Math.max(image.width / 2, tank.x - this.moveStep);
          break;
      }
    },
    shoot() {
      const currentTime = Date.now();
      if (!this.selectedTank || (currentTime - this.lastShootTime) < this.shootInterval) return;
      
      const bullet = {
        x: this.selectedTank.x,
        y: this.selectedTank.y,
        direction: this.selectedTank.direction,
        style: this.selectedTank.style.replace('.png', '_bullet.png')
      };
      // 调整炮弹初始位置使其不与坦克重叠
      const image = new Image();
      image.src = `/tank_assets/${this.selectedTank.style}`;
      switch (bullet.direction) {
        case 'up':
          bullet.y -= image.height / 2 + 10;
          break;
        case 'right':
          bullet.x += image.width / 2 + 10;
          break;
        case 'down':
          bullet.y += image.height / 2 + 10;
          break;
        case 'left':
          bullet.x -= image.width / 2 + 10;
          break;
      }
      
      webSocketService.sendMessage({ type: 'createBullet', bullet }, (responseBullet) => {
        console.log(responseBullet);
        bullet.id = responseBullet.id;
        this.bullets.push(bullet);
        this.drawTanks(); 
      });

      this.lastShootTime = currentTime;
    },
    startMovingBullets() {
      if (this.bulletInterval) {
        clearInterval(this.bulletInterval);
      }
      
      this.bulletInterval = setInterval(() => {
        this.bullets.forEach(bullet => {
          switch (bullet.direction) {
            case 'up':
              bullet.y -= this.bulletSpeed;
              break;
            case 'right':
              bullet.x += this.bulletSpeed;
              break;
            case 'down':
              bullet.y += this.bulletSpeed;
              break;
            case 'left':
              bullet.x -= this.bulletSpeed;
              break;
          }
          this.sendBulletUpdate();
          this.checkBulletCollision(bullet);
        });
        this.drawTanks();
      }, 100);
    },
    checkBulletCollision(bullet) {
      // 检查炮弹是否碰撞到坦克
      this.tanks.forEach(tank => {
        const dx = bullet.x - tank.x;
        const dy = bullet.y - tank.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        const image = new Image();
        image.src = `/tank_assets/${tank.style}`;
        if (distance < image.width / 2) {
          this.createExplosion(tank.x, tank.y);
          this.deleteTank(tank);
          this.deleteBullet(bullet);
        }
      });
      
      // 检查炮弹是否碰撞到边界
      if (bullet.x < 0 || bullet.x > this.canvas.width || bullet.y < 0 || bullet.y > this.canvas.height) {
        this.deleteBullet(bullet);
      }
    },
    createExplosion(x, y) {
      this.explosions.push({ x, y, frame: 'explosion1.png' });
      setTimeout(() => {
        this.explosions = this.explosions.map(explosion => {
          if (explosion.x === x && explosion.y === y) {
            return { ...explosion, frame: 'explosion2.png' };
          }
          return explosion;
        });
        this.drawTanks();
      }, 250); // 切换到第二帧爆炸图像
      setTimeout(() => {
        this.explosions = this.explosions.filter(explosion => explosion.x !== x || explosion.y !== y);
        this.drawTanks();
      }, 500); // 爆炸效果持续0.5秒
    },
    deleteTank(tank) {
      const index = this.tanks.indexOf(tank);
      if (index !== -1) {
        this.tanks.splice(index, 1);
        this.sendTankUpdate();
      }
      webSocketService.sendMessage({ type: 'deleteTank', tank });
    },
    deleteBullet(bullet) {
      const index = this.bullets.indexOf(bullet);
      if (index !== -1) {
        this.bullets.splice(index, 1);
        webSocketService.sendMessage({ type: 'deleteBullet', bullet });
      }
    },
    startRandomMovement() {
      this.movementInterval = setInterval(this.randomMoveTanks, 100);
    },
    randomMoveTanks() {
      this.tanks.forEach(tank => {
        if (this.selectedTank !== tank && tank.hasBeenDragged) {
          if (Math.random() < 0.05) {
            tank.direction = ['up', 'right', 'down', 'left'][Math.floor(Math.random() * 4)];
          }
          const image = new Image();
          image.src = `/tank_assets/${tank.style}`;
          switch (tank.direction) {
            case 'up':
              tank.y = Math.max(image.height / 2, tank.y - 5);
              break;
            case 'right':
              tank.x = Math.min(this.canvas.width - image.width / 2, tank.x + 5);
              break;
            case 'down':
              tank.y = Math.min(this.canvas.height - image.height / 2, tank.y + 5);
              break;
            case 'left':
              tank.x = Math.max(image.width / 2, tank.x - 5);
              break;
          }
          this.checkCollisions(tank);
        }
      });
      this.sendTankUpdate();
      this.drawTanks();
    },
    checkCollisions(tank) {
      this.tanks.forEach(otherTank => {
        if (otherTank !== tank) {
          const dx = otherTank.x - tank.x;
          const dy = otherTank.y - tank.y;
          const distance = Math.sqrt(dx * dx + dy * dy);
          const image = new Image();
          image.src = `/tank_assets/${tank.style}`;
          if (distance < image.width) {
            this.createExplosion(tank.x, tank.y);
            this.createExplosion(otherTank.x, otherTank.y);
            this.deleteTank(tank);
            this.deleteTank(otherTank);
          }
        }
      });
    },
    handleWebSocketMessage(message) {
      if (message.type === 'tankUpdate') {
        this.tanks = message.tanks;
        this.drawTanks();
      } else if (message.type === 'bulletUpdate') {
        this.bullets.push(message.bullet);
        this.drawTanks();
      } else if (message.type === 'initialize') {
        this.tanks = message.tanks;
        this.bullets = message.bullets;
        this.drawTanks();
      }
    },
    sendTankUpdate() {
      webSocketService.sendMessage({ type: 'tankUpdate', tanks: this.tanks });
    },
    sendBulletUpdate() {
      webSocketService.sendMessage({ type: 'bulletUpdate', bullets: this.bullets });
    }
  }
};
</script>

<style scoped>
#canvas-container {
  position: relative;
  width: 800px;
  height: 600px;
  border: 1px solid #000;
  margin: 0 auto; /* 使画布居中 */
}

.control-container {
  text-align: center; /* 使按钮居中 */
  margin-bottom: 10px;
}

.dialog {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  padding: 20px;
  border: 1px solid #ccc;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.dialog label {
  display: block;
  margin-bottom: 10px;
}

.dialog input {
  display: block;
  margin-bottom: 10px;
  width: 100%;
  padding: 5px;
  box-sizing: border-box;
}

.dialog button {
  margin-right: 10px;
}

.tank-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.tank-option {
  cursor: pointer;
}

.tank-option img {
  width: 40px;
  height: 40px;
}

.tank-option img.selected {
  border: 2px solid blue;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10;
}

.start-button {
  font-size: 24px;
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}
</style>
