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
    const getIcon = window.getPositionIconHTML;

    Promise.all([
        fetch(`/api/parties/${partyId}`).then(res => res.json()),
        fetch(`/api/parties/${partyId}/members`).then(res => res.json())
    ]).then(([party, members]) => {
        console.log("모집 포지션:", party.positions);
        console.log("현재 멤버 목록:", members);

        const availablePositions = party.positions;
        const takenPositions = members
            .filter(m => m.status === 'ACCEPTED')
            .map(m => m.position);

        console.log("이미 배정된 포지션:", takenPositions);

        // 모집 포지션이 ALL인 경우 전체 포지션으로 확장
        const isAllPosition = availablePositions.length === 1 && availablePositions[0] === 'ALL';
        const positionPool = isAllPosition
            ? ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT']
            : availablePositions;

        const selectablePositions = positionPool.filter(pos => !takenPositions.includes(pos));
        console.log("신청 가능한 포지션:", selectablePositions);

        const container = document.querySelector('.position-group');
        container.innerHTML = '';

        const positionLabels = {
            TOP: '탑',
            JUNGLE: '정글',
            MID: '미드',
            ADC: '원딜',
            SUPPORT: '서포터',
            ALL: '상관없음'
        };

        if (selectablePositions.length === 0) {
            container.innerHTML = `<p style="color:gray;">선택 가능한 포지션이 없습니다.</p>`;
        } else {
            if (isAllPosition) {
                const allLabel = document.createElement('label');
                allLabel.innerHTML = `
                    <input type="radio" name="joinPosition" value="ALL">
                    ${getIcon('ALL')} ${positionLabels['ALL']}
                `;
                container.appendChild(allLabel);
            }

            selectablePositions.forEach(pos => {
                const label = document.createElement('label');
                label.innerHTML = `
                    <input type="radio" name="joinPosition" value="${pos}">
                    ${getIcon(pos)} ${positionLabels[pos] || pos}
                `;
                container.appendChild(label);
            });
        }
    });
}

