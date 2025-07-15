const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

function sendCode() {
    const email = document.getElementById('emailInput').value.trim();
    if (!email) return alert("이메일을 입력해주세요.");

    fetch('/send-code', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...(csrfHeader && { [csrfHeader]: csrfToken })
        },
        body: new URLSearchParams({ email })
    })
        .then(res => res.ok ? res.text() : Promise.reject("인증번호 전송 실패"))
        .then(() => {
            document.getElementById('emailFinal').value = email;
            switchStep(2);
            startCooldown();
        })
        .catch(alert);
}

function resendCode() {
    const email = document.getElementById('emailFinal').value.trim();
    if (!email) return;

    fetch('/send-code', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...(csrfHeader && { [csrfHeader]: csrfToken })
        },
        body: new URLSearchParams({ email })
    })
        .then(() => {
            alert("인증번호가 재전송되었습니다.");
            startCooldown();
        })
        .catch(alert);
}

function verifyCode() {
    const email = document.getElementById('emailFinal').value.trim();
    const code = document.getElementById('codeInput').value.trim();

    fetch('/verify-code', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...(csrfHeader && { [csrfHeader]: csrfToken })
        },
        body: new URLSearchParams({ email, inputCode: code })
    })
        .then(res => res.text())
        .then(text => {
            if (text === "인증 성공") {
                switchStep(3);
            } else {
                alert("인증 실패: " + text);
            }
        });
}

function switchStep(step) {
    document.querySelectorAll('.form-step').forEach(el => el.classList.remove('active'));
    document.getElementById('step' + step).classList.add('active');
}

function startCooldown() {
    const btn = document.getElementById('resendBtn');
    let seconds = 180;
    btn.disabled = true;

    const timer = setInterval(() => {
        if (seconds <= 0) {
            clearInterval(timer);
            btn.textContent = "인증번호 재전송";
            btn.disabled = false;
        } else {
            btn.textContent = `재전송 ${seconds--}초`;
        }
    }, 1000);
}

function validateResetPassword() {
    const pwd = document.getElementById("newPwd").value.trim();
    const confirm = document.getElementById("confirmPwd").value.trim();

    if (pwd !== confirm) {
        alert("비밀번호가 일치하지 않습니다.");
        return false;
    }

    const hasLetter = /[a-zA-Z]/.test(pwd);
    const hasNumber = /[0-9]/.test(pwd);
    const hasSymbol = /[^a-zA-Z0-9]/.test(pwd);
    const typeCount = [hasLetter, hasNumber, hasSymbol].filter(Boolean).length;

    if (pwd.length < 8 || typeCount < 2 || pwd.includes("@") || pwd.includes(".com")) {
        alert("비밀번호는 8자 이상이며, 문자/숫자/기호 중 2가지 이상을 포함해야 하고 이메일을 포함할 수 없습니다.");
        return false;
    }

    return true;
}
