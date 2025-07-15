document.addEventListener("DOMContentLoaded", () => {
    const bodyClass = document.body.classList;

    // 클랜 상세 페이지 기능
    if (bodyClass.contains("clan-detail")) {
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
    if (bodyClass.contains("clan-list")) {
        const form = document.getElementById("clanForm");
        if (form) {
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
    alert('채팅 리스트 열기 구현 필요');
}

// 클랜 리스트 페이지 함수
function openModal() {
    document.getElementById("clanModal").style.display = "block";
}
function closeModal() {
    document.getElementById("clanModal").style.display = "none";
}

function validateImageRatio(event) {
    const file = event.target.files[0];
    if (!file) return;

    const img = new Image();
    const reader = new FileReader();

    reader.onload = e => img.src = e.target.result;
    img.onload = () => {
        const ratio = img.width / img.height;
        if (ratio < 0.5 || ratio > 2.5) {
            alert("4:3 비율 이미지로 업로드해주세요!");
            event.target.value = "";
            document.getElementById("imagePreview").src = "";
        } else {
            document.getElementById("imagePreview").src = img.src;
        }
    };

    reader.readAsDataURL(file);
}

function checkClanName(name) {
    const msgDiv = document.getElementById("clanNameMessage");

    if (!name || name.length < 2) {
        msgDiv.textContent = "클랜 이름은 2자 이상 입력해주세요!";
        msgDiv.style.color = "gray";
        return;
    }

    fetch(`/clan/check-name?name=${encodeURIComponent(name)}`)
        .then(res => res.json())
        .then(data => {
            msgDiv.textContent = data ? "사용 가능!" : "이미 존재하는 이름입니다.";
            msgDiv.style.color = data ? "green" : "red";
        })
        .catch(() => {
            msgDiv.textContent = "서버 오류!";
            msgDiv.style.color = "orange";
        });
}

async function validateForm() {
    const nameInput = document.querySelector("input[name='name']");
    const name = nameInput.value.trim();
    const msgDiv = document.getElementById("clanNameMessage");

    if (name.length < 2) {
        alert("클랜 이름은 2자 이상 입력해주세요!");
        nameInput.focus();
        return false;
    }

    try {
        const res = await fetch(`/clan/check-name?name=${encodeURIComponent(name)}`);
        const isAvailable = await res.json();
        if (!isAvailable) {
            alert("이미 존재하는 클랜 이름입니다.");
            nameInput.focus();
            return false;
        }
        return true;
    } catch {
        alert("서버 오류 발생!");
        return false;
    }
}


// 클랜 상세페이지 함수
function showTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-section').forEach(section => section.classList.add('hidden'));

    const targetTabBtn = document.querySelector(`.tab-btn[onclick*='${tab}']`);
    if (targetTabBtn) targetTabBtn.classList.add('active');

    const targetSection = document.getElementById(tab);
    if (targetSection) targetSection.classList.remove('hidden');

    const actionButtons = document.getElementById('editButtons');
    if (actionButtons) actionButtons.style.display = (tab === 'intro') ? 'block' : 'none';

    const memberLabel = document.getElementById('memberCountLabel');
    if (memberLabel) memberLabel.style.display = (tab === 'members') ? 'flex' : 'none';

    if (tab === 'members' && typeof loadMemberList === 'function') {
        loadMemberList();
    }
}

function openEditModal() {
    const modal = document.getElementById("editClanModal");
    modal.style.display = "flex";
    modal.style.justifyContent = "center";
    modal.style.alignItems = "center";
}

function closeEditModal() {
    document.getElementById("editClanModal").style.display = "none";
}

function openEditMemberModal(position) {
    const modal = document.getElementById("editMemberModal");
    document.querySelectorAll(".position-btn").forEach(btn => btn.classList.remove("active"));

    if (position) {
        const btn = document.querySelector(`.position-btn[data-position="${position}"]`);
        if (btn) btn.classList.add("active");
    }

    modal.style.display = "flex";
    modal.style.justifyContent = "center";
    modal.style.alignItems = "center";

    selectedPosition = position;
}

function openEditMemberModalByAttr(button) {
    const position = button.getAttribute('data-main-position');
    openEditMemberModal(position);
}

function closeEditMemberModal() {
    document.getElementById("editMemberModal").style.display = "none";
}

let selectedPosition = null;

function selectPosition(element) {
    document.querySelectorAll(".position-btn").forEach(btn => btn.classList.remove("active"));
    element.classList.add("active");
    selectedPosition = element.getAttribute("data-position");
}

function submitMemberEdit() {
    const intro = document.getElementById("memberIntro").value;
    const position = selectedPosition;

    if (!position) {
        alert("주 포지션을 선택해주세요.");
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute("content");

    const data = {
        clanId: parseInt(document.getElementById("clanId").value),
        intro,
        mainPosition: position
    };

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
        .catch(err => alert("요청 실패: " + err));
}

function openJoinClanModal() {
    const modal = document.getElementById("joinClanModal");
    modal.style.display = "flex";
    modal.style.justifyContent = "center";
    modal.style.alignItems = "center";
}

function closeJoinClanModal() {
    document.getElementById("joinClanModal").style.display = "none";
}

function submitJoinRequest() {
    const intro = document.querySelector("#joinClanModal textarea")?.value || "";
    const position = selectedPosition;
    const clanId = parseInt(document.getElementById("joinClanId").value);
    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

    if (!position) {
        alert("주 포지션을 선택해주세요!");
        return;
    }

    const requestBody = { clanId, mainPosition: position, intro };

    fetch("/clan/join", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(requestBody)
    })
        .then(async res => {
            if (res.ok) {
                alert("가입 신청 완료!");
                closeJoinClanModal();
                setTimeout(() => location.reload(), 100);
            } else {
                const msg = await res.text();
                alert("가입 실패: " + msg);
            }
        })
        .catch(err => alert("네트워크 오류: " + err));
}

function approveRequest(clanId, userId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/clan/${clanId}/approve/${userId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        }
    })
        .then(res => res.ok ? res.text() : res.text().then(msg => { throw new Error(msg); }))
        .then(message => {
            alert(message);
            sessionStorage.setItem("justHandledTab", "pending");
            location.reload();
        })
        .catch(err => alert("에러: " + err.message));
}

function rejectRequest(clanId, userId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/clan/${clanId}/reject/${userId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        }
    })
        .then(res => res.ok ? res.text() : res.text().then(msg => { throw new Error(msg); }))
        .then(message => {
            alert(message);
            sessionStorage.setItem("justHandledTab", "pending");
            location.reload();
        })
        .catch(err => alert("에러: " + err.message));
}

