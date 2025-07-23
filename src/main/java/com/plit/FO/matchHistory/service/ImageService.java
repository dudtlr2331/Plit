package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.plit.FO.matchHistory.service.MatchHelper.SPELL_ID_TO_FILENAME;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final RestTemplate restTemplate;

    private static final Set<String> STATIC_TYPES = Set.of("spell", "tier", "trait", "position", "objective");

//    이미지 조회용

    // name, type -> DB 에서 이미지 전체 정보 조회
    public Optional<ImageEntity> getImage(String name, String type) {
        return imageRepository.findByNameAndType(name, type);
    }

    // name, type -> DB 에서 이미지 url 반환
    public String getImageUrl(String name, String type) {
        if ("spell".equals(type)) {
            String fileName = SPELL_ID_TO_FILENAME.getOrDefault(name, "default.png");
            return "/images/spell/" + fileName;
        }

        if (STATIC_TYPES.contains(type)) {
            return "/images/" + type + "/" + name;
        }

        return imageRepository.findByNameAndType(name, type)
                .map(ImageEntity::getImageUrl)
                .orElse("/images/default.png");
    }

    // 프로필 아이콘 이미지 url ( 프로필 아이콘 아이디 -> url ) : 입력값 정수
    public String getProfileIconUrl(Integer profileIconId) {
        if (profileIconId == null) return "/images/default.png";

        return getImageUrl(profileIconId + ".png", "profile-icon");
    }

    public List<String> getItemImageUrls(String itemIds) {
        if (itemIds == null || itemIds.isBlank()) return List.of();

        return Arrays.stream(itemIds.split(","))
                .map(id -> getImageUrl(id + ".png", "item"))
                .toList();
    }

    public List<String> getProfileIconUrls(List<String> iconIds) {
        if (iconIds == null) return List.of();

        return iconIds.stream()
                .map(id -> getImageUrl(id + ".png", "profile-icon"))
                .toList();
    }

    public List<String> getTraitImageUrls(List<String> traitIds) {
        if (traitIds == null) return List.of();

        return traitIds.stream()
                .map(id -> getImageUrl(id + ".png", "rune"))
                .toList();
    }




