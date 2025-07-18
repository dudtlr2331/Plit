document.addEventListener("DOMContentLoaded", () => {
    const bodyClass = document.body?.classList;

    // showTab이 정의 확인
    const canShowTab = typeof showTab === 'function';
    const canValidateForm = typeof validateForm === 'function';

    // 클랜 상세 페이지 기능
    if (bodyClass?.contains("clan-detail")) {
        if (!canShowTab) {
            console.error("showTab 함수가 정의되지 않았습니다.");
            return;
        }

        const justEdited = sessionStorage.getItem("justEdited");
        const handledTab = sessionStorage.getItem("justHandledTab");

        if (justEdited === "true") {
            showTab("members");
            sessionStorage.removeItem("justEdited");
        } else if (handledTab) {
            showTab(handledTab);
            sessionStorage.removeItem("justHandledTab");
        } else {
            showTab("intro");
        }
    }

    // 클랜 리스트 페이지 기능
    if (bodyClass?.contains("clan-list")) {
        const form = document.getElementById("clanForm");

        if (form) {
            if (!canValidateForm) {
                console.error("validateForm 함수가 정의되지 않았습니다.");
                return;
            }

            form.addEventListener("submit", async function (event) {
                event.preventDefault();
                const isValid = await validateForm();
                if (isValid) form.submit();
            });
        }
    }
});

// 공통 유틸
function toggleChatList() {
    try {
        alert('채팅 리스트 열기 구현 필요');
    } catch (e) {
        console.error('채팅 리스트 열기 알림 실패:', e);
    }
}

// 클랜 리스트 페이지 함수
function openModal() {
    const modal = document.getElementById("clanModal");
    if (!modal) {
        console.warn("clanModal 요소를 찾을 수 없습니다.");
        return;
    }
    modal.style.display = "block";
}

function closeModal() {
    const modal = document.getElementById("clanModal");
    if (!modal) {
        console.warn("clanModal 요소를 찾을 수 없습니다.");
        return;
    }
    modal.style.display = "none";
}

function validateImageRatio(event) {
    const file = event.target?.files?.[0];
    if (!file) {
        console.warn("파일이 선택되지 않았습니다.");
        return;
    }

    const imagePreview = document.getElementById("imagePreview");
    if (!imagePreview) {
        console.warn("imagePreview 요소를 찾을 수 없습니다.");
        return;
    }

    const img = new Image();
    const reader = new FileReader();

    reader.onload = e => img.src = e.target.result;

    img.onload = () => {
        const ratio = img.width / img.height;

        if (ratio < 0.5 || ratio > 2.5) {
            alert("4:3 비율 이미지로 업로드해주세요!");
            event.target.value = "";
            imagePreview.src = "";
        } else {
            imagePreview.src = img.src;
        }
    };

    reader.onerror = () => {
        alert("이미지 파일을 읽는 도중 오류가 발생했습니다.");
    };

    reader.readAsDataURL(file);
}

function checkClanName(name) {
    const msgDiv = document.getElementById("clanNameMessage");
    if (!msgDiv) {
        console.warn("clanNameMessage 요소가 없습니다.");
        return;
    }

    const trimmedName = name?.trim() || "";
    if (trimmedName.length < 2) {
        msgDiv.textContent = "클랜 이름은 2자 이상 입력해주세요!";
        msgDiv.style.color = "gray";
        return;
    }

    fetch(`/clan/check-name?name=${encodeURIComponent(trimmedName)}`)
        .then(res => {
            if (!res.ok) throw new Error("서버 응답 오류");
            return res.json();
        })
        .then(data => {
            msgDiv.textContent = data ? "사용 가능!" : "이미 존재하는 이름입니다.";
            msgDiv.style.color = data ? "green" : "red";
        })
        .catch((err) => {
            console.error("클랜 이름 검사 실패:", err);
            msgDiv.textContent = "서버 오류!";
            msgDiv.style.color = "orange";
        });
}

