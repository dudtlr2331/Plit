console.log("JS loaded");
document.addEventListener('DOMContentLoaded', () => {
    const updateBtn = document.getElementById('update-btn');
    const puuid = "[[${summoner.puuid}]]";

    updateBtn.addEventListener('click', () => {
        if (!puuid) {
            alert('소환사 정보가 없습니다.');
            return;
        }

        if (!confirm('전적을 최신 상태로 갱신하시겠습니까?')) return;

        fetch(`/match/update?puuid=${puuid}`, {
            method: 'POST'
        })
        .then(res => {
            if (!res.ok) throw new Error('서버 오류');
            return res.text(); // 서버에서 텍스트 응답 반환한다고 가정
        })
        .then(msg => {
            alert(msg || '전적 갱신 완료!');
            location.reload();
        })
        .catch(err => {
            alert('갱신 중 오류가 발생했습니다.');
            console.error(err);
        });
    });
});

// 왼쪽 파넬 - 랭크 모드별 선호챔피언 정보
document.addEventListener("DOMContentLoaded", function () {

    const updateBtn = document.getElementById('update-btn');
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
        btn.addEventListener("click", () => { activateTab(btn.dataset.mode));
        });
    });

    activateTab("overall"); // 초기 설정

});

// 더보기 버튼
function showMoreChampions(button) {
    const parent = button.closest('.champion-tab-content');
    const hiddenRows = parent.querySelectorAll('.hidden-champ');

    hiddenRows.forEach(row => {
    row.classList.remove('hidden-champ');
    });

    button.style.display = 'none';

}

// 매치 요약 박스 클릭시 실행( 상세 페이지 )  (element) 는 match-summary-box
function loadMatchDetail(element) {
    const matchId = element.getAttribute("data-match-id");
    const puuid = element.getAttribute("data-puuid");
    const detailBox = element.nextElementSibling;

    if (detailBox.getAttribute("data-loaded") === "true") {
        detailBox.style.display = detailBox.style.display === "none" ? "block" : "none";
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
                <div><img src="/images/objective/tower.svg" class="objective-icon ${colorClass}"> ${obj.towerKills}</div>
                <div><img src="/images/objective/dragon.svg" class="objective-icon ${colorClass}"> ${obj.dragonKills}</div>
                <div><img src="/images/objective/baron.svg" class="objective-icon ${colorClass}"> ${obj.baronKills}</div>
                <div><img src="/images/objective/herald.svg" class="objective-icon ${colorClass}"> ${obj.heraldKills}</div>
                <div><img src="/images/objective/rift.svg" class="objective-icon ${colorClass}"> ${obj.riftKills}</div>
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
                const itemsHtml = player.itemImageUrls.map(img => img ? `<img src="${img}" class="item-icon">` : '').join('');

                return `
                    <tr>
                        <td>
                            <div class="player-summary-cell">
                                <img src="${player.profileIconUrl}" class="profile-icon" />
                                <img src="${player.championImageUrl}" class="champion-icon" />
                                <div class="runes">
                                    <img src="${player.mainRune1Url}" class="rune-icon" />
                                    <img src="${player.mainRune2Url}" class="rune-icon" />
                                    <img src="${player.statRune1Url}" class="rune-icon" />
                                    <img src="${player.statRune2Url}" class="rune-icon" />
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
        })
        .catch(err => {
            console.error("상세 정보 로딩 실패", err);
            detailBox.innerHTML = "<p>데이터 로딩 실패</p>";
        });
}

// 매치 요약 5개씩 표시 + 더보기 버튼
document.addEventListener("DOMContentLoaded", function () {
    const matchCards = document.querySelectorAll('.match-block');
    const loadMoreButton = document.getElementById('match-load-more-button');
    let visibleCount = 5;
    const step = 5;

    function updateVisibleMatches() {
        matchCards.forEach((card, index) => {
            card.style.display = index < visibleCount ? 'block' : 'none';
        });

        if (visibleCount >= matchCards.length && loadMoreButton) {
            loadMoreButton.style.display = 'none';
        }
    }

    if (loadMoreButton) {
        loadMoreButton.addEventListener('click', function () {
            visibleCount += step;
            updateVisibleMatches();
        });
    }

    // 처음 상태 반영
    updateVisibleMatches();
});

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