//    자동 동기화

    // 동기화 이미지 타입
    private static final List<String> IMAGE_TYPES = List.of("champion", "item", "rune", "profile-icon");

    @Scheduled(cron = "0 0 9 ? * WED") // 매주 수요일 오전 9시 자동 동기화
    public void updateAllImages() {
        String version = fetchLatestVersion();
        for (String type : IMAGE_TYPES) {
            updateImagesByType(type, version);
        }
    }

    // riot 최신 버전 가져오기
    public String fetchLatestVersion() {
        try {
            String[] versions = restTemplate.getForObject("https://ddragon.leagueoflegends.com/api/versions.json", String[].class);
            return versions != null && versions.length > 0 ? versions[0] : "";
        } catch (Exception e) {
            System.out.println("Riot 버전 정보를 불러오지 못했습니다.");
            return "";
        }
    }

    // riot 서버에서 타입별 이미지 url 가져오기
    private Map<String, String> fetchRiotImageUrls(String type, String version) {
        try {
            Map<String, String> map = new HashMap<>();

            switch (type) {
                case "champion" -> { // 챔피언 목록 json
                    String url = "https://ddragon.leagueoflegends.com/cdn/" + version + "/data/ko_KR/champion.json";
                    Map<String, Object> json = restTemplate.getForObject(url, Map.class);
                    Map<String, Object> data = (Map<String, Object>) json.get("data");

                    data.forEach((key, value) -> {
                        String fileName = key + ".png";
                        String imgUrl = "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/champion/" + fileName;
                        map.put(fileName, imgUrl);
                    });
                }
                case "item" -> { // 아이템
                    String url = "https://ddragon.leagueoflegends.com/cdn/" + version + "/data/ko_KR/item.json";
                    Map<String, Object> json = restTemplate.getForObject(url, Map.class);
                    Map<String, Object> data = (Map<String, Object>) json.get("data");

                    data.keySet().forEach(itemId -> {
                        String fileName = itemId + ".png";
                        String imgUrl = "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/item/" + fileName;
                        map.put(fileName, imgUrl);
                    });
                }
                case "rune" -> { // 룬
                    Path runeDir = Paths.get("src/main/resources/static/img/rune");

                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(runeDir, "*.png")) {
                        for (Path path : stream) {
                            String fileName = path.getFileName().toString();
                            String localUrl = "/img/rune/" + fileName;
                            map.put(fileName, localUrl);
                        }
                    } catch (IOException e) {
                        System.err.println("rune 폴더 로딩 실패: " + e.getMessage());
                    }
                }
                case "profile-icon" -> { // 프로필 아이콘
                    String url = "https://ddragon.leagueoflegends.com/cdn/" + version + "/data/en_US/profileicon.json";
                    Map<String, Object> json = restTemplate.getForObject(url, Map.class);
                    Map<String, Object> data = (Map<String, Object>) json.get("data");

                    for (String iconId : data.keySet()) {
                        String fileName = iconId + ".png";
                        String imgUrl = "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/profileicon/" + fileName;
                        map.put(fileName, imgUrl);
                    }
                }

            }

            return map;

        } catch (Exception e) {
            System.err.println("Riot 이미지 목록 로딩 실패 (" + type + ")");
            return null;
        }
    }

    // 로컬 이미지와 원격 이미지( riot ) 비교
    private boolean isSameImage(Path localPath, String remoteUrl) {
        try {
            byte[] local = Files.readAllBytes(localPath); // 로컬 이미지
            byte[] remote = restTemplate.getForObject(remoteUrl, byte[].class); // 라이엇 서버 이미지
            return Arrays.equals(local, remote);
        } catch (Exception e) {
            return false;
        }
    }

    // 폴더에 있는 .png 파일 이름 목록 반환 - 동적 이미지 등록 전 비교
    private Set<String> getFileNamesFromFolder(Path folderPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath, "*.png")) {
            Set<String> names = new HashSet<>();
            for (Path path : stream) {
                names.add(path.getFileName().toString());
            }
            return names;
        } catch (IOException e) {
            throw new RuntimeException("폴더 파일 읽기 실패: " + folderPath, e);
        }
    }

    // 특정 타입 이미지 동기화( 로컬 저장, DB 업데이트 )
    public void updateImagesByType(String type, String version) {
        Path folderPath = Paths.get("src/main/resources/static/images/" + type);
        if (!Files.exists(folderPath)) {
            System.out.println("[" + type + "] 폴더가 존재하지 않음 -> 생성함");
            try {
                Files.createDirectories(folderPath);
            } catch (IOException e) {
                System.err.println("폴더 생성 실패: " + folderPath);
                return;
            }
        }

        // riot 이미지 목록 가져오기
        Map<String, String> riotImageMap = fetchRiotImageUrls(type, version);
        if (riotImageMap == null) return;

        // DB에 저장된 이미지 정보
        List<ImageEntity> dbImages = imageRepository.findByType(type);
        Map<String, ImageEntity> dbMap = dbImages.stream().collect(Collectors.toMap(ImageEntity::getName, img -> img));

        for (Map.Entry<String, String> entry : riotImageMap.entrySet()) {
            String fileName = entry.getKey();
            String nameWithoutExtension = fileName.replaceAll("\\.png$", ""); // 확장자 제거
            String riotImageUrl = entry.getValue();
            String localImageUrl = "/img/" + type + "/" + fileName;

            Path filePath = folderPath.resolve(fileName);
            boolean needDownload = !Files.exists(filePath) || !isSameImage(filePath, riotImageUrl);

            if (needDownload) {
                try (InputStream in = new URL(riotImageUrl).openStream()) {
                    Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[다운로드] " + fileName);
                } catch (IOException e) {
                    System.err.println("[다운로드 실패] " + fileName + " - " + e.getMessage());
                }
            }

            // DB 에 저장 ( 없으면 추가, 있을 때 url 변경되면 수정 )
            ImageEntity image = dbMap.get(nameWithoutExtension);
            if (image == null) {
                imageRepository.save(new ImageEntity(nameWithoutExtension, type, localImageUrl));
                System.out.println("[DB 추가] " + fileName);
            } else if (!image.getImageUrl().equals(localImageUrl)) {
                image.setImageUrl(localImageUrl);
                imageRepository.save(image);
                System.out.println("[DB 수정] " + fileName);
            }
        }
    }

}
