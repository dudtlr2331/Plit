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

        console.log("전송 값:", { reporter, reportedPlayer, reason });

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
                closePopup();
            } else {
                // 서버에서 보낸 응답을 그대로 찍어보기
                res.text().then(text => {
                    console.error("서버 응답 상태:", res.status);
                    console.error("서버 응답 본문:", text);
                });
                alert("신고 처리 중 오류가 발생했습니다.");
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

        reporterInput.value = reporter;
        reportedInput.value = nickname;

        document.getElementById("reportPopup").style.display = "block";
    }

    window.handleReportButtonClick = function (button) {
        const nickname = button.getAttribute("data-nickname");
        openReportPopupWith(nickname);
    }
});