function createElementFromHTML(html) {
    const div = document.createElement('div');
    div.innerHTML = html.trim();
    return div.firstChild;
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
        empty.className = 'recruit-item empty-row';
        empty.innerHTML = `<span class="empty-message">현재 모집 중인 파티가 없습니다.</span>`;
        list.appendChild(empty);
        return;
    }

    data.forEach(party => {
        const item = document.createElement('div');
        item.className = 'recruit-item';

        const mainIcon = getPositionIconHTML(party.mainPosition, true);
        const recruitIcons = Array.isArray(party.positions)
            ? party.positions.map(p => getPositionIconHTML(p, true)).join(' ')
            : party.positions.split(',').map(p => getPositionIconHTML(p.trim(), true)).join(' ');

        item.innerHTML = `
            <span>${party.partySeq}</span>
            <span>
                <a href="javascript:void(0)" class="party-detail-link"
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
                    data-positions="${party.positions.join ? party.positions.join(',') : party.positions}"
                    data-createdby="${party.createdBy}"
                >${party.partyName}</a>
            </span>
            <span>${translateStatus(party.partyStatus)}</span>
            <span title="주 포지션">${mainIcon}</span>
            <span title="모집 포지션">${recruitIcons}</span>
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

/* 포지션 선택 시 인원 체크 */
function addPositionCheckboxBehavior(popup) {
    const checkboxes = popup.querySelectorAll("input[name='positions']");
    const maxInput = popup.querySelector("input[name='partyMax']");
    const headcountInput = popup.querySelector("input[name='partyHeadcount']");

    const updateHeadcounts = () => {
        const selected = Array.from(checkboxes).filter(cb => cb.checked).map(cb => cb.value);

        if (selected.includes('ALL')) {
            maxInput.value = 5;
        } else {
            maxInput.value = Math.min(selected.length + 1, 5); // +1 for party leader
        }

        headcountInput.value = 1; // 파티장은 무조건 1명
    };

    checkboxes.forEach(cb => {
        cb.addEventListener("change", () => {
            const selected = Array.from(checkboxes).filter(c => c.checked && c.value !== 'ALL');
            const all = popup.querySelector("input[name='positions'][value='ALL']");

            // ALL 자동 체크 로직
            if (selected.length === 5) {
                checkboxes.forEach(c => c.checked = false);
                if (all) all.checked = true;
            } else if (all && all.checked && selected.length > 0) {
                all.checked = false;
            }

            updateHeadcounts();
        });
    });

    // 폼 열릴 때 초기값 설정
    updateHeadcounts();
}

function showPartyDetail(seq, name, type, createDate, endDate, status, headcount, max, memo, mainPosition, positions, createdBy) {
    const popup = document.getElementById('partyDetailPopup');
    const currentUserId = document.querySelector('meta[name="user-id"]')?.getAttribute('content');
    const isOwner = currentUserId && currentUserId === createdBy;

    fetch(`/api/parties/${seq}/join-status`)
        .then(res => res.text())
        .then(joinStatus => {
            let joinBtnHtml = '';

            if (type === 'team' && status === 'WAITING') {
                if (currentUserId === createdBy) {
                    joinBtnHtml = `<p style="color: gray;"><strong>파티장은 참가 신청할 수 없습니다.</strong></p>`;
                } else if (headcount >= max) {
                    joinBtnHtml = `<p style="color: red;"><strong>파티 인원이 모두 찼습니다.</strong></p>`;
                } else if (joinStatus === 'NONE') {
                    joinBtnHtml = `<button onclick="openJoinPopup(${seq})">참가하기</button>`;
                } else if (joinStatus === 'PENDING') {
                    joinBtnHtml = `<p style="color: orange;"><strong>참가 신청 중입니다.</strong></p>`;
                } else if (joinStatus === 'ACCEPTED') {
                    joinBtnHtml = `<p style="color: green;"><strong>이미 참가한 파티입니다.</strong></p>`;
                } else if (joinStatus === 'REJECTED') {
                    joinBtnHtml = `<p style="color: red;"><strong>신청이 거절된 파티입니다.</strong></p>`;
                }
            }

            fetchPartyMembers(seq).then(members => {
                const approved = members.filter(m => m.status === 'ACCEPTED');
                const pending = members.filter(m => m.status === 'PENDING');

                const detailHtml = `
                    <p><strong>이름:</strong> ${name}</p>
                    <p><strong>타입:</strong> ${type}</p>
                    <p><strong>생성일자:</strong> ${formatDateTime(createDate)}</p>
                    <p><strong>종료일자:</strong> ${formatDateTime(endDate)}</p>
                    <p><strong>상태:</strong> ${translateStatus(status)}</p>
                    <p><strong>현재 인원:</strong> ${headcount}</p>
                    <p><strong>최대 인원:</strong> ${max}</p>
                    <p><strong>메모:</strong> ${memo}</p>
                    <p><strong>주 포지션:</strong> ${mainPosition}</p>
                    <p><strong>모집 포지션:</strong> ${positions}</p>
                `;

                const approvedHtml = approved.length > 0
                    ? `<ul>${approved.map(m => {
                        const kickBtn = (isOwner && m.userId !== createdBy)
                            ? `<button onclick="kickMember(${seq}, ${m.id})">내보내기</button>`
                            : '';
                        return `<li>${m.userId} - ${m.message || ''} ${kickBtn}</li>`;
                    }).join('')}</ul>`
                    : '<p>참가 멤버 없음</p>';

                const pendingHtml = pending.length > 0
                    ? `<ul>${pending.map(m => {
                        const actions = isOwner ? `
                            <button onclick="approveMember(${seq}, ${m.id})">수락</button>
                            <button onclick="rejectMember(${seq}, ${m.id})">거절</button>` : '';
                        return `<li>${m.userId} - ${m.message || ''} ${actions}</li>`;
                    }).join('')}</ul>`
                    : '<p>대기 중인 멤버 없음</p>';

                const ownerButtons = isOwner ? `
                    <button onclick="handleEditFromDetail('${encodeURIComponent(JSON.stringify({
                    partySeq: seq, partyName: name, partyType: type, partyCreateDate: createDate,
                    partyEndTime: endDate, partyStatus: status, partyHeadcount: headcount,
                    partyMax: max, memo: memo, mainPosition: mainPosition,
                    positions: positions.split(',').map(p => p.trim())
                }))}')">수정</button>
                    <button onclick="deleteParty(${seq})">삭제</button>
                ` : '';

                popup.innerHTML = `
                    <h3>파티 상세 정보</h3>

                    <div class="tabs">
                        <button class="tab-btn active" onclick="switchDetailTab('detail')">상세</button>
                        <button class="tab-btn" onclick="switchDetailTab('approved')">참가 멤버</button>
                        <button class="tab-btn" onclick="switchDetailTab('pending')">수락 대기</button>
                    </div>

                    <div id="tab-detail" class="tab-content">${detailHtml}</div>
                    <div id="tab-approved" class="tab-content" style="display:none;">${approvedHtml}</div>
                    <div id="tab-pending" class="tab-content" style="display:none;">${pendingHtml}</div>

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

