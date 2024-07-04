class WebSocketService {
    constructor() {
      this.socket = null;
      this.messageHandler = null;
      this.callbacks = {};
      this.queue = []; // 确保初始化队列
    }
  
    connect() {
      // 连接到 WebSocket 服务器
      this.socket = new WebSocket('ws://localhost:3602/tank-ws');
  
      // WebSocket 连接建立时的处理程序
      this.socket.onopen = () => {
        // 连接建立后处理所有等待发送的消息
        while (this.queue.length > 0) {
            const { message, callback } = this.queue.shift();
            this.sendMessage(message, callback);
        }
    };
  
      this.socket.onmessage = (event) => {
        const response = JSON.parse(event.data);
        if (this.responseCallback) {
          this.responseCallback(response);
        } else if (this.messageHandler) {
          this.messageHandler(response);
        }
      };
  
      // WebSocket 连接关闭时的处理程序
      this.socket.onclose = () => {
        console.log('WebSocket connection closed');
      };
  
      // WebSocket 错误时的处理程序
      this.socket.onerror = (error) => {
        console.error('WebSocket error', error);
      };
    }
  
    setMessageHandler(handler) {
      // 设置消息处理程序
      this.messageHandler = handler;
    }

    sendWebSocketMessage(message, type, callback) {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        console.warn('WebSocket connection is not open. Waiting for connection to be established.');
        setTimeout(() => {
          this.sendWebSocketMessage(message, type, callback);
        }, 500);
        return;
      }
    
      this.socket.send(JSON.stringify(message));
    
      const tempHandler = (event) => {
        const parsedMessage = JSON.parse(event.data);
        console.log('Parsed Message:', parsedMessage); // 调试打印
        if (parsedMessage.type === type && (parsedMessage.id || parsedMessage.tanks || parsedMessage.bullets)) {
          callback(parsedMessage);
          this.socket.removeEventListener('message', tempHandler);
        }
      };
    
      this.socket.addEventListener('message', tempHandler);
    }
    
    sendMessage(message, callback) {
      this.sendWebSocketMessage(message, message.type, callback);
    }
    
    initialize(callback) {
      const message = { type: 'initialize' };
      this.sendWebSocketMessage(message, 'initialize', (parsedMessage) => {
        callback(parsedMessage.tanks, parsedMessage.bullets);
      });
    }
 

  }
  
  const webSocketService = new WebSocketService();
  export default webSocketService;
  