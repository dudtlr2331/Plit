document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const rank = params.get('rank') || 'solo';
    setActiveTab(rank === 'team' ? 'freeTab' : 'soloTab');
    loadParties(rank);

    document.querySelectorAll('.position-selector span').forEach(span => {
        span.addEventListener('click', () => {
            selectPosition(span);
            filterByMainPosition(span.title);
        });
    });
});

let selectedPartyId = null;

function openJoinPopup(partyId) {
    selectedPartyId = partyId;
    document.getElementById('joinPopup').style.display = 'block';
}

function closeJoinPopup() {
    selectedPartyId = null;
    document.getElementById('joinPopup').style.display = 'none';
}

function submitJoinRequest() {
    const position = document.querySelector('input[name="joinPosition"]:checked')?.value;
    const message = document.getElementById('joinMessage').value;

    if (!position) {
        alert("포지션을 선택해주세요.");
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${selectedPartyId}/join`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({ position, message })
    })
        .then(res => res.text())
        .then(msg => {
            alert(msg === 'OK' ? '참가 신청 완료!' : `신청 실패: ${msg}`);
            closeJoinPopup();
            closePartyDetail();
            const activeTab = document.querySelector('.tab.active').id;
            const type = activeTab === 'freeTab' ? 'team' : 'solo';
            loadParties(type);
        })
        .catch(err => {
            console.error(err);
            alert('참가 신청 중 오류 발생');
        });
}

const positionMap = {
    "전체": "ALL",
    "탑": "TOP",
    "정글": "JUNGLE",
    "미드": "MID",
    "원딜": "ADC",
    "서포터": "SUPPORT"
};

let allParties = [];

function setActiveTab(id) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.getElementById(id).classList.add('active');
}

document.getElementById('soloTab').onclick = () => {
    setActiveTab('soloTab');
    loadParties('solo');
};

document.getElementById('freeTab').onclick = () => {
    setActiveTab('freeTab');
    loadParties('team');;
};

function loadParties(partyType) {
    fetch(`/api/parties?partyType=${encodeURIComponent(partyType)}`)
        .then(res => res.json())
        .then(data => {
            allParties = data;
            renderParties(data);
        });
}

function renderParties(data) {
    const list = document.getElementById('recruitList');
    const header = list.querySelector('.recruit-header');
    list.innerHTML = '';
    list.appendChild(header);

    if (data.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'recruit-item';
        empty.style.justifyContent = 'center';
        empty.innerHTML = '<span>현재 모집 중인 파티가 없습니다.</span>';
        list.appendChild(empty);
        return;
    }

    data.forEach(party => {
        const item = document.createElement('div');
        item.className = 'recruit-item';
        item.innerHTML = `
      <span><a href="javascript:void(0)" class="party-detail-link"
        data-seq="${party.partySeq}"
        data-name="${party.partyName}"
        data-type="${party.partyType}"
        data-created="${party.partyCreateDate}"
        data-end="${party.partyEndTime}"
        data-status="${party.partyStatus}"
        data-headcount="${party.partyHeadcount}"
        data-max="${party.partyMax}"
        data-memo="${party.memo}"
        data-main="${party.mainPosition}"
        data-createdby="${party.createdBy}"
        data-positions="${party.positions.join(', ')}"
      >${party.partyName}</a></span>
      <span>${party.partyType}</span>
      <span>${formatDateTime(party.partyCreateDate)}</span>
      <span>${formatDateTime(party.partyEndTime)}</span>
      <span>${party.partyStatus}</span>
      <span>${party.partyHeadcount}</span>
      <span>${party.partyMax}</span>
      <span>${party.partySeq}</span>
      <span class="chat-icon" onclick="toggleChatBox('partyId-${party.partySeq}')">💬</span>
    `;
        list.appendChild(item);
    });

    document.querySelectorAll('.party-detail-link').forEach(el => {
        el.addEventListener('click', () => {
            showPartyDetail(
                el.dataset.seq,
                el.dataset.name,
                el.dataset.type,
                el.dataset.created,
                el.dataset.end,
                el.dataset.status,
                el.dataset.headcount,
                el.dataset.max,
                el.dataset.memo,
                el.dataset.main,
                el.dataset.positions,
                el.dataset.createdby
            );
        });
    });
}

function filterByMainPosition(koreanPos) {
    const code = positionMap[koreanPos];
    if (!code) return;

    renderParties(
        allParties.filter(p => p.mainPosition && p.mainPosition.toUpperCase() === code)
    );
}

function toggleChatList() {
    const list = document.getElementById('chatList');
    list.style.display = list.style.display === 'block' ? 'none' : 'block';
}

function toggleChatBox(userId) {
    const box = document.getElementById('chatBox');
    box.style.display = box.style.display === 'block' ? 'none' : 'block';
}

function selectPosition(element) {
    document.querySelectorAll('.position-selector span').forEach(span => span.classList.remove('selected'));
    element.classList.add('selected');
}

function showPartyDetail(seq, name, type, createDate, endDate, status, headcount, max, memo, mainPosition, positions, createdBy) {
    const popup = document.getElementById('partyDetailPopup');
    const currentUserId = document.querySelector('meta[name="user-id"]')?.getAttribute('content');
    const isOwner = currentUserId && currentUserId === createdBy;

    // 참가 여부 확인 후 팝업 그리기
    checkJoined(seq).then(isJoined => {
        const joinBtnHtml = (() => {
            if (type !== 'team' || status !== 'WAITING') return '';
            if (isJoined) {
                return `<p style="color: green;"><strong>이미 참가한 파티입니다.</strong></p>`;
            }
            return `<button onclick="openJoinPopup(${seq})">참가하기</button>`;
        })();

        // 여기서 멤버 리스트를 추가로 불러와서 팝업에 삽입
        fetchPartyMembers(seq).then(members => {
            const memberListHtml = members.length > 0
                ? members.map(m => `<li>${m}</li>`).join('')
                : `<li>참가자가 없습니다.</li>`;

            // 수정/삭제 버튼 조건부 생성
            const ownerButtons = isOwner
                ? `
                <button onclick="handleEditFromDetail('${encodeURIComponent(JSON.stringify({
                    partySeq: seq,
                    partyName: name,
                    partyType: type,
                    partyCreateDate: createDate,
                    partyEndTime: endDate,
                    partyStatus: status,
                    partyHeadcount: headcount,
                    partyMax: max,
                    memo: memo,
                    mainPosition: mainPosition,
                    positions: positions.split(',').map(p => p.trim())
                }))}')">수정</button>
                <button onclick="deleteParty(${seq})">삭제</button>
                `
                : '';

            popup.innerHTML = `
            <h3>파티 상세 정보</h3>
            <p><strong>이름:</strong> ${name}</p>
            <p><strong>타입:</strong> ${type}</p>
            <p><strong>생성일자:</strong> ${formatDateTime(createDate)}</p>
            <p><strong>종료일자:</strong> ${formatDateTime(endDate)}</p>
            <p><strong>상태:</strong> ${status}</p>
            <p><strong>현재 인원:</strong> ${headcount}</p>
            <p><strong>최대 인원:</strong> ${max}</p>
            <p><strong>메모:</strong> ${memo}</p>
            <p><strong>주 포지션:</strong> ${mainPosition}</p>
            <p><strong>모집 포지션:</strong> ${positions}</p>
            <p><strong>참가 멤버:</strong></p>
            <ul>${memberListHtml}</ul>
            <div class="popup-buttons">
              ${ownerButtons}
              <button onclick="closePartyDetail()">닫기</button>
              ${joinBtnHtml}
            </div>
            `;
            popup.style.display = 'block';
        });
    });
}

const fetchPartyMembers = async (partyId) => {
    try {
        const res = await fetch(`/api/parties/${partyId}/members`);
        if (!res.ok) throw new Error('멤버 조회 실패');
        return await res.json(); // [ "user1", "user2", ... ]
    } catch (e) {
        console.error(e);
        return [];
    }
};

function joinParty(partySeq) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${partySeq}/join`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    })
        .then(res => res.text())  // 항상 text 응답 받기
        .then(msg => {
            if (msg === 'OK') {
                alert('파티에 참가하였습니다!');
                closePartyDetail();
                const activeTab = document.querySelector('.tab.active').id;
                const type = activeTab === 'freeTab' ? 'team' : 'solo';
                loadParties(type);
            } else {
                alert(`참가 실패: ${msg}`);
            }
        })
        .catch(err => {
            console.error(err);
            alert('참가 중 오류 발생');
        });
}