async function validateForm() {
    const nameInput = document.querySelector("input[name='name']");
    const msgDiv = document.getElementById("clanNameMessage");

    if (!nameInput || !msgDiv) {
        console.error("필수 요소(nameInput 또는 msgDiv)를 찾을 수 없습니다.");
        alert("폼 요소를 찾을 수 없습니다. 새로고침 후 다시 시도해주세요.");
        return false;
    }

    const name = nameInput.value.trim();

    if (name.length < 2) {
        msgDiv.textContent = "클랜 이름은 2자 이상 입력해주세요!";
        msgDiv.style.color = "gray";
        nameInput.focus();
        return false;
    }

    try {
        const res = await fetch(`/clan/check-name?name=${encodeURIComponent(name)}`);
        if (!res.ok) throw new Error(`서버 응답 실패: ${res.status}`);

        const isAvailable = await res.json();
        if (!isAvailable) {
            msgDiv.textContent = "이미 존재하는 클랜 이름입니다.";
            msgDiv.style.color = "red";
            nameInput.focus();
            return false;
        }

        msgDiv.textContent = "사용 가능한 이름입니다.";
        msgDiv.style.color = "green";
        return true;

    } catch (err) {
        console.error("클랜 이름 중복 확인 실패:", err);
        msgDiv.textContent = "서버 오류 발생. 다시 시도해주세요.";
        msgDiv.style.color = "orange";
        return false;
    }
}


// 클랜 상세페이지 함수
function showTab(tab) {
    try {
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.tab-section').forEach(section => section.classList.add('hidden'));

        const targetTabBtn = document.querySelector(`.tab-btn[onclick*='${tab}']`);
        if (targetTabBtn) {
            targetTabBtn.classList.add('active');
        } else {
            console.warn(`탭 버튼을 찾을 수 없습니다: ${tab}`);
        }

        const targetSection = document.getElementById(tab);
        if (targetSection) {
            targetSection.classList.remove('hidden');
        } else {
            console.warn(`탭 섹션을 찾을 수 없습니다: ${tab}`);
        }

        const actionButtons = document.getElementById('editButtons');
        if (actionButtons) {
            actionButtons.style.display = (tab === 'intro') ? 'block' : 'none';
        }

        const memberLabel = document.getElementById('memberCountLabel');
        if (memberLabel) {
            memberLabel.style.display = (tab === 'members') ? 'flex' : 'none';
        }

        if (tab === 'members' && typeof loadMemberList === 'function') {
            try {
                loadMemberList();
            } catch (err) {
                console.error("loadMemberList 실행 중 오류:", err);
            }
        }

    } catch (err) {
        console.error("showTab 함수 실행 중 예외 발생:", err);
        alert("탭을 전환하는 중 오류가 발생했습니다. 새로고침 후 다시 시도해주세요.");
    }
}

function openEditModal() {
    const modal = document.getElementById("editClanModal");

    if (!modal) {
        console.error("editClanModal 요소를 찾을 수 없습니다.");
        alert("클랜 수정 창을 열 수 없습니다. 관리자에게 문의해주세요.");
        return;
    }

    modal.style.display = "flex";
    modal.style.justifyContent = "center";
    modal.style.alignItems = "center";
}

function closeEditModal() {
    const modal = document.getElementById("editClanModal");

    if (!modal) {
        console.error("editClanModal 요소를 찾을 수 없습니다.");
        alert("클랜 수정 창을 닫을 수 없습니다. 관리자에게 문의해주세요.");
        return;
    }

    modal.style.display = "none";
}

