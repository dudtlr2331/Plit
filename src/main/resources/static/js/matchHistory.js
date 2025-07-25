console.log("JS loaded");
document.addEventListener('DOMContentLoaded', () => {

    const updateBtn = document.getElementById('update-btn');
    if (updateBtn) {
        updateBtn.addEventListener('click', updateMatch);
    }

    // 왼쪽 파넬 - 랭크 모드별 선호챔피언 정보
    const buttons = document.querySelectorAll(".tab-button");

    function activateTab(mode) {
        const tabContents = document.querySelectorAll(".champion-tab-content");
        // 버튼 클래스 갱신
        buttons.forEach(btn => btn.classList.toggle("active", btn.dataset.mode === mode));
        tabContents.forEach(tab => {
            const isActive = tab.dataset.mode === mode;
            tab.style.display = isActive ? "block" : "none";

            const rows = tab.querySelectorAll(".champion-row");
            const moreBtn = tab.querySelector(".show-more-button");

            rows.forEach((row, i) => {
                row.classList.toggle("hidden-champ", i >= 6);
            });

            if (moreBtn) {
                moreBtn.style.display = rows.length > 6 ? "block" : "none";
            }
        });
    }

    buttons.forEach((btn => {
        btn.addEventListener("click", () => { activateTab(btn.dataset.mode);
        });
    }));

    activateTab("overall"); // 초기 설정

    // 매치 요약 5개씩 표시
    const matchCards = document.querySelectorAll('.match-block');
    const loadMoreButton = document.getElementById('match-load-more-button');
    const closeButton = document.getElementById('match-close-button');
    let visibleCount = 5;
    const step = 5;

    function updateVisibleMatches() {
        matchCards.forEach((card, index) => {
            card.style.display = index < visibleCount ? 'block' : 'none';
        });

        // 더보기 버튼 표시/숨김
        if (loadMoreButton) {
            loadMoreButton.style.display = visibleCount >= matchCards.length ? 'none' : 'block';
        }

        // 닫기 버튼 표시/숨김 (모든 매치가 보일 때만)
        if (closeButton) {
            closeButton.style.display = visibleCount >= matchCards.length ? 'block' : 'none';
        }
    }

    if (loadMoreButton) {
        loadMoreButton.addEventListener('click', function () {
            visibleCount += step;
            updateVisibleMatches();
        });
    }

    if (closeButton) {
        closeButton.addEventListener('click', function () {
            visibleCount = 5; // 처음 5개로 되돌리기
            updateVisibleMatches();
        });
    }

    // 처음 상태 반영
    updateVisibleMatches();

    // 처음 초기화할 때 팝업 창
    const patchPopupShown = localStorage.getItem("patchPopupShown");

    if (!patchPopupShown) {
        const images = [
            "/images/patch/25.14/img1.png"
        ];
        for (let i = 2; i <= 5; i++) {
            images.push(`/images/patch/25.14/img${i}.jpg`);
        }

        let currentIndex = 0;
        const patchImage = document.getElementById("patchImage");

        function rotateImage() {
            patchImage.src = images[currentIndex];
            currentIndex = (currentIndex + 1) % images.length;
        }

        // 처음 이미지
        rotateImage();
        // 3초마다 이미지 변경
        setInterval(rotateImage, 3000);

        // 내용 설정
        document.getElementById("patchTitle").innerText = "시즌 중간 조정!";
        document.getElementById("patchContent").innerText = "25.14 패치에서는 챔피언과 특성, 시스템 변화가 포함되어 있어요.";

        // 팝업 보여주기
        document.getElementById("patchPopup").style.display = "block";

        // 다시 안 보이게 플래그 저장
        localStorage.setItem("patchPopupShown", "true");
    }
});

// 더보기 버튼
function showMoreChampions(button) {
    const parent = button.closest('.champion-tab-content');
    const rows = parent.querySelectorAll('.champion-row');
    const lessBtn = parent.querySelector('.show-less-button');
    
    // 현재 보이는 챔피언 수 계산
    let visibleCount = 0;
    rows.forEach(row => {
        if (!row.classList.contains('hidden-champ')) {
            visibleCount++;
        }
    });
    
    // 6개씩 추가로 보이게 하기
    const nextCount = visibleCount + 6;
    
    rows.forEach((row, index) => {
        if (index < nextCount) {
            row.classList.remove('hidden-champ');
        }
    });
    
    // 모든 챔피언이 보이면 더보기 버튼 숨기고 닫기 버튼 표시
    if (nextCount >= rows.length) {
        button.style.display = 'none';
        if (lessBtn) {
            lessBtn.style.display = 'block';
        }
    }
}