const checkJoined = async (partyId) => {
    try {
        const response = await fetch(`/api/party/${partyId}/joined`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            console.warn('참가 여부 확인 실패');
            return false; // 기본값
        }

        return await response.json(); // true or false
    } catch (err) {
        console.error(err);
        return false; // 오류 시 기본값
    }
};

function deleteParty(partyId) {
    if (!confirm("정말 삭제할까요?")) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${partyId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    })
        .then(res => {
            if (res.ok) {
                alert("삭제되었습니다.");
                closePartyDetail(); // 상세 팝업 닫기
                const activeTab = document.querySelector('.tab.active').id;
                const type = activeTab === 'freeTab' ? 'team' : 'solo';
                loadParties(type); // 목록 새로고침
            } else {
                alert("삭제 실패");
            }
        })
        .catch(() => {
            alert("서버 오류로 삭제에 실패했습니다.");
        });
}

function closePartyDetail() {
    const popup = document.getElementById('partyDetailPopup');
    popup.style.display = 'none';
    popup.innerHTML = '';
}

function toggleRecruitPopup() {
    openPartyFormPopup();
}

function handleEditFromDetail(partyJson) {
    closePartyDetail();
    const party = JSON.parse(decodeURIComponent(partyJson));
    openPartyFormPopup(party);
}

