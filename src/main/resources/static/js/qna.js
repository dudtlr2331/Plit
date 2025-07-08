function toggleChat() {
    const popup = document.getElementById("chatPopup");
    popup.style.display = popup.style.display === "none" ? "flex" : "none";
}

function requestChat() {
    toggleChat();
}

const roomId = "admin-room";
const userId = "plit유저"; // TODO: 실제 로그인 사용자 ID로 대체하기

const ws = new WebSocket(`wss://${location.host}/ws/chat?roomId=${roomId}&userId=${userId}`);

ws.onmessage = (event) => {
    const msgBox = document.getElementById("chatMessages");
    const div = document.createElement("div");
    div.textContent = event.data;
    msgBox.appendChild(div);
    msgBox.scrollTop = msgBox.scrollHeight;
};

function sendMessage() {
    const input = document.getElementById("messageInput");
    if (input.value.trim()) {
        ws.send(input.value);
        input.value = "";
    }
}

document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("qnaForm");
    const title = document.getElementById("title");
    const category = document.getElementById("category");
    const content = document.getElementById("content");
    const privacy = document.getElementById("privacy");

    const titleError = document.getElementById("title-error");
    const categoryError = document.getElementById("category-error");
    const contentError = document.getElementById("content-error");
    const privacyError = document.getElementById("privacy-error");

    form.addEventListener("submit", function (e) {
        // 먼저 모든 에러 숨기기
        titleError.classList.remove("show");
        categoryError.classList.remove("show");
        contentError.classList.remove("show");
        privacyError.classList.remove("show");

        // 제목 먼저
        if (title.value.trim() === "") {
            e.preventDefault();
            titleError.classList.add("show");
            title.focus();
            return;
        }

        // 카테고리
        if (category.value === "") {
            e.preventDefault();
            categoryError.classList.add("show");
            category.focus();
            return;
        }

        // 내용
        if (content.value.trim() === "") {
            e.preventDefault();
            contentError.classList.add("show");
            content.focus();
            return;
        }

        // 마지막으로 체크박스
        if (!privacy.checked) {
            e.preventDefault();
            privacyError.classList.add("show");
            privacy.focus();
            return;
        }
    });
});