document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const rank = params.get('rank') || '솔로랭크';
    setActiveTab(rank === '자유랭크' ? 'freeTab' : 'soloTab');
    loadParties(rank);

    document.querySelectorAll('.position-selector span').forEach(span => {
        span.addEventListener('click', () => {
            selectPosition(span);
            filterByMainPosition(span.title);
        });
    });
});

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
    loadParties('솔로랭크');
};

document.getElementById('freeTab').onclick = () => {
    setActiveTab('freeTab');
    loadParties('자유랭크');
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
                el.dataset.positions
            );
        });
    });
}

function filterByMainPosition(koreanPos) {
    const code = positionMap[koreanPos] || 'ALL';
    if (code === 'ALL') {
        renderParties(allParties);
    } else {
        renderParties(allParties.filter(p => p.mainPosition === code));
    }
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

function showPartyDetail(seq, name, type, createDate, endDate, status, headcount, max, memo, mainPosition, positions) {
    const popup = document.getElementById('partyDetailPopup');
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
    <div class="popup-buttons">
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
      <form action="/party/delete/${seq}" method="post" style="display:inline;">
        <button type="submit" onclick="return confirm('정말 삭제할까요?')">삭제</button>
      </form>
      <button onclick="closePartyDetail()">닫기</button>
    </div>
  `;
    popup.style.display = 'block';
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
    const popup = document.getElementById('recruitPopup');
    const isEdit = party !== null;
    const action = isEdit ? `/party/edit/${party.partySeq}` : '/party/new';

    popup.innerHTML = `
    <h3>${isEdit ? '파티 수정하기' : '새 파티 등록하기'}</h3>
    <form action="${action}" method="post">
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
      <button type="submit">${isEdit ? '수정 완료' : '모집 시작'}</button>
      <button type="button" onclick="closePartyPopup()">닫기</button>
    </form>
  `;
    popup.style.display = 'block';
    addPositionCheckboxBehavior(popup);
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