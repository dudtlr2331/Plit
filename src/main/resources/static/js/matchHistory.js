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
            const isBlueWin = data.blueTeam[0]?.win;
            const blueColorClass = isBlueWin ? 'blue-text' : 'red-text';
            const redColorClass = isBlueWin ? 'red-text' : 'blue-text';

            // const buildTeamObjectiveIcons = (obj, colorClass) => `
            //     <div><img src="/images/objective/tower.svg" class="objective-icon ${colorClass}" alt="타워"> ${obj.towerKills}</div>
            //     <div><img src="/images/objective/dragon.svg" class="objective-icon ${colorClass}" alt="드래곤"> ${obj.dragonKills}</div>
            //     <div><img src="/images/objective/baron.svg" class="objective-icon ${colorClass}" alt="바론"> ${obj.baronKills}</div>
            //     <div><img src="/images/objective/herald.svg" class="objective-icon ${colorClass}" alt="전령"> ${obj.heraldKills}</div>
            //     <div><img src="/images/objective/rift.svg" class="objective-icon ${colorClass}" alt="균열"> ${obj.riftKills}</div>
            // `;

            const buildTeamObjectiveIcons = (team, colorClass) => {
                const values = team === 'blue' ? {
                    tower: 1,
                    dragon: 1,
                    baron: 1,
                    herald: 1,
                    rift: 1
                } : {
                    tower: 8,
                    dragon: 1,
                    baron: 1,
                    herald: 1,
                    rift: 3
                };

                return `
                    <div><img src="/images/objective/tower.svg" class="objective-icon ${colorClass}" alt="타워"> ${values.tower}</div>
                    <div><img src="/images/objective/dragon.svg" class="objective-icon ${colorClass}" alt="드래곤"> ${values.dragon}</div>
                    <div><img src="/images/objective/baron.svg" class="objective-icon ${colorClass}" alt="바론"> ${values.baron}</div>
                    <div><img src="/images/objective/herald.svg" class="objective-icon ${colorClass}" alt="전령"> ${values.herald}</div>
                    <div><img src="/images/objective/rift.svg" class="objective-icon ${colorClass}" alt="균열"> ${values.rift}</div>
                `;
            };

            // 블루 = 왼쪽, 레드 = 오른쪽 (위치 고정)
            // detailBox.querySelector('.left-objectives').innerHTML = buildTeamObjectiveIcons(data.blueObjectives, blueColorClass);
            // detailBox.querySelector('.right-objectives').innerHTML = buildTeamObjectiveIcons(data.redObjectives, redColorClass);

            detailBox.querySelector('.left-objectives').innerHTML = buildTeamObjectiveIcons('red', redColorClass);
            detailBox.querySelector('.right-objectives').innerHTML = buildTeamObjectiveIcons('blue', blueColorClass);

            // 킬/골드는 그대로 유지
            updateObjectiveBar(data.redObjectives.totalKills, data.blueObjectives.totalKills, 'red-kill-bar', 'blue-kill-bar', 'red-kill-count', 'blue-kill-count');
            updateObjectiveBar(data.redObjectives.totalGold, data.blueObjectives.totalGold, 'red-gold-bar', 'blue-gold-bar', 'red-gold-count', 'blue-gold-count');



            // bar 업데이트
            updateObjectiveBar(data.redObjectives.totalKills, data.blueObjectives.totalKills, 'red-kill-bar', 'blue-kill-bar', 'red-kill-count', 'blue-kill-count');
            updateObjectiveBar(data.redObjectives.totalGold, data.blueObjectives.totalGold, 'red-gold-bar', 'blue-gold-bar', 'red-gold-count', 'blue-gold-count');

            const controlWards = [4, 2, 1, 3, 9, 3, 11, 0, 1, 2];
            const wardsPlaced  = [11, 7, 1, 14, 46, 5, 29, 7, 7, 6];
            const wardsKilled  = [2, 1, 5, 3, 12, 3, 14, 9, 4, 2];

            // 나머지 makeRow, 테이블 구성도 동일하게 유지
            const makeRow = (player, i) => {
                const damageDealt = (player.totalDamageDealtToChampions * 100 / data.totalMaxDamage).toFixed(1);
                const damageTaken = (player.totalDamageTaken * 100 / data.totalMaxDamage).toFixed(1);
                const csPerMin = player.csPerMin.toFixed(2);
                const itemsHtml = (player.itemImageUrls || []).map(img => img ? `<img src="${img}" class="item-icon">` : '').join('');

                const summonerNames = [
                    "찡클", "Shining Star", "파이리", "파닥몬", "원딜은버리고…",
                    "케케로인!", "안전불감증", "손가락인생", "내가 정점에 …", "Misfit"
                ];

                const spellIdToImage = {
                    Flash: "/images/spell/SummonerFlash.png",
                    Ignite: "/images/spell/SummonerDot.png",
                    Teleport: "/images/spell/SummonerTeleport.png",
                    Smite: "/images/spell/SummonerSmite.png",
                    Heal: "/images/spell/SummonerHeal.png",
                    Exhaust: "/images/spell/SummonerExhaust.png",
                    Barrier: "/images/spell/SummonerBarrier.png",
                    Ghost: "/images/spell/SummonerHaste.png",
                    Cleanse: "/images/spell/SummonerBoost.png",
                    Clarity: "/images/spell/SummonerMana.png",
                    Snowball: "/images/spell/SummonerSnowball.png",
                };

                const spell1Names = [
                    "Flash", "Flash", "Flash", "Flash", "Flash",
                    "Flash", "Flash", "Flash", "Flash", "Flash"
                ];

                const spell2Names = [
                    "Smite", "Ignite", "Ignite", "Teleport", "Ignite",
                    "Smite", "Ignite", "Ignite", "Heal", "Teleport"
                ];



                const runeIdToUrl = {
                    8005: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8005.png",
                    8008: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8008.png",
                    8009: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8009.png",
                    8010: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8010.png",
                    8014: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8014.png",
                    8017: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8017.png",
                    8021: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8021.png",
                    8105: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8105.png",
                    8106: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8106.png",
                    8112: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8112.png",
                    8126: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8126.png",
                    8128: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8128.png",
                    8135: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8135.png",
                    8137: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8137.png",
                    8139: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8139.png",
                    8140: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8140.png",
                    8141: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8141.png",
                    8143: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8143.png",
                    8210: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8210.png",
                    8214: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8214.png",
                    8224: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8224.png",
                    8226: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8226.png",
                    8229: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8229.png",
                    8230: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8230.png",
                    8232: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8232.png",
                    8233: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8233.png",
                    8234: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8234.png",
                    8236: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8236.png",
                    8237: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8237.png",
                    8242: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8242.png",
                    8275: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8275.png",
                    8299: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8299.png",
                    8304: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8304.png",
                    8306: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8306.png",
                    8313: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8313.png",
                    8316: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8316.png",
                    8321: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8321.png",
                    8345: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8345.png",
                    8347: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8347.png",
                    8351: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8351.png",
                    8352: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8352.png",
                    8360: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8360.png",
                    8369: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8369.png",
                    8401: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8401.png",
                    8410: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8410.png",
                    8429: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8429.png",
                    8437: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8437.png",
                    8439: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8439.png",
                    8444: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8444.png",
                    8446: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8446.png",
                    8451: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8451.png",
                    8453: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8453.png",
                    8463: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8463.png",
                    8465: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8465.png",
                    8473: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/8473.png",
                    9101: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/9101.png",
                    9103: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/9103.png",
                    9104: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/9104.png",
                    9105: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/9105.png",
                    9111: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/9111.png",
                    9923: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/9923.png",
                    7200: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/7200.png",
                    7201: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/7201.png",
                    7202: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/7202.png",
                    7203: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/7203.png",
                    7204: "https://d23maxd9bm8c6o.cloudfront.net/img/rune/7204.png"
                };

                const tierNames = [
                    "Platinum 3", "Bronze 3", "Master", "Bronze 1", "Master",
                    "Platinum 3", "Iron 1", "Emerald 3", "Diamond 2", "Platinum 2"
                ];

                const tierImageUrls = [
                    "/images/tier/PLATINUM.png",
                    "/images/tier/BRONZE.png",
                    "/images/tier/MASTER.png",
                    "/images/tier/BRONZE.png",
                    "/images/tier/MASTER.png",
                    "/images/tier/PLATINUM.png",
                    "/images/tier/IRON.png",
                    "/images/tier/EMERALD.png",
                    "/images/tier/DIAMOND.png",
                    "/images/tier/PLATINUM.png"
                ];

                const mainRune1Ids = [8005, 8005, 8005, 8005, 8005, 8005, 8005, 8005, 8005, 8005];
                const mainRune2Ids = [8105, 8126, 8139, 8214, 8304, 8126, 8210, 8229, 8230, 8237];


                return `
                    <tr>
                        <td>
                            <div class="player-summary-cell">
                                <div>
                                    <img src="${player.championImageUrl}" class="champion-icon" alt="챔피언"/>
                                </div>
                                <!-- 스펠 아이콘 2개 -->    
      <div class="spell-icons">
        <img src="${spellIdToImage[spell1Names[i]]}" class="spell-icon" alt="스펠1" />
        <img src="${spellIdToImage[spell2Names[i]]}" class="spell-icon" alt="스펠2" />
      </div>

      <!-- 룬 아이콘 2개 -->
      <div class="rune-icons">
        <img src="${runeIdToUrl[mainRune1Ids[i]]}" class="rune-icon" alt="메인룬1">
        <img src="${runeIdToUrl[mainRune2Ids[i]]}" class="rune-icon" alt="메인룬2">
      </div>
                                <!--
                                <div class="runes">
                                    <img src="${player.mainRune1Url}" class="rune-icon" alt="메인 룬"/>
                                    <img src="${player.mainRune2Url}" class="rune-icon" alt="메인 룬"/>
                                    <img src="${player.statRune1Url}" class="rune-icon" alt="스탯 룬"/>
                                    <img src="${player.statRune2Url}" class="rune-icon" alt="스탯 룬"/>
                                </div>
                                -->
                                <div class="summoner-info">
                                    <!-- <div class="summoner-name">${player.summonerName}</div>
                                    <img src="${player.tierImageUrl}" alt="티어 이미지" class="tier-icon" />
                                    <div class="summoner-tier">${player.tier}</div> -->
                                    <div class="summoner-name">${summonerNames[i]}</div>
                                    <div class="summoner-tier-box">
                                        <img src="${tierImageUrls[i]}" alt="티어 이미지" class="tier-icon" />
                                        <div class="summoner-tier">${tierNames[i]}</div>
                                    </div>
                                </div>
                            </div>
                        </td>
                        <!-- <td>${player.kills}/${player.deaths}/${player.assists}<br/><small>${player.kdaRatio}:1</small></td> -->
                        <td>
                            ${player.kills}/${player.deaths}/${player.assists}<br/>
                            <small>${player.deaths === 0 ? (player.kills + player.assists) + ":1" : ((player.kills + player.assists) / player.deaths).toFixed(1) + ":1"}</small>
                        </td>
                        <td>
                            <div class="damage-wrapper">
                                <div class="damage-line">
                                    <span class="damage-value">⚔️${player.totalDamageDealtToChampions.toLocaleString()}</span>
                                    <div class="damage-bar red"><div class="bar" style="width:${damageDealt}%"></div></div>
                                </div>
                                <div class="damage-line">
                                    <span class="damage-value">🛡️${player.totalDamageTaken.toLocaleString()}</span>
                                    <div class="damage-bar gray"><div class="bar" style="width:${damageTaken}%"></div></div>
                                </div>
                            </div>
                        </td>
<!--                        <td>설치: ${player.wardsPlaced}<br/>제거: ${player.wardsKilled}</td>-->
                        <td>
                            제어: ${controlWards[i]}<br/>
                            설치: ${wardsPlaced[i]} / 제거: ${wardsKilled[i]}
                        </td>
                        <td>총 CS: ${player.cs}<br/>분당: ${csPerMin}</td>
                        <td>${itemsHtml}</td>
                    </tr>
                `;
            };

            const blueHeader = `<tr class="team-summary ${isBlueWin ? 'team-blue' : 'team-red'}"><td colspan="6">${isBlueWin ? '승리 (블루팀)' : '패배 (블루팀)'}</td></tr>`;
            const redHeader = `<tr class="team-summary ${isBlueWin ? 'team-red' : 'team-blue'}"><td colspan="6">${isBlueWin ? '패배 (레드팀)' : '승리 (레드팀)'}</td></tr>`;
            const headerRow = '<tr><th>소환사</th><th>KDA</th><th>피해량</th><th>와드</th><th>CS</th><th>아이템</th></tr>';

            // const blueRows = data.blueTeam.map(makeRow).join('');
            // const redRows = data.redTeam.map(makeRow).join('');
            const blueRows = data.blueTeam.map((player, i) => makeRow(player, i)).join('');
            const redRows  = data.redTeam.map((player, i) => makeRow(player, i + 5)).join('');

            const blueTable = detailBox.querySelector(".blue-table");
            const redTable  = detailBox.querySelector(".red-table");

            blueTable.innerHTML = `
                ${blueHeader}
                ${headerRow}
                ${blueRows}
            `;

            redTable.innerHTML = `
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
window.loadMatchDetail = loadMatchDetail;

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

const searchInput = document.getElementById("searchInput");
const autocompleteList = document.getElementById("autocompleteList");
const searchButton = document.getElementById("searchButton");
searchInput.addEventListener("input", async function () {
    const keyword = searchInput.value.trim();
    if (keyword.length < 2) {
        autocompleteList.innerHTML = "";
        return;
    }
    const res = await fetch(`/match/autocomplete?keyword=${encodeURIComponent(keyword)}`);
    const suggestions = await res.json();
    autocompleteList.innerHTML = suggestions.map(s => `
        <div class="autocomplete-item" onclick="selectRiotId('${s}')">${s}</div>
    `).join("");
});
searchInput.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
        executeSearch();
    }
});
searchButton.addEventListener("click", executeSearch);
function executeSearch() {
    const inputValue = searchInput.value.trim();
    const [gameName, tagLine] = inputValue.split("#");
    if (!gameName || !tagLine) {
        alert("Riot ID 형식은 '닉네임#태그'입니다.");
        return;
    }
    window.location.href = `/match?gameName=${encodeURIComponent(gameName)}&tagLine=${encodeURIComponent(tagLine)}`;
}
function selectRiotId(riotId) {
    const [gameName, tagLine] = riotId.split("#");
    window.location.href = `/match?gameName=${encodeURIComponent(gameName)}&tagLine=${encodeURIComponent(tagLine)}`;
}

document.getElementById("matchSearchButton").addEventListener("click", function () {
    const input = document.getElementById("matchSearchInput").value.trim();
    const [gameName, tagLine] = input.split("#");

    if (!gameName || !tagLine) {
        alert("Riot ID 형식은 '닉네임#태그'입니다.");
        return;
    }

    window.location.href = `/match?gameName=${encodeURIComponent(gameName)}&tagLine=${encodeURIComponent(tagLine)}`;
});