// 닫기 버튼
function showLessChampions(button) {
    const parent = button.closest('.champion-tab-content');
    const rows = parent.querySelectorAll('.champion-row');
    const moreBtn = parent.querySelector('.show-more-button');

    rows.forEach((row, index) => {
        if (index > 5) {
            row.classList.add('hidden-champ');
        }
    });

    button.style.display = 'none';
    
    if (moreBtn) {
        moreBtn.style.display = 'block';
    }
}

// 매치 요약 박스 클릭시 실행( 상세 페이지 )  (element) 는 match-summary-box
function loadMatchDetail(element) {
    const matchId = element.getAttribute("data-match-id");
    const puuid = element.getAttribute("data-puuid");
    const detailBox = element.nextElementSibling;

    if (detailBox.getAttribute("data-loaded") === "true") {
        detailBox.style.display = detailBox.style.display === "none" ? "block" : "none";
        // 화살표 회전 토글
        element.classList.toggle("expanded", detailBox.style.display === "block");
        return;
    }

    // Ajax 요청
    fetch(`/match/detail?matchId=${matchId}&puuid=${puuid}`)
        .then(res => res.json())
        .then(data => {
            const table = detailBox.querySelector(".match-detail-table");
            const isBlueWin = data.blueTeam[0]?.win;
            const blueColorClass = isBlueWin ? 'blue-text' : 'red-text';
            const redColorClass = isBlueWin ? 'red-text' : 'blue-text';

            const buildTeamObjectiveIcons = (obj, colorClass) => `
                <div><img src="/images/objective/tower.svg" class="objective-icon ${colorClass}" alt="타워"> ${obj.towerKills}</div>
                <div><img src="/images/objective/dragon.svg" class="objective-icon ${colorClass}" alt="드래곤"> ${obj.dragonKills}</div>
                <div><img src="/images/objective/baron.svg" class="objective-icon ${colorClass}" alt="바론"> ${obj.baronKills}</div>
                <div><img src="/images/objective/herald.svg" class="objective-icon ${colorClass}" alt="전령"> ${obj.heraldKills}</div>
                <div><img src="/images/objective/rift.svg" class="objective-icon ${colorClass}" alt="균열"> ${obj.riftKills}</div>
            `;

            // 블루 = 왼쪽, 레드 = 오른쪽 (위치 고정)
            detailBox.querySelector('.left-objectives').innerHTML = buildTeamObjectiveIcons(data.blueObjectives, blueColorClass);
            detailBox.querySelector('.right-objectives').innerHTML = buildTeamObjectiveIcons(data.redObjectives, redColorClass);

            // bar 업데이트
            updateObjectiveBar(data.redObjectives.totalKills, data.blueObjectives.totalKills, 'red-kill-bar', 'blue-kill-bar', 'red-kill-count', 'blue-kill-count');
            updateObjectiveBar(data.redObjectives.totalGold, data.blueObjectives.totalGold, 'red-gold-bar', 'blue-gold-bar', 'red-gold-count', 'blue-gold-count');

            // 나머지 makeRow, 테이블 구성도 동일하게 유지
            const makeRow = (player) => {
                const damageDealt = (player.totalDamageDealtToChampions * 100 / data.totalMaxDamage).toFixed(1);
                const damageTaken = (player.totalDamageTaken * 100 / data.totalMaxDamage).toFixed(1);
                const csPerMin = player.csPerMin.toFixed(2);
                const itemsHtml = (player.itemImageUrls || []).map(img => img ? `<img src="${img}" class="item-icon">` : '').join('');

                return `
                    <tr>
                        <td>
                            <div class="player-summary-cell">
                                <img src="${player.championImageUrl}" class="champion-icon" alt="챔피언"/>
                                <div class="runes">
                                    <img src="${player.mainRune1Url}" class="rune-icon" alt="메인 룬"/>
                                    <img src="${player.mainRune2Url}" class="rune-icon" alt="메인 룬"/>
                                    <img src="${player.statRune1Url}" class="rune-icon" alt="스탯 룬"/>
                                    <img src="${player.statRune2Url}" class="rune-icon" alt="스탯 룬"/>
                                </div>
                                <div class="summoner-info">
                                    <div class="summoner-name">${player.summonerName}</div>
                                    <img src="${player.tierImageUrl}" alt="티어 이미지" class="tier-icon" />
                                    <div class="summoner-tier">${player.tier}</div>
                                </div>
                            </div>
                        </td>
                        <td>${player.kills}/${player.deaths}/${player.assists}<br/><small>${player.kdaRatio}:1</small></td>
                        <td>
                            <div class="damage-wrapper">
                                <div class="damage-line">
                                    <span class="damage-value">${player.totalDamageDealtToChampions.toLocaleString()}</span>
                                    <div class="damage-bar red"><div class="bar" style="width:${damageDealt}%"></div></div>
                                </div>
                                <div class="damage-line">
                                    <span class="damage-value">${player.totalDamageTaken.toLocaleString()}</span>
                                    <div class="damage-bar gray"><div class="bar" style="width:${damageTaken}%"></div></div>
                                </div>
                            </div>
                        </td>
                        <td>설치: ${player.wardsPlaced}<br/>제거: ${player.wardsKilled}</td>
                        <td>총 CS: ${player.cs}<br/>분당: ${csPerMin}</td>
                        <td>${itemsHtml}</td>
                    </tr>
                `;
            };

            const blueHeader = `<tr class="team-summary ${isBlueWin ? 'team-blue' : 'team-red'}"><td colspan="6">${isBlueWin ? '승리 (블루팀)' : '패배 (블루팀)'}</td></tr>`;
            const redHeader = `<tr class="team-summary ${isBlueWin ? 'team-red' : 'team-blue'}"><td colspan="6">${isBlueWin ? '패배 (레드팀)' : '승리 (레드팀)'}</td></tr>`;
            const headerRow = '<tr><th>소환사</th><th>KDA</th><th>피해량</th><th>와드</th><th>CS</th><th>아이템</th></tr>';

            const blueRows = data.blueTeam.map(makeRow).join('');
            const redRows = data.redTeam.map(makeRow).join('');

            table.innerHTML = `
                ${blueHeader}
                ${headerRow}
                ${blueRows}
                <tr class="vs-row"><td colspan="6" class="vs-cell">VS</td></tr>
                ${redHeader}
                ${headerRow}
                ${redRows}
            `;

            detailBox.style.display = "block";
            detailBox.setAttribute("data-loaded", "true");
            // 화살표 회전
            element.classList.add("expanded");
        })
        .catch(err => {
            console.error("상세 정보 로딩 실패", err);
            detailBox.innerHTML = "<p>데이터 로딩 실패</p>";
        });
}