function openPartyFormPopup(party = null) {
    const csrfParam = document.querySelector('meta[name="_csrf_parameter"]').getAttribute('content');
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');

    const popup = document.getElementById('recruitPopup');
    const isEdit = party !== null;

    popup.innerHTML = `
    <h3>${isEdit ? '파티 수정하기' : '새 파티 등록하기'}</h3>
    <div class="party-form">
        <input type="hidden" name="${csrfParam}" value="${csrfToken}" />
        ${isEdit ? `<input type="hidden" name="partySeq" value="${party.partySeq}">` : ''}
        
        <label>파티 이름: <input type="text" name="partyName" value="${party?.partyName ?? ''}" required></label><br>
        
        <label>타입:
            <select name="partyType" required>
                <option value="solo" ${party?.partyType === 'solo' ? 'selected' : ''}>솔로랭크</option>
                <option value="team" ${party?.partyType === 'team' ? 'selected' : ''}>자유랭크</option>
            </select>
        </label><br>
        
        ${isEdit ? `<label>생성일자: <input type="datetime-local" name="partyCreateDate" value="${party.partyCreateDate}" readonly></label><br>` : ''}
        
        <label>종료일자: <input type="datetime-local" name="partyEndTime" value="${party?.partyEndTime ?? ''}" required></label><br>
        <label>상태: <input type="text" name="partyStatus" value="${party?.partyStatus ?? 'WAITING'}" required></label><br>
        <label>현재 인원: <input type="number" name="partyHeadcount" value="${party?.partyHeadcount ?? 1}" min="1" required></label><br>
        <label>최대 인원: <input type="number" name="partyMax" value="${party?.partyMax ?? 5}" min="1" required></label><br>
        <label>메모:<br><textarea name="memo" rows="3" cols="40">${party?.memo ?? ''}</textarea></label><br>
        
        <label>주 포지션:
            <select name="mainPosition" required>
                ${['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT', 'ALL'].map(pos => `
                    <option value="${pos}" ${party?.mainPosition === pos ? 'selected' : ''}>${pos}</option>
                `).join('')}
            </select>
        </label><br>
        
        <label>모집 포지션:<br/>
            <div class="position-group">
                ${['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT', 'ALL'].map(pos => `
                    <label>
                        <input type="checkbox" name="positions" value="${pos}" ${party?.positions?.includes(pos) ? 'checked' : ''}>
                        <i class="${getPositionIconClass(pos)}" title="${pos}"></i>
                    </label>
                `).join('')}
            </div>
        </label><br>
        
        <button type="button" onclick="submitPartyForm()">${isEdit ? '수정 완료' : '모집 시작'}</button>
        <button type="button" onclick="closePartyPopup()">닫기</button>
    </div>
    `;

    popup.style.display = 'block';
    addPositionCheckboxBehavior(popup);
}