function openEditMemberModal(position) {
    const modal = document.getElementById("editMemberModal");

    if (!modal) {
        console.error("editMemberModal 요소를 찾을 수 없습니다.");
        alert("멤버 수정 창을 열 수 없습니다. 관리자에게 문의해주세요.");
        return;
    }

    const buttons = document.querySelectorAll(".position-btn");
    if (buttons.length === 0) {
        console.warn("position-btn 요소가 없습니다.");
    }

    buttons.forEach(btn => btn.classList.remove("active"));

    if (position) {
        const btn = document.querySelector(`.position-btn[data-position="${position}"]`);
        if (btn) {
            btn.classList.add("active");
        } else {
            console.warn(`'${position}' 포지션 버튼을 찾을 수 없습니다.`);
        }
    }

    modal.style.display = "flex";
    modal.style.justifyContent = "center";
    modal.style.alignItems = "center";

    selectedPosition = position;
}

function openEditMemberModalByAttr(button) {
    if (!button) {
        console.error("버튼 요소가 null입니다.");
        alert("잘못된 동작입니다. 새로고침 후 다시 시도해주세요.");
        return;
    }

    const position = button.getAttribute('data-main-position');
    if (!position) {
        console.warn("data-main-position 속성이 없습니다.");
        alert("선택된 포지션 정보를 찾을 수 없습니다.");
        return;
    }

    openEditMemberModal(position);
}

function closeEditMemberModal() {
    const modal = document.getElementById("editMemberModal");
    if (!modal) {
        console.warn("editMemberModal 요소를 찾을 수 없습니다.");
        return;
    }

    modal.style.display = "none";

    selectedPosition = null;
    document.querySelectorAll(".position-btn").forEach(btn => btn.classList.remove("active"));
}

let selectedPosition = null;

function selectPosition(element) {
    if (!element || !element.classList.contains("position-btn")) {
        console.warn("유효하지 않은 포지션 버튼입니다.");
        return;
    }

    const position = element.getAttribute("data-position");
    if (!position) {
        console.warn("data-position 속성이 없습니다.");
        return;
    }

    document.querySelectorAll(".position-btn").forEach(btn => btn.classList.remove("active"));
    element.classList.add("active");
    selectedPosition = position;
}

function submitMemberEdit() {
    const introInput = document.getElementById("memberIntro");
    const clanIdInput = document.getElementById("clanId");

    if (!introInput || !clanIdInput) {
        alert("입력 요소를 찾을 수 없습니다. 페이지를 새로고침 해주세요.");
        return;
    }

    const intro = introInput.value.trim();
    const clanId = parseInt(clanIdInput.value);
    const position = selectedPosition;

    if (!position) {
        alert("주 포지션을 선택해주세요.");
        return;
    }

    if (isNaN(clanId)) {
        alert("유효하지 않은 클랜 ID입니다.");
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");

    if (!csrfToken || !csrfHeader) {
        alert("보안 토큰이 누락되었습니다. 다시 로그인 해주세요.");
        return;
    }

    const data = { clanId, intro, position };

    fetch('/clan/member/update', {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(data)
    })
        .then(async res => {
            const text = await res.text();
            if (res.ok) {
                alert("멤버 수정 완료!");
                closeEditMemberModal();
                sessionStorage.setItem("justEdited", "true");
                location.reload();
            } else {
                alert("수정 실패: " + text);
            }
        })
        .catch(err => {
            console.error("요청 오류:", err);
            alert("서버와의 통신 중 문제가 발생했습니다.");
        });
}

function openEditModal(button) {
    if (!button) {
        alert("잘못된 호출입니다. 버튼 요소가 없습니다.");
        return;
    }

    const modal = document.getElementById("editClanModal");
    if (!modal) {
        alert("수정 모달을 찾을 수 없습니다.");
        return;
    }

    const minTierSelect = modal.querySelector("select[name='minTier']");
    if (!minTierSelect) {
        alert("티어 선택 요소를 찾을 수 없습니다.");
        return;
    }

    const tierValue = button.dataset.minTier;
    if (!tierValue) {
        alert("선택된 티어 값이 없습니다.");
        return;
    }

    minTierSelect.value = tierValue;
    modal.style.display = "flex";
    modal.style.justifyContent = "center";
    modal.style.alignItems = "center";
}

function openJoinClanModal() {
    const modal = document.getElementById("joinClanModal");

    if (!modal) {
        console.error("joinClanModal 요소를 찾을 수 없습니다!");
        alert("클랜 가입창을 불러올 수 없습니다. 페이지를 새로고침해주세요.");
        return;
    }

    modal.style.display = "flex";
    modal.style.justifyContent = "center";
    modal.style.alignItems = "center";
}

function closeJoinClanModal() {
    const modal = document.getElementById("joinClanModal");

    if (!modal) {
        console.error("joinClanModal 요소가 존재하지 않습니다.");
        return;
    }

    modal.style.display = "none";
}

function submitJoinRequest() {
    const modal = document.getElementById("joinClanModal");
    const intro = modal?.querySelector("textarea")?.value || "";
    const clanIdInput = document.getElementById("joinClanId");

    if (!clanIdInput) {
        alert("클랜 ID를 찾을 수 없습니다. 페이지를 새로고침 해주세요.");
        return;
    }

    const clanId = parseInt(clanIdInput.value);
    const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

    if (!positionCheck(selectedPosition)) return;

    const requestBody = { clanId, position: selectedPosition, intro };

    fetch("/clan/join", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {})
        },
        body: JSON.stringify(requestBody)
    })
        .then(async res => {
            const text = await res.text();
            if (res.ok) {
                alert("가입 신청 완료!");
                closeJoinClanModal();
                setTimeout(() => location.reload(), 100);
            } else {
                alert(`가입 실패: ${text || "서버 오류입니다."}`);
            }
        })
        .catch(err => {
            console.error("네트워크 오류:", err);
            alert("서버와 연결에 실패했습니다. 인터넷 연결을 확인해주세요.");
        });
}

