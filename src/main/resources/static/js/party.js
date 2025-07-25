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
        const availablePositions = party.positions;
        const takenPositions = members
            .filter(m => m.status === 'ACCEPTED')
            .map(m => m.position);

        // 모집 포지션이 ALL인 경우 전체 포지션으로 확장
        const isAllPosition = availablePositions.length === 1 && availablePositions[0] === 'ALL';
        const positionPool = isAllPosition
            ? ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT']
            : availablePositions;

        const selectablePositions = positionPool.filter(pos => !takenPositions.includes(pos));

        const container = document.querySelector('.position-group');
        container.innerHTML = '';
        container.classList.add('join-position-group'); // 스타일 클래스 추가

        if (selectablePositions.length === 0) {
            container.innerHTML = `<p style="color:gray;">선택 가능한 포지션이 없습니다.</p>`;
        } else {
            // 모집 포지션 라디오 버튼을 아이콘으로 표시
            selectablePositions.forEach(pos => {
                const label = document.createElement('label');
                label.innerHTML = `
                    <input type="radio" name="joinPosition" value="${pos}" style="display: none;">
                    ${getIcon(pos)}
                `;
                container.appendChild(label);

                // 클릭 시 스타일 적용
                label.addEventListener('click', () => {
                    container.querySelectorAll('label').forEach(l => l.classList.remove('selected'));
                    label.classList.add('selected');
                    label.querySelector('input').checked = true;
                });
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

/* 솔로랭크 탭 선택 */
document.getElementById('soloTab').onclick = () => {
    setActiveTab('soloTab');
    loadParties('solo');
};

/* 자유랭크 탭 선택*/
document.getElementById('freeTab').onclick = () => {
    setActiveTab('freeTab');
    loadParties('team');
};

/* 내전 찾기 탭 선택 */
document.getElementById('scrimTab').onclick = () => {
    setActiveTab('scrimTab');
    loadParties('scrim');
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

    // 각 파티의 join-status를 모두 요청
    const statusPromises = data.map(party =>
        fetch(`/api/parties/${party.partySeq}/join-status`)
            .then(res => res.ok ? res.text() : 'NONE') // 실패하면 기본값
            .catch(() => 'NONE')
    );

    Promise.all(statusPromises).then(statusList => {
        data.forEach((party, idx) => {
            const joinStatus = statusList[idx];
            const canChat = (joinStatus === 'ACCEPTED');

            const item = document.createElement('div');
            item.className = 'recruit-item';

            const mainIcon = getPositionIconHTML(party.mainPosition, true);
            const recruitIcons = Array.isArray(party.positions)
                ? party.positions.map(p => getPositionIconHTML(p, true)).join(' ')
                : party.positions.split(',').map(p => getPositionIconHTML(p.trim(), true)).join(' ');

            // chat-icon 부분을 조건부로 처리
            const chatIconHtml = canChat
                ? `<span class="chat-icon" onclick="openPartyChat(${party.partySeq}, '${party.partyName}')">💬</span>`
                : '';

            item.innerHTML = `
            <span>${party.partyType.toUpperCase()}</span>
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
            <span title="메모">${party.memo || ''}</span>
            <span>${translateStatus(party.partyStatus)}</span>
            <span title="주 포지션">${mainIcon}</span>
            <span title="모집 포지션">${recruitIcons}</span>
            <span>${chatIconHtml}</span>
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
    });
}

function filterByMainPosition(koreanPos) {
    const code = positionMap[koreanPos];
    if (!code) return;

    if (code === 'ALL') {
        // 주 포지션이 TOP/JUNGLE/MID/ADC/SUPPORT 중 하나라도 해당되면 모두 출력
        renderParties(
            allParties.filter(p =>
                ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'].includes(p.mainPosition?.toUpperCase())
            )
        );
    } else {
        renderParties(
            allParties.filter(p => p.mainPosition?.toUpperCase() === code)
        );
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

async function showPartyDetail(seq, name, type, createDate, endDate, status, headcount, max, memo, mainPosition, positions, createdBy) {
    const popup = document.getElementById('partyDetailPopup');
    const currentUserId = document.querySelector('meta[name="user-id"]')?.getAttribute('content');
    const currentUserSeq = Number(document.querySelector('meta[name="login-user-seq"]')?.getAttribute('content'));
    const currentUserNickname = document.querySelector('meta[name="login-user-nickname"]')?.getAttribute('content');
    const isOwner = currentUserId && currentUserId === createdBy;

    fetch(`/api/parties/${seq}/join-status`)
        .then(res => res.text())
        .then(joinStatus => {
            let joinBtnHtml = '';

            if (status === 'WAITING') {
                if (currentUserId === createdBy) {
                    joinBtnHtml = `<p style="color: gray;"><strong>파티장은 참가 신청할 수 없습니다.</strong></p>`;
                } else if ((type === 'scrim' && headcount >= 10) || (type !== 'scrim' && headcount >= max)) {
                    joinBtnHtml = `<p style="color: red;"><strong>파티 인원이 모두 찼습니다.</strong></p>`;
                } else if (joinStatus === 'NONE') {
                    joinBtnHtml = type === 'scrim'
                        ? `<button onclick="openScrimJoinPopup(${seq})">팀 신청</button>`
                        : `<button onclick="openJoinPopup(${seq})">참가하기</button>`;
                } else if (joinStatus === 'PENDING') {
                    joinBtnHtml = `<p style="color: orange;"><strong>참가 신청 중입니다.</strong></p>`;
                } else if (joinStatus === 'ACCEPTED') {
                    joinBtnHtml = `<p style="color: green;"><strong>이미 참가한 파티입니다.</strong></p>`;
                } else if (joinStatus === 'REJECTED') {
                    joinBtnHtml = `<p style="color: red;"><strong>신청이 거절된 파티입니다.</strong></p>`;
                }
            }

            fetchPartyMembers(seq).then(async members => {
                const approved = members.filter(m => m.status === 'ACCEPTED');
                const pending = members.filter(m => m.status === 'PENDING');

                const detailHtml = `
                    <div class="detail-summary-box">
                        <p><strong>이름</strong><br>${name}</p>
                        <p><strong>타입</strong><br>${type.toUpperCase()}</p>
                        <p><strong>상태</strong><br>${translateStatus(status)}</p>
                        
                        <p><strong>생성일자</strong><br>${formatDateTime(createDate)}</p>
                        <p><strong>종료일자</strong><br>${formatDateTime(endDate)}</p>
                        <p><strong>현재 인원</strong><br>${headcount} / ${max}</p>
                        
                        <div class="position-row">
                            <div class="position-cell">
                                <strong>주 포지션</strong><br>
                                    ${getPositionIconHTML(mainPosition, true)}
                            </div>
                            <div class="position-cell">
                                <strong>모집 포지션</strong><br>
                                <div class="recruit-position-icons">
                                    ${positions.split(',').map(p => getPositionIconHTML(p.trim(), true)).join(' ')}
                                </div>
                            </div>
                        </div>
                        
                        <p style="grid-column: 1 / -1;"><strong>메모</strong><br>${memo?.trim() || '-'}</p>
                    </div>
                `;

                const positionOrder = {TOP: 0, JUNGLE: 1, MID: 2, ADC: 3, SUPPORT: 4};

                const renderScrimVsLayout = async (members) => {
                    const teamA = members.filter(m => m.role === 'A');
                    const teamB = members.filter(m => m.role === 'B');
                    const positionOrder = {TOP: 0, JUNGLE: 1, MID: 2, ADC: 3, SUPPORT: 4};

                    const buildTable = async (team) => {
                        const rows = await Promise.all(team.sort((a, b) => positionOrder[a.position] - positionOrder[b.position]).map(async m => {
                            const kda = m.averageKda || 0;
                            let kdaClass = 'kda-low';
                            if (kda >= 5) kdaClass = 'kda-great';
                            else if (kda >= 4) kdaClass = 'kda-good';
                            else if (kda >= 3) kdaClass = 'kda-mid';

                            // 여기에 relation-status API 호출
                            let isBlocked = false;
                            try {
                                const res = await fetch(`/api/users/${encodeURIComponent(m.userId)}/relation-status`);
                                if (res.ok) {
                                    const relation = await res.json();
                                    isBlocked = relation.isBlocked;
                                }
                            } catch (err) {
                                console.warn("차단 여부 조회 실패", err);
                            }

                            const nicknameHtml = m.userId === createdBy
                                ? `<span class="leader-icon">👑</span><strong class="${isBlocked ? 'blocked-name' : ''}">${m.userNickname}</strong>`
                                : `<span class="${isBlocked ? 'blocked-name' : ''}">${m.userNickname}</span>`;

                            return `
                                <tr>
                                    <td>${nicknameHtml}</td>
                                    <td>${getPositionIconHTML(m.position, true)}</td>
                                    <td>
                                        ${m.tierImageUrl ? `<img src="${m.tierImageUrl}" width="20" class="tier-icon" />` : ''}
                                        <span class="tier-text">${m.tier || 'Unranked'}</span>
                                    </td>
                                    <td>
                                        ${(m.championImageUrls || []).map(url => `<img src="${url}" class="champion-icon" width="24">`).join('')}
                                    </td>
                                    <td>${m.winRate != null ? `${m.winRate.toFixed(0)}%` : '0%'}</td>
                                    <td class="${kdaClass}">${kda.toFixed(2)}</td>
                                </tr>
                            `;
                                        }));

                                        return `<table class="member-table">
                            <thead>
                                <tr>
                                    <th>닉네임</th><th>포지션</th><th>티어</th>
                                    <th>선호 챔피언</th><th>승률</th><th>KDA</th>
                                </tr>
                            </thead>
                            <tbody>${rows.join('')}</tbody>
                        </table>`;
                                    };

                                    const tableA = await buildTable(teamA);
                                    const tableB = await buildTable(teamB);

                                    return `
                        <div class="scrim-vs-layout">
                            <div class="team-table">
                                <h4>A 팀</h4>
                                ${tableA}
                            </div>
                            <div class="vs-text">VS</div>
                            <div class="team-table">
                                <h4>B 팀</h4>
                                ${tableB}
                            </div>
                        </div>
                    `;
                };

                const renderDefaultTable = async () => {
                    let sortedApproved = approved;
                    if (type === 'solo') {
                        sortedApproved = approved.sort((a, b) => {
                            if (a.userId === createdBy) return -1;
                            if (b.userId === createdBy) return 1;
                            return 0;
                        });
                    }

                    const rows = await Promise.all(approved.map(async m => {
                        const res = await fetch(`/api/users/${encodeURIComponent(m.userId)}/relation-status`);
                        const relation = await res.json();

                        const isLoggedIn = !!currentUserId;
                        const isCurrentUser = m.userNickname === currentUserNickname; // 닉네임으로 본인 여부 판단
                        const isLeader = m.userSeq === currentUserSeq && isOwner;
                        const canInteract = isOwner || joinStatus === 'ACCEPTED';

                        const icon = getPositionIconHTML(m.position, true);

                        // 👑 왕관 표시
                        const isBlocked = relation.isBlocked;
                        const nicknameHtml = m.userId === createdBy
                            ? `<span class="leader-icon">👑</span><strong class="${isBlocked ? 'blocked-name' : ''}">${m.userNickname}</strong>`
                            : `<span class="${isBlocked ? 'blocked-name' : ''}">${m.userNickname}</span>`;

                        // 버튼들 조건 분기
                        const kickBtn = (isOwner && !isCurrentUser)
                            ? `<button onclick="kickMember(${seq}, ${m.id})">내보내기</button>` : '';

                        const leaveBtn = (isLoggedIn && !isOwner && isCurrentUser)
                            ? `<button onclick="leaveParty(${seq})">나가기</button>` : '';

                        const friendBtn = (isLoggedIn && canInteract && !isCurrentUser && !isLeader && !relation.isFriend && !relation.isBlocked)
                            ? `<button onclick="openFriendMemoPopup('${m.userId}')">친구신청</button>` : '';

                        const blockBtn = (isLoggedIn && canInteract && !isCurrentUser && !isLeader && !relation.isBlocked)
                            ? `<button onclick="blockMember('${m.userId}')">차단</button>` : '';

                        // KDA 색상 클래스
                        const kda = m.averageKda || 0;
                        let kdaClass = 'kda-low';
                        if (kda >= 5) kdaClass = 'kda-great';
                        else if (kda >= 4) kdaClass = 'kda-good';
                        else if (kda >= 3) kdaClass = 'kda-mid';

                        return `
                            <tr>
                                <td>${nicknameHtml}</td>
                                <td>${icon}</td>
                                <td>
                                    ${m.tierImageUrl ? `<img src="${m.tierImageUrl}" width="20" class="tier-icon" />` : ''}
                                    <span class="tier-text">${m.tier || 'Unranked'}</span>
                                </td>
                                <td>
                                    ${(m.championImageUrls || []).map(url => `<img src="${url}" class="champion-icon" width="24" />`).join('')}
                                </td>
                                <td>${m.winRate != null ? `${m.winRate.toFixed(0)}%` : '0%'}</td>
                                <td class="${kdaClass}">${kda.toFixed(2)}</td>
                                <td>${kickBtn} ${leaveBtn} ${friendBtn} ${blockBtn}</td>
                            </tr>
                        `;
                    }));

                    return `
                        <table class="member-table">
                            <thead>
                                <tr>
                                    <th>닉네임</th>
                                    <th>포지션</th>
                                    <th>티어</th>
                                    <th>선호 챔피언</th>
                                    <th>승률</th>
                                    <th>KDA</th>
                                    <th>관리</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${rows.join('')}
                            </tbody>
                        </table>
                    `;
                };

                /* 멤버 수락 */
                const pendingHtml = await renderPendingTable(type, pending, seq, isOwner);

                const approvedHtml = (type === 'scrim')
                    ? await renderScrimVsLayout(approved)
                    : await renderDefaultTable();

                const partyObj = {
                    partySeq: seq,
                    partyName: name,
                    partyType: type,
                    partyCreateDate: createDate,
                    partyEndTime: endDate,
                    partyStatus: status,
                    partyHeadcount: headcount,
                    partyMax: max,
                    memo,
                    mainPosition,
                    positions: positions.split(',').map(p => p.trim())
                };
                const encodedPartyJson = encodeURIComponent(JSON.stringify(partyObj));

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
                            ${isOwner ? `
                            <button class="edit-btn" data-party='${encodedPartyJson}'>수정</button>
                            <button onclick="deleteParty(${seq})">삭제</button>
                            ` : ''}
                            <button onclick="closePartyDetail()">닫기</button>
                            ${joinBtnHtml}
                        </div>
                    `;

                setTimeout(() => {
                    document.querySelectorAll('.edit-btn').forEach(btn => {
                        btn.addEventListener('click', () => {
                            const raw = btn.dataset.party;
                            try {
                                const partyObj = JSON.parse(decodeURIComponent(raw));
                                handleEditFromDetail(JSON.stringify(partyObj));
                            } catch (e) {
                                console.error("파티 JSON 파싱 오류", e);
                                alert("파티 정보 처리 중 오류 발생");
                            }
                        });
                    });
                }, 0);

                popup.style.display = 'block';
            });
            });
}

async function renderPendingTable(type, pending, seq, isOwner) {
    if (!pending.length) {
        const label = type === 'scrim' ? '팀' : '멤버';
        return `<p style="text-align:center;color:gray;">수락 대기 중인 ${label}이 없습니다.</p>`;
    }

    // — 내전(scrim)은 팀 단위 수락/거절 —
    if (type === 'scrim') {
        return await renderScrimPendingTeams(pending, seq, isOwner);
    }

    // — SOLO/TEAM: 개별 멤버 수락/거절 —
    const rows = await Promise.all(pending.map(async m => {
        const kda = (m.averageKda || 0).toFixed(2);
        let cls = 'kda-low';
        if (kda >= 5) cls = 'kda-great';
        else if (kda >= 4) cls = 'kda-good';
        else if (kda >= 3) cls = 'kda-mid';

        // relation-status API 호출
        let isBlocked = false;
        try {
            const res = await fetch(`/api/users/${encodeURIComponent(m.userId)}/relation-status`);
            if (res.ok) {
                const relation = await res.json();
                isBlocked = relation.isBlocked;
            }
        } catch (err) {
            console.warn("차단 여부 조회 실패", err);
        }

        const nicknameHtml = `<span class="${isBlocked ? 'blocked-name' : ''}">${m.userNickname}</span>`;

        return `
            <tr>
                <td>${nicknameHtml}</td>
                <td>${getPositionIconHTML(m.position, true)}</td>
                <td>
                    ${m.tierImageUrl ? `<img src="${m.tierImageUrl}" width="20" class="tier-icon"/>` : ''}
                    <span class="tier-text">${m.tier || 'Unranked'}</span>
                </td>
                <td>${(m.championImageUrls || []).map(u => `<img src="${u}" width="24" class="champion-icon"/>`).join('')}</td>
                <td>${m.winRate != null ? m.winRate.toFixed(0) + '%' : '0%'}</td>
                <td class="${cls}">${kda}</td>
                <td>
                    ${isOwner
            ? `<button onclick="approveMember(${seq}, ${m.id})">수락</button>
               <button onclick="rejectMember(${seq}, ${m.id})">거절</button>`
            : ''}
                </td>
            </tr>`;
    }));

    return `
        <table class="member-table">
            <thead>
                <tr>
                    <th>닉네임</th><th>포지션</th><th>티어</th>
                    <th>선호 챔프</th><th>승률</th><th>KDA</th><th>관리</th>
                </tr>
            </thead>
            <tbody>${rows.join('')}</tbody>
        </table>`;
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

/* 파티 나가기 */
function leaveParty(partyId) {
    if (!confirm("정말 이 파티에서 나가시겠습니까?")) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${partyId}/members/leave`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        }
    })
        .then(res => {
            if (res.ok) {
                alert("파티에서 나갔습니다.");
                closePartyDetail();
                loadParties('team');
            } else {
                return res.text().then(msg => alert("실패: " + msg));
            }
        })
        .catch(err => {
            console.error(err);
            alert("나가기 중 오류 발생");
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
    const activeTab = document.querySelector('.tab.active').id;

    const partyType = activeTab === 'freeTab' ? 'team'
        : activeTab === 'scrimTab' ? 'scrim'
            : 'solo';

    if (partyType === 'scrim') {
        openScrimCreatePopup(); // 기존 scrim 전용 팝업
    } else {
        openPartyFormPopup();
    }
}

function handleEditFromDetail(partyJson) {
    closePartyDetail();
    const party = JSON.parse(decodeURIComponent(partyJson));
    openPartyFormPopup(party);
}

function openPartyFormPopup(party = null) {
    const isReadOnly = party?.partyType === 'scrim';
    const csrfParam = document.querySelector('meta[name="_csrf_parameter"]').getAttribute('content');
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const getIcon = window.getPositionIconHTML;

    const popup = document.getElementById('recruitPopup');
    const isEdit = party != null && party.partySeq != null;

    const activeTab = document.querySelector('.tab.active')?.id;
    const fixedType = activeTab === 'freeTab' ? 'team'
        : activeTab === 'scrimTab' ? 'scrim'
            : 'solo';

    popup.innerHTML = `
      <h3>${isEdit ? '파티 수정하기' : '새 파티 등록하기'}</h3>
      <div class="party-form">
        <input type="hidden" name="${csrfParam}" value="${csrfToken}" />
        ${isEdit ? `<input type="hidden" name="partySeq" value="${party.partySeq}">` : ''}

        <div class="party-row">
          <div class="field-group">
            <label>파티 이름</label>
            <input type="text" name="partyName" maxlength="20" placeholder="예 : 즐겁게 게임 하실 분! (최대 20자)" value="${party?.partyName ?? ''}" required>
          </div>
          <div class="field-group">
            <label>종료일자</label>
            <input type="datetime-local" id="partyEndTime" name="partyEndTime" value="${formatLocalDateTime(party?.partyEndTime)}" required>
          </div>
          ${isEdit ? `
            <div class="field-group">
              <label>상태</label>
              <select name="partyStatus" required>
                ${[
        { value: 'WAITING', label: '모집 중' },
        { value: 'FULL', label: '인원 꽉참' },
        { value: 'CLOSED', label: '모집 마감' }
    ].map(opt => `
                  <option value="${opt.value}" ${party?.partyStatus === opt.value ? 'selected' : ''}>${opt.label}</option>
                `).join('')}
              </select>
            </div>
          ` : `<input type="hidden" name="partyStatus" value="WAITING">`}
        </div>

        <div class="position-type-row">
          <div class="party-type-selector">
            <label>타입</label>
            <div class="fixed-party-type" style="margin-top: 6px; font-weight: bold; color: black;">
              ${party?.partyType === 'team' ? '자유랭크'
        : party?.partyType === 'scrim' ? '내전찾기'
            : party?.partyType === 'solo' ? '솔로랭크'
                : fixedType === 'team' ? '자유랭크'
                    : fixedType === 'scrim' ? '내전찾기'
                        : '솔로랭크'}
            </div>
            <input type="hidden" name="partyType" value="${party?.partyType ?? fixedType}">
          </div>

          <div class="main-position-selector-wrapper">
            <label>주 포지션</label>
            <div class="main-position-selector" id="mainPositionGroup">
              ${
                    // scrim은 무조건 ALL 하나만
                    party?.partyType === 'scrim'
                        ? `
                    <label class="selected" data-value="ALL" style="pointer-events:none;opacity:0.6;">
                      ${getIcon('ALL', true)}
                      <input type="radio" name="mainPosition" value="ALL" style="display:none;" checked disabled />
                    </label>
                  `
                        : ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'].map(pos => {
                            const selected = party?.mainPosition === pos ? 'selected' : '';
                            return `
                        <label class="${selected}" data-value="${pos}" style="${isReadOnly ? 'pointer-events:none;opacity:0.6;' : ''}">
                          ${getIcon(pos, true)}
                          <input type="radio" name="mainPosition" value="${pos}" style="display:none;" ${selected ? 'checked' : ''} ${isReadOnly ? 'disabled' : ''} />
                        </label>
                      `;
                        }).join('')
                }
            </div>
          </div>

          <div class="position-group-wrapper">
            <label>모집 포지션</label>
            <div class="position-group" id="recruitPositionGroup"></div>
          </div>
        </div>

        ${isEdit ? `<label>생성일자: <input type="datetime-local" name="partyCreateDate" value="${formatLocalDateTime(party?.partyCreateDate)}" readonly><br>` : ''}

        <label>메모 (선택)<br><textarea name="memo" maxlength="200" rows="3" cols="40">${party?.memo ?? ''}</textarea></label><br>
        
        <div class="form-buttons">
            ${isEdit
                ? `<button type="button" onclick="submitPartyForm()">수정</button>`
                : `<button type="button" onclick="submitPartyForm()">모집 시작</button>`
            }
            <button type="button" onclick="closePartyPopup()">닫기</button>
        </div>
      </div>
    `;

    popup.style.display = 'block';

    const container = popup.querySelector('#recruitPositionGroup');
    container.innerHTML = '';

    // scrim 편집일 땐 ALL만, 아니면 기존 목록
    const positionsToRender = isReadOnly
        ? ['ALL']
        : ['TOP','JUNGLE','MID','ADC','SUPPORT','ALL'];

    positionsToRender.forEach(pos => {
        const label = document.createElement('label');
        if (isReadOnly) {
            label.style.pointerEvents = 'none';
            label.style.opacity = '0.6';
        }

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.name = 'positions';
        checkbox.value = pos;
        checkbox.style.display = 'none';
        if (isReadOnly) checkbox.disabled = true;

        // scrim 편집이거나, 기존에 선택된 포지션이면 체크
        if (isReadOnly || party?.positions?.includes(pos)) {
            checkbox.checked = true;
            label.classList.add('selected');
        }

        label.appendChild(checkbox);
        label.appendChild(createElementFromHTML(getIcon(pos, true)));
        container.appendChild(label);

        if (!isReadOnly) {
            label.addEventListener('click', (e) => {
                e.preventDefault();
                const isSelected = label.classList.contains('selected');
                const isAll = checkbox.value === 'ALL';
                const allLabels = container.querySelectorAll('label');
                const allCheckboxes = container.querySelectorAll('input[type="checkbox"]');

                if (isAll) {
                    allLabels.forEach(l => l.classList.remove('selected'));
                    allCheckboxes.forEach(c => c.checked = false);
                    checkbox.checked = true;
                    label.classList.add('selected');
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
                        .map(cb => cb.value).filter(v => v !== 'ALL');

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
        }
    });

    setMinEndTime();
    updatePartyHeadcountFromSelection(popup);

    const mainGroup = popup.querySelector('#mainPositionGroup');
    if (!isReadOnly) {
        mainGroup.querySelectorAll('label').forEach(label => {
            label.addEventListener('click', (e) => {
                e.preventDefault(); // 기본 동작 방지

                const radio = label.querySelector('input');
                const isSelected = label.classList.contains('selected');

                if (isSelected) {
                    label.classList.remove('selected');
                    radio.checked = false;
                } else {
                    mainGroup.querySelectorAll('label').forEach(l => l.classList.remove('selected'));
                    mainGroup.querySelectorAll('input[type="radio"]').forEach(r => r.checked = false);
                    label.classList.add('selected');
                    radio.checked = true;
                }

                const selected = radio.value;

                // 주 포지션이 선택되면 동일한 모집 포지션은 비활성화
                popup.querySelectorAll('.position-group input[type="checkbox"]').forEach(chk => {
                    if (chk.value === selected) {
                        chk.checked = false;
                        chk.disabled = true;
                        chk.closest('label')?.classList.remove('selected');
                    } else {
                        chk.disabled = false;
                    }
                });

                updatePartyHeadcountFromSelection(popup);
            });
        });
    }
}


/* 시간포맷팅*/
function formatLocalDateTime(datetimeString) {
    if (!datetimeString) return '';
    const date = new Date(datetimeString);
    const pad = n => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
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
    const partyName = popup.querySelector('input[name="partyName"]').value.trim();
    const partyType = popup.querySelector('[name="partyType"]').value;
    const partyEndTime = popup.querySelector('input[name="partyEndTime"]').value;
    const partyStatus = popup.querySelector('[name="partyStatus"]')?.value ?? 'WAITING';
    const memo = popup.querySelector('textarea[name="memo"]').value.trim();

    // scrim일 경우 고정 처리
    let mainPosition, positions;

    if (partyType === 'scrim') {
        mainPosition = 'ALL';
        positions = ['ALL'];
    } else {
        mainPosition = popup.querySelector('.main-position-selector label.selected input')?.value;

        if (!mainPosition) {
            alert("주 포지션을 선택해주세요.");
            return;
        }

        positions = Array.from(popup.querySelectorAll('.position-group label.selected input'))
            .map(input => input.value);

        if (positions.length === 0) {
            alert("모집 포지션을 하나 이상 선택해주세요.");
            return;
        }

        if (positions.includes(mainPosition)) {
            alert("주 포지션과 같은 포지션은 모집할 수 없습니다.");
            return;
        }
    }

    const partyHeadcount = 1;
    let partyMax;
    if (partyType === 'scrim') {
        mainPosition = 'ALL';
        positions = ['ALL'];
        partyMax = 10; // 내전 최대 인원은 10명
    } else {
        mainPosition = popup.querySelector('.main-position-selector label.selected input')?.value;

        if (!mainPosition) {
            alert("주 포지션을 선택해주세요.");
            return;
        }

        positions = Array.from(popup.querySelectorAll('.position-group label.selected input'))
            .map(input => input.value);

        if (positions.length === 0) {
            alert("모집 포지션을 하나 이상 선택해주세요.");
            return;
        }

        if (positions.includes(mainPosition)) {
            alert("주 포지션과 같은 포지션은 모집할 수 없습니다.");
            return;
        }

        partyMax = positions.includes("ALL") ? 5 : Math.min(positions.length + 1, 5);
    }

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
                const type = activeTab === 'freeTab' ? 'team'
                    : activeTab === 'scrimTab' ? 'scrim'
                        : 'solo';
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

// 친구 신청 팝업
function openFriendMemoPopup(nickname) {
    const memo = prompt("메모를 입력해주세요 (선택):", "");
    if (memo !== null) {
        const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
        const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
        fetch(`/api/friends/request`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({ toUserId: nickname, memo: memo })
        })
            .then(res => {
                if (res.ok) alert("친구 요청을 보냈습니다.");
                else alert("친구 요청에 실패했습니다.");
            });
    }
}


// 차단
function blockMember(targetUserId) {
    // 친구 여부 확인
    fetch(`/api/friends/check?targetUserId=${encodeURIComponent(targetUserId)}`)
        .then(res => res.json())
        .then(isFriend => {
            let proceed = true;
            if (isFriend) {
                proceed = confirm("이 사용자는 친구입니다. 차단하면 친구 목록에서도 삭제됩니다. 계속하시겠습니까?");
            } else {
                proceed = confirm("정말로 이 사용자를 차단하시겠습니까?");
            }

            if (!proceed) return;

            // 차단 진행
            const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
            const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
            fetch(`/api/blocks/direct`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ blockedUserId: targetUserId })
            })
                .then(res => {
                    if (res.ok) alert("차단되었습니다.");
                    else alert("차단에 실패했습니다.");
                });
        });
}

/* 내전찾기 팀 수락 */
function approveTeam(partyId, memberIds) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/api/parties/${partyId}/members/approve-team`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({ memberIds })
    })
        .then(res => {
            if (res.ok) {
                alert('팀 수락 완료');
                closePartyDetail();
                loadParties('scrim');
            } else {
                res.text().then(msg => alert('수락 실패: ' + msg));
            }
        });
}

