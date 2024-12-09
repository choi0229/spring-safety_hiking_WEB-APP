// server.js
const WebSocket = require('ws');

// WebSocket 서버 생성
const wss = new WebSocket.Server({ port: 3000 });

// 클라이언트가 연결되었을 때
wss.on('connection', (ws) => {
    console.log('클라이언트가 연결되었습니다.');

    // 클라이언트로부터 메시지를 받았을 때
    ws.on('message', (message) => {
        console.log(`수신된 메시지: ${message}`);

        // 모든 클라이언트에게 메시지를 브로드캐스트
        wss.clients.forEach((client) => {
            if (client.readyState === WebSocket.OPEN) {
                client.send(message);
            }
        });
    });

    // 연결이 닫혔을 때
    ws.on('close', () => {
        console.log('클라이언트가 연결을 끊었습니다.');
    });
});

console.log('WebSocket 서버가 실행 중입니다.');