/* 파티원 내보내기 */
function kickMember(partyId, memberId) {
    if (!confirm("정말 이 멤버를 내보내시겠습니까?")) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${partyId}/members/${memberId}/kick`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    })
        .then(res => {
            if (res.ok) {
                alert('멤버를 내보냈습니다.');
                closePartyDetail();
                loadParties('team');
            } else {
                return res.text().then(text => {
                    alert(`실패: ${text || '알 수 없는 오류'}`);
                });
            }
        })
        .catch(err => {
            console.error(err);
            alert('서버 오류로 강퇴에 실패했습니다.');
        });
}

/* 파티 탭 전환 */
function switchDetailTab(tabName) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(div => div.style.display = 'none');

    const tabId = `tab-${tabName}`;
    document.getElementById(tabId).style.display = 'block';

    const tabIndex = { detail: 0, approved: 1, pending: 2 }[tabName];
    document.querySelectorAll('.tab-btn')[tabIndex]?.classList.add('active');
}

/* 파티 참여 수락 버튼 */
function approveMember(partyId, memberId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${partyId}/members/${memberId}/accept`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    })
        .then(res => {
            if (res.ok) {
                alert('참가 수락 완료');
                closePartyDetail();
                loadParties('team');
            } else {
                // 서버에서 메시지를 전달했다면 그것도 함께 알림
                return res.text().then(text => {
                    alert(`수락 실패: ${text || '알 수 없는 오류'}`);
                });
            }
        })
        .catch(err => {
            console.error(err);
            alert('서버 오류로 수락 처리에 실패했습니다.');
        });
}

/* 파티 참여 거절 버튼 */
function rejectMember(partyId, memberId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${partyId}/members/${memberId}/reject`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    })
        .then(res => {
            if (res.ok) {
                alert('참가 거절 완료');
                closePartyDetail();
                loadParties('team');
            } else {
                alert('거절 실패');
            }
        });
}

const fetchPartyMembers = async (partyId) => {
    try {
        const res = await fetch(`/api/parties/${partyId}/members`);
        if (!res.ok) throw new Error('멤버 조회 실패');
        return await res.json(); // [{ id, userId, message, status, ... }]
    } catch (e) {
        console.error(e);
        return [];
    }
};

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

            <label>종료일자: <input type="datetime-local" id="partyEndTime" name="partyEndTime" value="${party?.partyEndTime ?? ''}" required></label><br>

            ${isEdit
        ? `<label>상태:
                      <select name="partyStatus" required>
                          ${[
            { value: 'WAITING', label: '모집 중' },
            { value: 'FULL', label: '인원 꽉참' },
            { value: 'CLOSED', label: '모집 마감' }
        ].map(opt => `
                            <option value="${opt.value}" ${party?.partyStatus === opt.value ? 'selected' : ''}>${opt.label}</option>
                          `).join('')}
                      </select>
                  </label><br>`
        : `<input type="hidden" name="partyStatus" value="WAITING">`
    }

            <label>메모:<br><textarea name="memo" rows="3" cols="40">${party?.memo ?? ''}</textarea></label><br>

            <label>주 포지션:
                <select name="mainPosition" required>
                    ${['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT', 'ALL'].map(pos => `
                        <option value="${pos}" ${party?.mainPosition === pos ? 'selected' : ''}>${pos}</option>
                    `).join('')}
                </select>
            </label><br>

            <label>모집 포지션:<br/>
                <div class="position-group" id="recruitPositionGroup"></div>
            </label><br>

            <button type="button" onclick="submitPartyForm()">${isEdit ? '수정 완료' : '모집 시작'}</button>
            <button type="button" onclick="closePartyPopup()">닫기</button>
        </div>
    `;

    popup.style.display = 'block';

    const getIcon = window.getPositionIconHTML;
    const container = popup.querySelector('#recruitPositionGroup');

    ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT', 'ALL'].forEach(pos => {
        const label = document.createElement('label');
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.name = 'positions';
        checkbox.value = pos;
        checkbox.style.display = 'none';

        if (party?.positions?.includes(pos)) {
            checkbox.checked = true;
            label.classList.add('selected');
        }

        const icon = createElementFromHTML(getIcon(pos, true));
        label.appendChild(checkbox);
        label.appendChild(icon);
        container.appendChild(label);

        label.addEventListener('click', (e) => {
            e.preventDefault();

            const isSelected = label.classList.contains('selected');
            const isAll = checkbox.value === 'ALL';

            const allLabels = container.querySelectorAll('label');
            const allCheckboxes = container.querySelectorAll('input[type="checkbox"]');

            if (isAll) {
                if (isSelected) {
                    checkbox.checked = false;
                    label.classList.remove('selected');
                } else {
                    allLabels.forEach(l => l.classList.remove('selected'));
                    allCheckboxes.forEach(c => c.checked = false);

                    checkbox.checked = true;
                    label.classList.add('selected');
                }
            } else {
                const allCheckbox = container.querySelector('input[value="ALL"]');
                const allLabel = allCheckbox?.closest('label');

                if (allCheckbox?.checked) {
                    allCheckbox.checked = false;
                    allLabel?.classList.remove('selected');
                }

                checkbox.checked = !isSelected;
                label.classList.toggle('selected', checkbox.checked);

                const selected = Array.from(container.querySelectorAll('label.selected input'))
                    .map(cb => cb.value)
                    .filter(v => v !== 'ALL');

                if (selected.length === 5) {
                    allLabels.forEach(l => l.classList.remove('selected'));
                    allCheckboxes.forEach(cb => cb.checked = false);

                    const allCb = container.querySelector('input[value="ALL"]');
                    const allLb = allCb.closest('label');
                    allCb.checked = true;
                    allLb.classList.add('selected');
                }
            }

            updatePartyHeadcountFromSelection(popup);
        });
    });

    setMinEndTime();
    updatePartyHeadcountFromSelection(popup);
}