/* 내전 팀 찾기 */
function openScrimJoinPopup(partyId) {
    selectedPartyId = partyId;
    const popup = document.getElementById('scrimJoinPopup');
    const container = document.getElementById('scrimJoinTeamInputs');
    container.innerHTML = '';

    const positions = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'];

    for (let i = 0; i < 5; i++) {
        const pos = positions[i];

        const div = document.createElement('div');
        div.classList.add('team-member-row'); // flex 스타일 클래스

        div.innerHTML = `
            <div class="position-icon">${getPositionIconHTML(pos, true)}</div>
            <input type="text" name="nickname" placeholder="닉네임 ${i + 1}" required>
            <input type="hidden" name="position" value="${pos}">
        `;
        container.appendChild(div);
    }

    popup.style.display = 'block';
}

function closeScrimJoinPopup() {
    document.getElementById('scrimJoinPopup').style.display = 'none';
}


/* 내전 팀 신청 요청 */
function submitScrimJoinRequest() {
    const form = document.getElementById('scrimJoinForm');
    const nicknames = form.querySelectorAll('input[name="nickname"]');
    const positions = form.querySelectorAll('input[name="position"]');
    const message = form.querySelector('textarea[name="message"]').value.trim();

    const teamMembers = [];
    const nicknameSet = new Set();

    for (let i = 0; i < 5; i++) {
        const userId = nicknames[i].value.trim();
        const position = positions[i].value;

        if (!userId) {
            alert(`닉네임 ${i + 1}을 입력해주세요.`);
            return;
        }

        if (nicknameSet.has(userId)) {
            alert(`중복된 닉네임이 있습니다: ${userId}`);
            return;
        }
        nicknameSet.add(userId);

        teamMembers.push({ userNickname: userId, position });
    }

    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

    fetch(`/api/parties/${selectedPartyId}/scrim-join`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({ message, teamMembers })
    })
        .then(res => res.text())
        .then(text => {
            if (text === 'OK') {
                alert('신청 완료!');
                closeScrimJoinPopup();
                closePartyDetail();
                loadParties('scrim');
            } else {
                alert("신청 실패: " + text);
            }
        })
        .catch(err => {
            console.error(err);
            alert("신청 중 오류 발생");
        });
}