function delegateLeader(clanId, userId) {
    if (!confirm("정말 이 멤버에게 리더를 위임하시겠습니까?")) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch(`/clan/${clanId}/delegate/${userId}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        }
    })
        .then(res => {
            if (!res.ok) throw new Error("위임 실패");
            return res.text();
        })
        .then(msg => {
            alert(msg);
            sessionStorage.setItem("justHandledTab", "members");
            location.reload();
        })
        .catch(err => alert(err.message));
}

function kickMember(clanId, memberId) {
    if (!confirm("정말 이 멤버를 추방하시겠습니까?")) return;

    fetch(`/clan/${clanId}/kick/${memberId}`, {
        method: "POST",
        headers: {
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    })
        .then(res => {
            if (!res.ok) throw new Error("추방 실패");
            return res.text();
        })
        .then(message => {
            alert(message);
            sessionStorage.setItem("justHandledTab", "members");
            location.reload();
        })
        .catch(err => alert(err));
}

function confirmLeaveClan(clanId) {
    if (!confirm("정말 탈퇴하시겠습니까?")) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    fetch(`/clan/${clanId}/leave`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
        }
    })
        .then(res => {
            if (!res.ok) throw new Error("탈퇴 실패");
            return res.text();
        })
        .then(msg => {
            alert(msg);
            window.location.href = `/clan/${clanId}`;
        })
        .catch(err => alert("에러 발생!\n" + err.message));
}

function confirmDelete() {
    if (confirm('정말로 클랜을 삭제하시겠습니까?')) {
        document.getElementById('deleteForm').submit();
    }
}

function handleJoinClick() {
    const isAuthenticated = document.getElementById("isAuthenticated")?.value === 'true';
    const userRole = document.getElementById("userRole")?.value;

    if (!isAuthenticated) {
        alert("로그인이 필요합니다!");
        window.location.href = "/login";
        return;
    }

    if (userRole === 'GUEST' || userRole === '') {
        openJoinClanModal();
    } else {
        alert("이미 클랜에 가입된 사용자입니다!");
    }
}
