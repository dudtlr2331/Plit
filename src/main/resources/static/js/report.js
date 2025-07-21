document.addEventListener("DOMContentLoaded", function () {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    const nicknameMeta = document.querySelector('meta[name="loginUserNickname"]');
    const nickname = nicknameMeta ? nicknameMeta.content : null;

    // 닉네임 세션에 저장
    if (nickname) {
        sessionStorage.setItem("loginUserNickname", nickname);
    } else {
        sessionStorage.removeItem("loginUserNickname");
    }

    // 초기 데이터 로딩
    if (nickname) {
        fetch("/api/blacklist/reported-list?nickname=" + encodeURIComponent(nickname))
            .then(res => res.json())
            .then(data => {
                // 서버 응답 예시: { myReports: ["유저1","유저2"], allReports: ["유저1","유저2","유저3"] }
                alreadyReportedByMe = data.myReports || [];
                alreadyReportedByOthers = data.allReports || [];

                // 화면의 모든 신고 버튼 상태 갱신
                updateReportButtons();
            })
            .catch(err => console.error("기존 신고 목록 불러오기 오류:", err));
    }

    // 버튼 상태 갱신
    function updateReportButtons() {
        document.querySelectorAll(".report-btn").forEach(btn => {
            const targetNickname = btn.getAttribute("data-nickname");

            // 내가 이미 신고한 유저라면 체크표시 + 비활성화
            if (alreadyReportedByMe.includes(targetNickname)) {
                btn.textContent = "✔";
                btn.disabled = true;
                btn.title = "이미 신고한 유저입니다.";
            } else {
                btn.textContent = "+";
                btn.disabled = false;
                btn.title = "신고하기";
            }
        });
    }

    // 팝업 열기 버튼
    const openBtn = document.getElementById("openReportPopup");
    if (openBtn) {
        openBtn.addEventListener("click", function () {
            const nick = sessionStorage.getItem("loginUserNickname");
            const reporterInput = document.getElementById("reporter");
            if (nick) {
                reporterInput.value = nick;
            } else {
                reporterInput.value = "로그인이 필요합니다";
            }
            document.getElementById("reportPopup").style.display = "block";
        });
    }

    // ===== 전역 노출 함수들 =====
    window.closePopup = function () {
        document.getElementById("reportPopup").style.display = "none";
    }

    window.submitReport = function () {
        const reporter = document.getElementById("reporter").value;
        const reportedPlayer = document.getElementById("reportedPlayer").value;
        const reason = document.getElementById("reportReason").value;

        // 이미 신고한 유저인지 확인
        if (alreadyReportedByMe.includes(reportedPlayer)) {
            alert("이미 신고한 유저입니다.");
            return;
        }

        if (!reportedPlayer || !reason.trim()) {
            alert("모든 항목을 입력해주세요.");
            return;
        }

        fetch("/api/blacklist/report", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [csrfHeader]: csrfToken
            },
            credentials: "same-origin",
            body: JSON.stringify({
                reporterNickname: reporter,
                reportedNickname: reportedPlayer,
                reason: reason
            })
        }).then(res => {
            if (res.ok) {
                alert("신고가 접수되었습니다.");
                // 성공했으니 프론트 리스트에도 추가
                alreadyReportedByMe.push(reportedPlayer);
                if (!alreadyReportedByOthers.includes(reportedPlayer)) {
                    alreadyReportedByOthers.push(reportedPlayer);
                }
                updateReportButtons();
                closePopup();
            } else {
                // 서버에서 보낸 응답을 그대로 찍어보기
                res.text().then(text => {
                    console.error("서버 응답 상태:", res.status);
                    console.error("서버 응답 본문:", text);

                    if (res.status === 409 || text.includes("이미 신고")) {
                        // 서버에서 중복 신고라는 메시지를 준 경우
                        alert("이미 신고한 유저입니다.");
                    } else {
                        alert("신고 처리 중 오류가 발생했습니다.");
                    }
                });
            }
        }).catch(err => {
            console.error("fetch 중 오류:", err);
            alert("신고 처리 중 네트워크 오류가 발생했습니다.");
        });
    }

    window.openReportPopupWith = function (nickname) {
        const reporter = sessionStorage.getItem("loginUserNickname");
        const reporterInput = document.getElementById("reporter");
        const reportedInput = document.getElementById("reportedPlayer");

        if (!reporter) {
            alert("로그인이 필요합니다.");
            return;
        }

        // 내가 이미 신고한 유저인지 확인
        if (alreadyReportedByMe.includes(nickname)) {
            alert("이미 신고한 유저입니다.");
            return;
        }

        reporterInput.value = reporter;
        reportedInput.value = nickname;

        document.getElementById("reportPopup").style.display = "block";
    }

    window.handleReportButtonClick = function (button) {
        const nickname = button.getAttribute("data-nickname");
        openReportPopupWith(nickname);
    }
});
