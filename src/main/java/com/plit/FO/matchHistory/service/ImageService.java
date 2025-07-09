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

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // 동기화 이미지 타입
    private static final List<String> IMAGE_TYPES = List.of("champion", "item", "rune", "profile-icon");

    // name, type -> DB 에서 이미지 조회
    public Optional<ImageEntity> getImage(String name, String type) {
        return imageRepository.findByNameAndType(name, type);
    }

    // name, type -> DB 에서 이미지 url 반환
    public String getImageUrl(String name, String type) {
        return imageRepository.findByNameAndType(name, type)
                .map(ImageEntity::getImageUrl)
                .orElse("/images/default.png");
    }

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

    //
    private boolean isSameImage(Path localPath, String remoteUrl) {
        try {
            byte[] local = Files.readAllBytes(localPath); // 로컬 이미지
            byte[] remote = restTemplate.getForObject(remoteUrl, byte[].class); // 라이엇 서버 이미지
            return Arrays.equals(local, remote);
        } catch (Exception e) {
            return false;
        }
    }

    public String getProfileIconUrl(Integer profileIconId) {
        if (profileIconId == null) return "/images/default.png";

        String fileName = profileIconId + ".png";

        return imageRepository.findByNameAndType(fileName, "profile-icon")
                .map(ImageEntity::getImageUrl)
                .orElse("/images/default.png");
    }



    // 폴더에 있는 .png 파일 이름 목록 반환
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

    // static/images 폴더 이미지 DB에 등록 ( 중복 제외 ) -> 처음 DB 등록할때만 사용 용도 [ url 지정시 코드 바꿀 예정 ]
    public void bulkInsertFromFolder(String type) {
        Path folder = Paths.get("src/main/resources/static/images/" + type);
        if (!Files.exists(folder)) {
            System.err.println("폴더 없음: " + folder.toAbsolutePath());
            return;
        }

        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().toLowerCase().endsWith(".png"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String name = fileName.replaceAll("\\.png$", "");
                        String imageUrl = "/images/" + type + "/" + fileName;

                        if (!imageRepository.existsByNameAndType(name, type)) {
                            imageRepository.save(new ImageEntity(name, type, imageUrl));
                            System.out.println("[등록 완료] " + imageUrl);
                        } else {
                            System.out.println("[중복 스킵] " + imageUrl);
                        }
                    });
        } catch (IOException e) {
            System.err.println("[에러] 폴더 읽기 실패: " + e.getMessage());
        }
    }

}