function submitPartyForm() {
    const popup = document.getElementById('recruitPopup');
    const isEdit = popup.querySelector('h3').textContent.includes('수정');

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    const partySeq = popup.querySelector('input[name="partySeq"]')?.value;
    const partyName = popup.querySelector('input[name="partyName"]').value;
    const partyType = popup.querySelector('select[name="partyType"]').value;
    const partyEndTime = popup.querySelector('input[name="partyEndTime"]').value;
    const partyStatus = popup.querySelector('input[name="partyStatus"]').value;
    const partyHeadcount = parseInt(popup.querySelector('input[name="partyHeadcount"]').value);
    const partyMax = parseInt(popup.querySelector('input[name="partyMax"]').value);
    const memo = popup.querySelector('textarea[name="memo"]').value;
    const mainPosition = popup.querySelector('select[name="mainPosition"]').value;
    const positions = Array.from(popup.querySelectorAll('input[name="positions"]:checked'))
        .map(cb => cb.value);

    const data = {
        partyName,
        partyType,
        partyEndTime,
        partyStatus,
        partyHeadcount,
        partyMax,
        memo,
        mainPosition,
        positions
    };

    const url = isEdit ? `/api/parties/${partySeq}` : '/api/parties';
    const method = isEdit ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(data)
    })
        .then(res => {
            if (res.ok) {
                alert(isEdit ? '수정 완료!' : '등록 완료!');
                closePartyPopup();
                const activeTab = document.querySelector('.tab.active').id;
                const type = activeTab === 'freeTab' ? 'team' : 'solo';
                loadParties(type);
            } else {
                alert('실패했습니다.');
            }
        })
        .catch(err => {
            console.error(err);
            alert('서버 오류');
        });
}

function getPositionIconClass(pos) {
    return {
        TOP: 'fas fa-shield-alt',
        JUNGLE: 'fas fa-tree',
        MID: 'fas fa-magic',
        ADC: 'fas fa-crosshairs',
        SUPPORT: 'fas fa-eye',
        ALL: 'fas fa-asterisk'
    }[pos] || 'fas fa-user';
}

function addPositionCheckboxBehavior(popup) {
    const checkboxes = popup.querySelectorAll("input[name='positions']");
    checkboxes.forEach(cb => {
        cb.addEventListener("change", () => {
            const selected = Array.from(checkboxes).filter(c => c.checked && c.value !== 'ALL');
            const all = popup.querySelector("input[name='positions'][value='ALL']");
            if (selected.length === 5) {
                checkboxes.forEach(c => c.checked = false);
                if (all) all.checked = true;
            } else if (all && all.checked && selected.length > 0) {
                all.checked = false;
            }
        });
    });
}

function closePartyPopup() {
    const popup = document.getElementById('recruitPopup');
    popup.style.display = 'none';
    popup.innerHTML = '';
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '';
    return dateTimeStr.replace('T', ' ').slice(0, 16);
}

