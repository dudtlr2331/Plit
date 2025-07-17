document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("qnaForm");
    if (form) {
        const title = document.getElementById("title");
        const category = document.getElementById("category");
        const content = document.getElementById("content");
        const privacy = document.getElementById("privacy");

        const titleError = document.getElementById("title-error");
        const categoryError = document.getElementById("category-error");
        const contentError = document.getElementById("content-error");
        const privacyError = document.getElementById("privacy-error");

        form.addEventListener("submit", function (e) {
            [titleError, categoryError, contentError, privacyError].forEach(err => err?.classList.remove("show"));

            if (title && title.value.trim() === "") {
                e.preventDefault();
                titleError?.classList.add("show");
                title.focus();
                return;
            }

            if (category && category.value === "") {
                e.preventDefault();
                categoryError?.classList.add("show");
                category.focus();
                return;
            }

            if (content && content.value.trim() === "") {
                e.preventDefault();
                contentError?.classList.add("show");
                content.focus();
                return;
            }

            if (privacy && !privacy.checked) {
                e.preventDefault();
                privacyError?.classList.add("show");
                privacy.focus();
                return;
            }
        });

        const fileInput = document.getElementById("file");
        if (fileInput) {
            fileInput.addEventListener("change", function () {
                previewQnaFile(this);
            });
        }
    }
});

function previewQnaFile(input) {
    const preview = document.getElementById("qna-file-preview");
    preview.innerHTML = "";

    const file = input.files[0];
    if (!file) return;

    const fileName = file.name.toLowerCase();
    const imageTypes = ['jpg', 'jpeg', 'png', 'gif'];
    const ext = fileName.split('.').pop();

    if (imageTypes.includes(ext)) {
        const reader = new FileReader();
        reader.onload = function (e) {
            const img = document.createElement("img");
            img.src = e.target.result;
            img.className = "qna-write-preview-image";
            preview.appendChild(img);
        };
        reader.readAsDataURL(file);
    } else {
        const info = document.createElement("p");
        info.textContent = "이미지 미리보기를 지원하지 않는 파일 형식입니다.";
        preview.appendChild(info);
    }
}