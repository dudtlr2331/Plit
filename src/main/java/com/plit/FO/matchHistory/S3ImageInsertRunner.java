package com.plit.FO.matchHistory;

import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

@Component
@RequiredArgsConstructor
public class S3ImageInsertRunner implements CommandLineRunner {

    private final ImageRepository imageRepository;

    private final String bucketName = "plit-bucket";
    private final String region = "eu-central-1";
    private final String baseUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/";

    @Override
    public void run(String... args) {
//        S3Client s3 = S3Client.builder()
//                .region(Region.of(region))
//                .credentialsProvider(AnonymousCredentialsProvider.create())
//                .build();
//
//        System.out.println("S3ImageInsertRunner 실행 시작");
//
//        List<String> types = List.of("champion", "item", "rune", "profile-icon");
//
//        for (String type : types) {
//            String prefix = "img/" + type + "/";
//            String continuationToken = null;
//
//            do {
//                ListObjectsV2Request.Builder listReqBuilder = ListObjectsV2Request.builder()
//                        .bucket(bucketName)
//                        .prefix(prefix)
//                        .maxKeys(1000);
//
//                if (continuationToken != null) {
//                    listReqBuilder.continuationToken(continuationToken);
//                }
//
//                ListObjectsV2Response listRes = s3.listObjectsV2(listReqBuilder.build());
//
//                for (S3Object obj : listRes.contents()) {
//                    String key = obj.key();
//                    if (key.endsWith("/")) continue;
//
//                    String name = key.substring(key.lastIndexOf("/") + 1);
//                    String imageUrl = baseUrl + key;
//
//                    if (imageRepository.existsByNameAndType(name, type)) {
//                        System.out.println("[SKIP] 이미 존재: " + type + " / " + name);
//                        continue;
//                    }
//
//                    ImageEntity image = new ImageEntity(name, type, imageUrl);
//                    imageRepository.save(image);
//                    System.out.println("[INSERTED] " + type + " / " + name);
//                }
//
//                continuationToken = listRes.nextContinuationToken();
//            } while (continuationToken != null);
//        }
//
//
//        s3.close();
//        System.out.println("S3ImageInsertRunner 실행 완료");
    }
}
