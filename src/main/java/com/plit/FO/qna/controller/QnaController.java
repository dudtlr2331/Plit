package com.plit.FO.qna.controller;

import com.plit.FO.block.dto.BlockDTO;
import com.plit.FO.qna.dto.QnaDTO;
import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.qna.service.QnaService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
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
    private final UserService userService;

    @Value("${custom.upload-path.qna}")
    private String uploadDir;

    @GetMapping("/write")
    public String showWriteForm(@ModelAttribute("loginUser") UserDTO loginUser, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("viewSection", "qna");
            model.addAttribute("viewMode", "write");
            model.addAttribute("qnaDTO", new QnaDTO());
            return "fo/mypage/mypage";
        } catch (Exception e) {
            System.err.println("QnA 작성 폼 로딩 중 오류: " + e.getMessage());
            ra.addFlashAttribute("error", "QnA 작성 화면을 여는 중 문제가 발생했습니다.");
            return "redirect:/mypage/qna/list";
        }
    }

    @PostMapping("/write")
    public String writeQna(@ModelAttribute("loginUser") UserDTO loginUser,
                           @ModelAttribute @Valid QnaDTO dto,
                           BindingResult bindingResult,
                           RedirectAttributes ra,
                           Model model) {
        try {
            Long userId = loginUser.getUserSeq().longValue();
            if (userId == null) {
                ra.addFlashAttribute("error", "로그인이 필요합니다.");
                return "redirect:/login";
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("viewSection", "qna");
                model.addAttribute("viewMode", "write");
                model.addAttribute("qnaDTO", dto);
                return "fo/mypage/mypage";
            }

            MultipartFile file = dto.getFile();
            if (file != null && !file.isEmpty()) {
                List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "pdf", "docx");
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

                if (!allowedExtensions.contains(extension)) {
                    ra.addFlashAttribute("error", "허용되지 않은 파일 형식입니다.");
                    return "redirect:/mypage/qna/write";
                }
            }

            qnaService.saveQuestion(dto, userId);
            return "redirect:/mypage/qna/list";

        } catch (Exception e) {
            System.err.println("QnA 작성 중 오류: " + e.getMessage());
            ra.addFlashAttribute("error", "문의 작성 중 문제가 발생했습니다. 다시 시도해주세요.");
            return "redirect:/mypage/qna/write";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteQna(@ModelAttribute("loginUser") UserDTO loginUser,
                            @PathVariable Long id,
                            RedirectAttributes ra) {
        try {
            Long userId = loginUser.getUserSeq().longValue();
            if (userId == null) {
                ra.addFlashAttribute("error", "로그인이 필요합니다.");
                return "redirect:/login";
            }

            qnaService.deleteQna(id, userId);
            ra.addFlashAttribute("success", "문의가 삭제되었습니다.");
            return "redirect:/mypage/qna/list";

        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage()); // 서비스에서 커스텀 메시지 넘기는 경우
            return "redirect:/mypage/qna/list";

        } catch (Exception e) {
            System.err.println("QnA 삭제 중 오류: " + e.getMessage());
            ra.addFlashAttribute("error", "문의 삭제 중 오류가 발생했습니다.");
            return "redirect:/mypage/qna/list";
        }
    }

    @GetMapping("/list")
    public String getMyQuestions(@ModelAttribute("loginUser") UserDTO loginUser,
                                 Model model, RedirectAttributes ra) {
        try {
            if (loginUser == null || loginUser.getUserSeq() == null) {
                ra.addFlashAttribute("error", "로그인이 필요합니다.");
                return "redirect:/login";
            }

            Long userId = loginUser.getUserSeq().longValue();
            List<QnaEntity> myQuestions = qnaService.getMyQuestions(userId);

            model.addAttribute("viewSection", "qna");
            model.addAttribute("viewMode", "list");
            model.addAttribute("questions", myQuestions);
            return "fo/mypage/mypage";

        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";

        } catch (Exception e) {
            System.err.println("QnA 리스트 로딩 중 오류: " + e.getMessage());
            ra.addFlashAttribute("error", "문의 목록을 불러오는 중 오류가 발생했습니다.");
            return "redirect:/mypage";
        }
    }

    @GetMapping("/view/{id}")
    public String viewQna(@ModelAttribute("loginUser") UserDTO loginUser,
                          @PathVariable Long id,
                          Model model,
                          RedirectAttributes ra) {
        try {
            if (loginUser == null || loginUser.getUserSeq() == null) {
                ra.addFlashAttribute("error", "로그인이 필요합니다.");
                return "redirect:/login";
            }

            Long userId = loginUser.getUserSeq().longValue();
            QnaEntity qna = qnaService.findById(id);

            if (qna == null) {
                ra.addFlashAttribute("error", "해당 문의글을 찾을 수 없습니다.");
                return "redirect:/mypage/qna/list";
            }

            if (qna.getUser() == null || !Long.valueOf(qna.getUser().getUserSeq()).equals(userId)) {
                ra.addFlashAttribute("error", "본인만 열람 가능합니다.");
                return "redirect:/mypage/qna/list";
            }

            model.addAttribute("viewSection", "qna");
            model.addAttribute("viewMode", "view");
            model.addAttribute("qna", qna);
            return "fo/mypage/mypage";

        } catch (Exception e) {
            System.err.println("QnA 상세 조회 오류: " + e.getMessage());
            ra.addFlashAttribute("error", "문의글을 불러오는 중 문제가 발생했습니다.");
            return "redirect:/mypage/qna/list";
        }
    }

    @GetMapping("/download/{fileName}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            if (fileName == null || fileName.contains("..")) {
                // 보안 체크
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                System.err.println("파일이 존재하지 않거나 읽을 수 없습니다: " + fileName);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (IOException e) {
            System.err.println("파일 경로 오류: " + e.getMessage());
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            System.err.println("파일 다운로드 실패: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(RedirectAttributes ra) {
        System.err.println("업로드 용량 초과 예외 발생: MaxUploadSizeExceededException");
        ra.addFlashAttribute("error", "파일 크기가 너무 큽니다! 최대 10MB까지 업로드 가능합니다.");
        return "redirect:/mypage/qna/write";
    }

    @Configuration
    public class WebMvcConfig implements WebMvcConfigurer {

        @Value("${custom.upload-path.qna}")
        private String qnaUploadPath;

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/uploads/qna/**")
                    .addResourceLocations("file:" + qnaUploadPath + "/");
        }
    }
}