function closeMatchDetail(detailBox) {
    const matchCard = detailBox.previousElementSibling;
    detailBox.style.display = "none";
    // 화살표 회전 원래대로
    matchCard.classList.remove("expanded");
}

function initMatch() {
    const puuid = document.getElementById("puuid").value;

    fetch(`/match/init?puuid=${puuid}`)
        .then(res => res.text())
        .then(msg => alert(msg))
        .catch(err => alert("초기화 실패: " + err));
}

function updateMatch() {
    const puuid = document.getElementById("puuid").value;

    if (!puuid) {
        alert("소환사 정보가 없습니다.");
        return;
    }

    if (!confirm("전적을 최신 상태로 갱신하시겠습니까?")) return;

    fetch(`/match/update?puuid=${puuid}`)
        .then(res => res.text())
        .then(msg => {
            alert(msg || "전적 갱신 완료!");
            location.reload();
        })
        .catch(err => {
            alert("갱신 중 오류가 발생했습니다.");
            console.error(err);
        });
}

function cancelInit() {
    // 초기화 중지: 팝업 닫고, 초기화 중단 플래그 설정
    document.getElementById("patchPopup").style.display = "none";
    localStorage.setItem("initCanceled", "true");
    window.location.href = "/main";
}

function updateObjectiveBar(redValue, blueValue, redBarId, blueBarId, redTextId, blueTextId) {
    const total = redValue + blueValue || 1;
    const redPercent = (redValue / total) * 100;
    const bluePercent = (blueValue / total) * 100;

    document.getElementById(redBarId).style.width = `${redPercent}%`;
    document.getElementById(blueBarId).style.width = `${bluePercent}%`;

    document.getElementById(redTextId).innerText = redValue.toLocaleString();
    document.getElementById(blueTextId).innerText = blueValue.toLocaleString();
}