// 포지션 예외처리 함수 분리
function positionCheck(pos) {
    if (!pos) {
        alert("주 포지션을 선택해주세요!");
        return false;
    }
    return true;
}

function approveRequest(clanId, userId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    if (!clanId || !userId) {
        alert("승인할 유저 정보가 올바르지 않습니다.");
        return;
    }

    fetch(`/clan/${clanId}/approve/${userId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {})
        }
    })
        .then(async res => {
            const msg = await res.text();
            if (res.ok) {
                alert(msg || "승인이 완료되었습니다.");
                sessionStorage.setItem("justHandledTab", "pending");
                location.reload();
            } else {
                alert("승인 실패: " + (msg || "서버 오류입니다."));
            }
        })
        .catch(err => {
            console.error("네트워크 오류:", err);
            alert("요청 중 문제가 발생했습니다: " + err.message);
        });
}

function rejectRequest(clanId, userId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    if (!clanId || !userId) {
        alert("거절할 유저 정보가 올바르지 않습니다.");
        return;
    }

    fetch(`/clan/${clanId}/reject/${userId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {})
        }
    })
        .then(async res => {
            const msg = await res.text();
            if (res.ok) {
                alert(msg || "거절이 완료되었습니다.");
                sessionStorage.setItem("justHandledTab", "pending");
                location.reload();
            } else {
                alert("거절 실패: " + (msg || "서버 오류입니다."));
            }
        })
        .catch(err => {
            console.error("네트워크 오류:", err);
            alert("요청 중 문제가 발생했습니다: " + err.message);
        });
}