/* 종료시간 계산 */
function setMinEndTime() {
    const input = document.querySelector('input[name="partyEndTime"]');
    if (!input) return;

    const now = new Date();
    now.setSeconds(0, 0); // 초, 밀리초 0으로 맞춤

    const yyyy = now.getFullYear();
    const MM = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');

    input.min = `${yyyy}-${MM}-${dd}T${hh}:${mm}`;
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
    const partyStatus = popup.querySelector('[name="partyStatus"]').value;
    const memo = popup.querySelector('textarea[name="memo"]').value;
    const mainPosition = popup.querySelector('select[name="mainPosition"]').value;

    // .selected된 label 안의 input value만 추출
    const positions = Array.from(popup.querySelectorAll('.position-group label.selected input'))
        .map(input => input.value);

    if (positions.length === 0) {
        alert("모집 포지션을 하나 이상 선택해주세요.");
        return;
    }

    const partyHeadcount = 1;
    const partyMax = positions.includes("ALL") ? 5 : Math.min(positions.length + 1, 5);

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
        method,
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
                return res.text().then(msg => {
                    alert(msg || '실패했습니다.');
                });
            }
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

function closePartyPopup() {
    const popup = document.getElementById('recruitPopup');
    popup.style.display = 'none';
    popup.innerHTML = '';
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '';
    return dateTimeStr.replace('T', ' ').slice(0, 16);
}

// 상태 값 한글로 변환
function translateStatus(status) {
    switch (status) {
        case "WAITING": return "모집 중";
        case "FULL": return "인원 꽉참";
        case "CLOSED": return "모집 마감";
        default: return status;
    }
}

function updatePartyHeadcountFromSelection(popup) {
    const selected = Array.from(popup.querySelectorAll('.position-group label.selected input'))
        .map(input => input.value);

    const maxInput = popup.querySelector("input[name='partyMax']");
    const headcountInput = popup.querySelector("input[name='partyHeadcount']");

    if (!maxInput || !headcountInput) return;

    if (selected.includes("ALL")) {
        maxInput.value = 5;
    } else {
        maxInput.value = Math.min(selected.length + 1, 5); // +1 = 파티장
    }

    headcountInput.value = 1;
}