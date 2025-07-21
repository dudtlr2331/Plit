let stompClient = null;
let currentRoomId = null;

const currentUserSeq = document.querySelector('meta[name="login-user-seq"]').getAttribute('content');
const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

// 친구 요청 목록 불러오기
function loadFriendRequests() {
    fetch('/api/friends/requests')
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById('requestList');
            container.innerHTML = '';

            if (data.length === 0) {
                container.innerHTML = '<p>요청이 없습니다.</p>';
                return;
            }

            data.forEach(req => {
                const div = document.createElement('div');
                div.className = 'request-item';
                div.style.display = 'flex';
                div.style.justifyContent = 'space-between';
                div.style.alignItems = 'center';
                div.style.marginBottom = '10px';

                const nickname = document.createElement('span');
                nickname.textContent = req.user.userNickname;
                nickname.style.flex = '1';

                const memo = document.createElement('span');
                memo.textContent = req.memo || '';
                memo.style.flex = '2';
                memo.style.margin = '0 10px';

                const actions = document.createElement('div');
                actions.style.display = 'flex';
                actions.style.gap = '5px';

                const acceptBtn = document.createElement('button');
                acceptBtn.textContent = '수락';
                acceptBtn.className = 'submit-button';
                acceptBtn.onclick = () => handleFriendAction(req.friendsNo, 'accept');

                const declineBtn = document.createElement('button');
                declineBtn.textContent = '거절';
                declineBtn.className = 'delete-btn';
                declineBtn.onclick = () => handleFriendAction(req.friendsNo, 'decline');

                actions.appendChild(acceptBtn);
                actions.appendChild(declineBtn);

                div.appendChild(nickname);
                div.appendChild(memo);
                div.appendChild(actions);

                container.appendChild(div);
            });
        });
}


// 이전 메시지 불러오기
function loadPreviousMessages(roomId) {
    fetch(`/api/chat/${roomId}`)
        .then(res => res.json())
        .then(messages => {
            const body = document.getElementById('chatBody');
            body.innerHTML = '';
            messages.forEach(msg => {
                appendMessage(msg.content, msg.sender == currentUserSeq, msg.senderNickname, msg.sentAt);
            });
        });
}

function openChatWithFriend(btn) {
    const myId = btn.getAttribute("data-myid");
    const otherId = btn.getAttribute("data-otherid");
    const nickname = btn.getAttribute("data-nickname");
    if (!myId || !otherId) {
        alert("친구 정보가 잘못되었습니다.");
        return;
    }
    fetch(`/api/chat/room/${myId}/${otherId}`)
        .then(res => res.json())
        .then(roomId => {
            document.getElementById('chatTarget').textContent = nickname;
            document.getElementById('chatPopup').style.display = 'flex';
            document.getElementById('chatInput').focus();
            loadPreviousMessages(roomId);
            connectToChatRoom(roomId);
            markMessagesAsRead(roomId);
        });
}

function closeChat() {
    document.getElementById('chatPopup').style.display = 'none';
    document.getElementById('chatBody').innerHTML = '';
    document.getElementById('chatInput').value = '';
    if (stompClient) stompClient.disconnect();
}

// 메모 팝업 열기/닫기
function openMemoPopup(btn) {
    const friendNo = btn.getAttribute("data-friendsno");
    const currentMemo = btn.getAttribute("data-currentmemo");
    document.getElementById("memoFriendId").value = friendNo;
    document.getElementById("memoInputArea").value = currentMemo;
    document.getElementById("memoPopup").style.display = "flex";
}
function closeMemoPopup() {
    document.getElementById("memoPopup").style.display = "none";
}
function submitMemo() {
    const friendNo = document.getElementById("memoFriendId").value;
    const newMemo = document.getElementById("memoInputArea").value;
    fetch(`/api/friends/${friendNo}/memo`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({ memo: newMemo })
    }).then(res => {
        if (res.ok) {
            alert("메모가 저장되었습니다.");
            location.reload();
        } else {
            alert("메모 저장에 실패했습니다.");
        }
    });
}

// 친구 신청 수락/거절
function handleFriendAction(friendNo, action) {
    fetch(`/api/friends/${friendNo}/${action}`, {
        headers: {
            [csrfHeader]: csrfToken
        },
        method: "POST"
    }).then(res => {
        if (res.ok) {
            alert(`${action === 'accept' ? '수락' : '거절'}되었습니다.`);
            loadFriendRequests();
        } else {
            alert('처리에 실패했습니다.');
        }
    });
}