function openScrimCreatePopup() {
    const popup = document.getElementById('scrimCreatePopup');
    const container = document.getElementById('scrimCreateTeamInputs');
    container.innerHTML = ''; // 초기화

    const positions = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT'];

    for (let i = 0; i < 5; i++) {
        const pos = positions[i];

        const div = document.createElement('div');
        div.classList.add('team-member-row');
        div.innerHTML = `
            <div class="position-icon">${getPositionIconHTML(pos, true)}</div>
            <input type="text" name="nickname" placeholder="닉네임 ${i + 1}" required>
            <input type="hidden" name="position" value="${pos}">
        `;
        container.appendChild(div);
    }

    popup.style.display = 'block';
}

function closeScrimCreatePopup() {
    document.getElementById('scrimCreatePopup').style.display = 'none';
}

function submitScrimCreateForm() {
    const name = document.getElementById('scrimPartyName').value.trim();
    const endTime = document.getElementById('scrimPartyEndTime').value;
    const memo = document.getElementById('scrimPartyMemo').value.trim();

    const nicknames = document.querySelectorAll('#scrimCreateTeamInputs input[name="nickname"]');
    const positionInputs = document.querySelectorAll('#scrimCreateTeamInputs input[name="position"]');

    if (!name) return alert("파티 이름을 입력해주세요.");
    if (!endTime) return alert("종료일자를 입력해주세요.");

    const teamMembers = [];

    for (let i = 0; i < 5; i++) {
        const userId = nicknames[i].value.trim();
        const position = positionInputs[i].value;

        if (!userId) return alert(`${i + 1}번 팀원의 닉네임을 입력해주세요.`);
        if (!position) return alert(`${i + 1}번 팀원의 포지션 값이 비어있습니다.`);

        teamMembers.push({ userNickname: userId, position });
    }

    const data = {
        partyName: name,
        partyEndTime: endTime,
        memo,
        teamMembers
    };

    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

    fetch('/api/parties/scrim-create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        credentials: 'include',
        body: JSON.stringify(data)
    })
        .then(res => res.text().then(text => ({ ok: res.ok, text })))
        .then(({ ok, text }) => {
            if (ok && text === 'OK') {
                alert('내전 파티가 성공적으로 생성되었습니다!');
                closeScrimCreatePopup();
                loadParties('scrim');
            } else {
                alert(text);
            }
        })
        .catch(err => {
            console.error(err);
            alert('서버 오류가 발생했습니다.');
        });
}


