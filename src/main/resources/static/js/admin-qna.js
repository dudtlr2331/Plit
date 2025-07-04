let ws;

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".chat-open-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            const roomId = btn.getAttribute("data-room-id");
            openChatPopup(roomId);
        });
    });

    // Enter 키로도 전송
    document.getElementById("messageInput").addEventListener("keyup", function (e) {
        if (e.key === "Enter") {
            sendMessage();
        }
    });
});

function openChatPopup(roomId) {
    document.getElementById("chatPopup").style.display = "block";
    const userId = "admin"; // TODO: 실제 로그인 사용자 ID로 바꾸기

    ws = new WebSocket(`wss://${location.host}/ws/chat?roomId=${roomId}&userId=${userId}`);

    ws.onmessage = (event) => {
        const msgBox = document.getElementById("chatMessages");
        const div = document.createElement("div");
        div.textContent = event.data;
        msgBox.appendChild(div);
        msgBox.scrollTop = msgBox.scrollHeight;
    };
}

function sendMessage() {
    const input = document.getElementById("messageInput");
    if (input.value.trim()) {
        ws.send(input.value);
        input.value = "";
    }
}

function closeChatPopup() {
    document.getElementById("chatPopup").style.display = "none";
    if (ws) {
        ws.close();
    }
}