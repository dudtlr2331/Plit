package com.plit.FO.qna;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/mypage/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @Value("${custom.upload-path}")
    private String uploadDir;

    @GetMapping("/write")
    public String showWriteForm(Model model) {
        model.addAttribute("viewSection", "qna");
        model.addAttribute("viewMode", "write");
        model.addAttribute("qnaDTO", new QnaDTO());
        return "fo/mypage/mypage";
    }

    @PostMapping("/write")
    public String writeQna(@ModelAttribute @Valid QnaDTO dto, BindingResult bindingResult, HttpSession session, RedirectAttributes ra, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("viewSection", "qna");
            model.addAttribute("viewMode", "write");
            model.addAttribute("qnaDTO", dto);
            return "fo/mypage/mypage";
        }

        Long userId = 1L;
//        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "pdf", "docx");
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

                if (!allowedExtensions.contains(extension)) {
                    ra.addFlashAttribute("error", "허용되지 않은 파일 형식입니다.");
                    return "redirect:/mypage/qna/write";
                }

                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String savedName = UUID.randomUUID() + "_" + originalFilename;
                File saveFile = new File(uploadDir, savedName);
                file.transferTo(saveFile);

//                dto.setContent(dto.getContent() + "\n\n[첨부파일: " + savedName + "]");
                dto.setFileName(savedName);

            } catch (IOException e) {
                e.printStackTrace();
                ra.addFlashAttribute("error", "파일 업로드 중 오류가 발생했습니다.");
                return "redirect:/mypage/qna/write";
            }
        }

        qnaService.saveQuestion(dto, userId);
        return "redirect:/mypage/qna/list";
    }

    @PostMapping("/delete/{id}")
    public String deleteQna(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Long userId = 1L;
//        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        qnaService.deleteQna(id, userId);
        return "redirect:/mypage/qna/list";
    }

    @GetMapping("/list")
    public String getMyQuestions(HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = 1L;
//        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        List<QnaEntity> myQuestions = qnaService.getMyQuestions(userId);
        model.addAttribute("viewSection", "qna");
        model.addAttribute("viewMode", "list");
        model.addAttribute("questions", myQuestions);
        return "fo/mypage/mypage";
    }

    @GetMapping("/view/{id}")
    public String viewQna(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = 1L;
//        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        QnaEntity qna = qnaService.findById(id);
        if (!qna.getUserId().equals(userId)) {
            ra.addFlashAttribute("error", "본인만 열람 가능합니다.");
            return "redirect:/mypage/qna/list";
        }
        model.addAttribute("viewSection", "qna");
        model.addAttribute("viewMode", "view");
        model.addAttribute("qna", qna);
        return "fo/mypage/mypage";
    }

    @GetMapping("/download/{fileName}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "파일 크기가 너무 큽니다! 최대 300MB까지 업로드 가능합니다.");
        return "redirect:/mypage/qna/write";
    }
}