function delegateLeader(clanId, userId) {
    if (!confirm("정말 이 멤버에게 리더를 위임하시겠습니까?")) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    if (!clanId || !userId) {
        alert("리더 위임에 필요한 정보가 부족합니다.");
        return;
    }

    fetch(`/clan/${clanId}/delegate/${userId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {})
        }
    })
        .then(async res => {
            const msg = await res.text();
            if (res.ok) {
                alert(msg || "리더 위임 완료");
                sessionStorage.setItem("justHandledTab", "members");
                location.reload();
            } else {
                alert("리더 위임 실패: " + (msg || "서버 오류"));
            }
        })
        .catch(err => {
            console.error("네트워크 오류:", err);
            alert("요청 중 문제가 발생했습니다: " + err.message);
        });
}

function kickMember(clanId, memberId) {
    if (!confirm("정말 이 멤버를 추방하시겠습니까?")) return;

    if (!clanId || !memberId) {
        alert("필수 정보가 부족하여 멤버를 추방할 수 없습니다.");
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');

    fetch(`/clan/${clanId}/kick/${memberId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfToken ? { "X-CSRF-TOKEN": csrfToken } : {})
        }
    })
        .then(async res => {
            const msg = await res.text();
            if (res.ok) {
                alert(msg || "멤버가 성공적으로 추방되었습니다.");
                sessionStorage.setItem("justHandledTab", "members");
                location.reload();
            } else {
                alert("추방 실패: " + (msg || "서버 오류"));
            }
        })
        .catch(err => {
            console.error("네트워크 오류:", err);
            alert("요청 처리 중 오류가 발생했습니다: " + err.message);
        });
}

function confirmLeaveClan(clanId) {
    if (!confirm("정말 탈퇴하시겠습니까?")) return;

    if (!clanId) {
        alert("클랜 정보가 부족합니다.");
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {
        "Content-Type": "application/json"
    };
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(`/clan/${clanId}/leave`, {
        method: "POST",
        headers
    })
        .then(async res => {
            const msg = await res.text();
            if (res.ok) {
                alert(msg || "탈퇴가 완료되었습니다.");
                window.location.href = `/clan/${clanId}`;
            } else {
                alert("탈퇴 실패: " + (msg || "서버 오류"));
            }
        })
        .catch(err => {
            console.error("네트워크 오류:", err);
            alert("요청 처리 중 오류가 발생했습니다: " + err.message);
        });
}

function confirmDelete(button) {
    const form = button.closest('form');

    if (!form) {
        alert("삭제 폼을 찾을 수 없습니다.");
        console.error("deleteForm 요소가 존재하지 않음");
        return;
    }

    if (confirm("정말로 클랜을 삭제하시겠습니까? 삭제 후 복구는 불가능합니다.")) {
        try {
            form.submit();
        } catch (e) {
            console.error("폼 제출 중 오류:", e);
            alert("클랜 삭제 요청 중 오류가 발생했습니다.");
        }
    }
}

function handleJoinClick() {
    try {
        const isAuthenticated = document.getElementById("isAuthenticated")?.value === 'true';
        const userRole = document.getElementById("userRole")?.value;
        const isAdmin = document.getElementById("isAdmin")?.value === 'true';

        if (!isAuthenticated) {
            alert("로그인이 필요합니다!");
            window.location.href = "/login";
            return;
        }

        if (userRole === 'GUEST' || isAdmin) {
            openJoinClanModal();
        } else {
            alert("이미 클랜에 가입된 사용자입니다!");
        }
    } catch (err) {
        console.error("클랜 가입 처리 중 오류 발생:", err);
        alert("예상치 못한 오류가 발생했습니다.");
    }
}

window.addEventListener('pageshow', (e) => {
    try {
        const navType = performance.getEntriesByType('navigation')[0]?.type;
        if (e.persisted || navType === 'back_forward') {
            location.reload();
        }
    } catch (err) {
        console.warn("페이지 show 처리 중 오류:", err);
    }
});

function updateSearchTier(tier) {
    try {
        const icon = document.getElementById('searchTierIcon');
        if (!icon) return;

        if (!tier || tier === 'NONE') {
            icon.style.display = 'none';
        } else {
            icon.style.display = 'inline-block';
            icon.src = `/images/tier/${tier}.png`;
        }
    } catch (err) {
        console.error("티어 아이콘 업데이트 오류:", err);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const tierValue = document.querySelector("select[name='tier']")?.value;
    if (tierValue) updateSearchTier(tierValue);
});