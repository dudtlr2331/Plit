// chatPopup.js (전역 등록 없이 실행, 단 initChatFeature만 공개)

(function () {
    let stompClient = null;
    let currentRoomId = null;
    let chatUserId = null;

    function getCsrfHeaders() {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        return csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {};
    }

    function createChatListPopupElement() {
        if (document.getElementById('chatListPopup')) return;

        const popup = document.createElement('div');
        popup.id = 'chatListPopup';
        popup.className = 'chat-list-popup';
        popup.style.display = 'none';

        popup.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
            <h4>채팅 목록</h4>
            <button class="close-btn" onclick="document.getElementById('chatListPopup').style.display='none'">×</button>
            </div>
            <div id="chatListContainer"></div>
        `;

        document.body.appendChild(popup);
    }

    function createChatPopupElement() {
        if (document.getElementById('chatPopup')) return;

        const popup = document.createElement('div');
        popup.id = 'chatPopup';
        popup.className = 'chat-popup';
        popup.style.display = 'none';
        popup.innerHTML = `
            <div class="chat-header" style="display: flex; justify-content: space-between; align-items: center; padding: 4px 8px;">
                <div id="chatTarget" style="flex: 1;">상대 닉네임</div>
                <button class="close-btn" onclick="document.getElementById('chatPopup').style.display='none'">×</button>
            </div>
            <div id="chatBody" class="chat-body"></div>
            <input id="chatInput" type="text" placeholder="메시지를 입력하세요">
        `;
        document.body.appendChild(popup);
    }

    function bindChatIconClick() {
        const icon = document.querySelector('.chat-icon');
        if (!icon) return;

        icon.addEventListener('click', () => {
            const popup = document.getElementById('chatListPopup');
            if (!popup) return;

            if (!chatUserId) {
                alert("로그인이 필요합니다.");
                return;
            }

            popup.style.display = (popup.style.display === 'none' || popup.style.display === '') ? 'block' : 'none';
            if (popup.style.display === 'block') {
                loadChatList();
            }
        });
    }

    function loadChatList() {
        fetch(`/api/chat/rooms/${chatUserId}`, {
            method: 'GET',
            headers: getCsrfHeaders()
        })
            .then(res => res.json())
            .then(data => {
                const container = document.getElementById('chatListContainer');
                container.innerHTML = '';

                if (data.length === 0) {
                    container.innerHTML = '<div class="chat-list-empty">참여한 채팅방이 없습니다.</div>';
                    return;
                }

                data.forEach(room => {
                    const div = document.createElement('div');
                    div.className = 'chat-list-item';

                    if (room.type === 'friend') {
                        div.textContent = room.otherNickname + (room.unreadCount > 0 ? ` (${room.unreadCount})` : '');
                        div.onclick = () => openChatWithFriend(room.roomId, room.otherUserId, room.otherNickname);
                    } else if (room.type === 'party') {
                        div.textContent = `[파티] ${room.partyName || '이름없음'}` + (room.unreadCount > 0 ? ` (${room.unreadCount})` : '');
                        div.onclick = () => openPartyChat(room.partyId, room.partyName);
                    }

                    container.appendChild(div);
                });

            });
    }

    function openChatWithFriend(roomId, otherUserId, nickname) {
        currentRoomId = roomId;

        document.getElementById('chatTarget').textContent = nickname;
        document.getElementById('chatPopup').style.display = 'flex';
        document.getElementById('chatInput').focus();

        loadPreviousMessages(roomId);
        connectToChatRoom(roomId);
    }

    function loadPreviousMessages(roomId) {
        fetch(`/api/chat/${roomId}`, {
            method: 'GET',
            headers: getCsrfHeaders()
        })
            .then(res => res.json())
            .then(messages => {
                const chatBody = document.getElementById('chatBody');
                chatBody.innerHTML = '';
                messages.forEach(msg =>
                    appendMessage(msg.content, msg.sender == chatUserId, msg.senderNickname, msg.sentAt)
                );
                chatBody.scrollTop = chatBody.scrollHeight;
            });
    }

    function appendMessage(content, isMine, nickname, sentAt) {
        const wrapper = document.createElement('div');
        wrapper.className = 'chat-message-wrapper ' + (isMine ? 'sent' : 'received');

        wrapper.innerHTML = `
            <div class="chat-nickname">${nickname}</div>
            <div class="chat-bubble">${content}</div>
            <div class="chat-time">${formatDateTime(sentAt)}</div>
        `;

        const chatBody = document.getElementById('chatBody');
        chatBody.appendChild(wrapper);
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    function formatDateTime(sentAt) {
        const date = new Date(sentAt);
        return date.toLocaleString();
    }

    function connectToChatRoom(roomId) {
        if (stompClient) stompClient.disconnect();

        const socket = new SockJS('/ws-stomp');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, () => {
            stompClient.subscribe(`/sub/chat/room/${roomId}`, (message) => {
                const msg = JSON.parse(message.body);
                appendMessage(msg.content, msg.sender == chatUserId, msg.senderNickname, msg.sentAt);
            });
        });
    }

    function bindChatInputHandler() {
        document.addEventListener('keypress', function (e) {
            if (e.target.id === 'chatInput' && e.key === 'Enter' && stompClient && currentRoomId) {
                const content = e.target.value.trim();
                if (!content) return;

                stompClient.send("/pub/chat/send", {}, JSON.stringify({
                    roomId: currentRoomId,
                    sender: chatUserId,
                    content: content
                }));

                e.target.value = '';
            }
        });
    }

    window.openPartyChat = function(partyId, partyName) {
        if (!chatUserId) {
            alert("로그인이 필요합니다.");
            return;
        }

        // partyId로 chatRoomId만 가져오기
        fetch(`/api/chat/room/party/${partyId}`, {
            method: 'GET',
            headers: getCsrfHeaders()
        })
            .then(res => {
                if (!res.ok) throw new Error('채팅방 조회 실패');
                return res.json();
            })
            .then(roomId => {
                currentRoomId = roomId;
                document.getElementById('chatTarget').textContent = partyName ? `[파티] ${partyName}` : `파티 #${partyId}`;
                document.getElementById('chatPopup').style.display = 'flex';
                document.getElementById('chatInput').focus();

                loadPreviousMessages(roomId);
                connectToChatRoom(roomId);
            })
            .catch(err => {
                console.error(err);
                alert('채팅방을 불러오지 못했습니다.');
            });
    };


    // 전역 등록 단 하나만
    window.initChatFeature = function (userSeq) {
        chatUserId = userSeq;
        createChatListPopupElement();
        createChatPopupElement();
        bindChatInputHandler();
        bindChatIconClick();
    };
})();