function toggleRequestPopup() {
    const popup = document.getElementById('requestPopup');
    popup.style.display = popup.style.display === 'flex' ? 'none' : 'flex';
    if (popup.style.display === 'flex') loadFriendRequests();
}

// 친구 차단/삭제
function blockFriend(friendNo) {
    if (!confirm('정말 이 친구를 차단하시겠습니까?')) return;
    fetch(`/api/friends/${friendNo}/block`, {
        headers: { [csrfHeader]: csrfToken },
        method: "POST"
    }).then(res => {
        if (res.ok) {
            alert("친구를 차단했습니다.");
            location.reload();
        } else {
            alert("차단에 실패했습니다.");
        }
    });
}

function deleteFriend(friendNo) {
    if (!confirm('정말 이 친구를 삭제하시겠습니까?')) return;
    fetch(`/api/friends/${friendNo}`, {
        headers: { [csrfHeader]: csrfToken },
        method: "DELETE"
    }).then(res => {
        if (res.ok) {
            alert("친구가 삭제되었습니다.");
            location.reload();
        } else {
            alert("삭제에 실패했습니다.");
        }
    });
}

// WebSocket
function connectToChatRoom(roomId) {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {
            doConnect(roomId);
        });
    } else {
        doConnect(roomId);
    }
}

function doConnect(roomId) {
    const socket = new SockJS('/ws/chat');
    stompClient = Stomp.over(socket);
    currentRoomId = roomId;
    stompClient.connect({}, () => {
        stompClient.subscribe(`/sub/chat/room/${roomId}`, message => {
            const msg = JSON.parse(message.body);
            if (msg.sender == currentUserSeq) return;
            appendMessage(msg.content, false, msg.senderNickname, msg.sentAt);
        });
    });
    replaceChatInput(roomId);
}

function replaceChatInput(roomId) {
    const input = document.getElementById('chatInput');
    const newInput = input.cloneNode(true);
    input.parentNode.replaceChild(newInput, input);
    newInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && this.value.trim()) {
            const msg = this.value.trim();
            stompClient.send("/pub/chat/send", {}, JSON.stringify({
                sender: currentUserSeq,
                content: msg,
                roomId: roomId,
                senderNickname: document.querySelector('meta[name="login-user-nickname"]').getAttribute('content'),
                sentAt: new Date().toISOString()
            }));
            appendMessage(msg, true, "나", new Date().toISOString());
            this.value = '';
        }
    });
}

function appendMessage(content, isMine, nickname = null, sentAt = null) {
    const wrapper = document.createElement('div');
    wrapper.className = 'chat-message ' + (isMine ? 'sent' : 'received');
    if (!isMine && nickname) {
        const nickElem = document.createElement('div');
        nickElem.className = 'message-nickname';
        nickElem.textContent = nickname;
        wrapper.appendChild(nickElem);
    }
    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    bubble.textContent = content;
    wrapper.appendChild(bubble);
    const timeElem = document.createElement('div');
    timeElem.className = 'message-time';
    timeElem.textContent = sentAt ? new Date(sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
    wrapper.appendChild(timeElem);
    const body = document.getElementById('chatBody');
    body.appendChild(wrapper);
    body.scrollTop = body.scrollHeight;
}

function fetchUnreadCounts(userId) {
    fetch(`/api/chat/rooms/unread/${userId}`)
        .then(res => res.json())
        .then(data => {
            data.forEach(room => {
                const btn = document.querySelector(`.chat-btn[data-roomid="${room.roomId}"]`);
                if (btn && room.unreadCount > 0) {
                    btn.textContent = `💬 (${room.unreadCount})`;
                }
            });
        });
}

function markMessagesAsRead(roomId) {
    fetch(`/api/chat/${roomId}/read?userId=${currentUserSeq}`, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken }
    }).then(res => {
        if (res.ok) {
            fetchUnreadCounts(currentUserSeq);
        }
    });
}

// 초기 실행
document.addEventListener("DOMContentLoaded", () => {
    fetchUnreadCounts(parseInt(currentUserSeq));
});