async function renderScrimPendingTeams(pending, partySeq, isOwner) {
    if (!pending.length) {
        return `<p style="text-align:center;color:gray;">수락 대기 중인 팀이 없습니다.</p>`;
    }

    const teams = pending.reduce((acc, m) => {
        (acc[m.teamId] = acc[m.teamId] || []).push(m);
        return acc;
    }, {});

    const teamEntries = await Promise.all(Object.entries(teams).map(async ([teamId, members]) => {
        const memberIds = members.map(m => m.id);

        const rows = await Promise.all(members.map(async m => {
            const kda = m.averageKda?.toFixed(2) ?? '0.00';
            let kdaClass = 'kda-low';
            if (kda >= 5) kdaClass = 'kda-great';
            else if (kda >= 4) kdaClass = 'kda-good';
            else if (kda >= 3) kdaClass = 'kda-mid';

            // relation-status API로 차단 여부 확인
            let isBlocked = false;
            try {
                const res = await fetch(`/api/users/${encodeURIComponent(m.userId)}/relation-status`);
                if (res.ok) {
                    const relation = await res.json();
                    isBlocked = relation.isBlocked;
                }
            } catch (err) {
                console.warn("차단 여부 조회 실패", err);
            }

            const nicknameHtml = `<span class="${isBlocked ? 'blocked-name' : ''}">${m.userNickname}</span>`;

            return `
                <tr>
                    <td>${nicknameHtml}</td>
                    <td>${getPositionIconHTML(m.position, true)}</td>
                    <td>
                        ${m.tierImageUrl ? `<img src="${m.tierImageUrl}" width="20" class="tier-icon" />` : ''}
                        <span class="tier-text">${m.tier || 'Unranked'}</span>
                    </td>
                    <td>
                        ${(m.championImageUrls || []).map(url => `<img src="${url}" class="champion-icon" width="24" />`).join('')}
                    </td>
                    <td>${m.winRate != null ? `${m.winRate.toFixed(0)}%` : '0%'}</td>
                    <td class="${kdaClass}">${kda}</td>
                    <td></td>
                </tr>
            `;
        }));

        const actionButtons = isOwner
            ? `<div style="text-align:right; margin-top:8px;">
                    <button onclick="approveTeam(${partySeq}, [${memberIds.join(',')}])">팀 수락</button>
                    <button onclick="rejectTeam(${partySeq}, [${memberIds.join(',')}])">팀 거절</button>
               </div>`
            : '';

        return `
            <div class="team-table">
                <table class="member-table">
                    <thead>
                        <tr>
                            <th>닉네임</th><th>포지션</th><th>티어</th>
                            <th>선호 챔프</th><th>승률</th><th>KDA</th><th></th>
                        </tr>
                    </thead>
                    <tbody>${rows.join('')}</tbody>
                </table>
                ${actionButtons}
            </div>
        `;
    }));

    return teamEntries.join('');
}