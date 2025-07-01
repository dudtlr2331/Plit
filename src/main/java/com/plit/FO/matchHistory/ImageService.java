//package com.plit.FO.matchHistory;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.awt.*;
//
//@Service
//@RequiredArgsConstructor
//public class ImageService {
//
//    private final  ImageRepository imageRepository;
//
//    public String getImageUrl(String name, String type) {
//        return imageRepository.findByNameAndType(name, type)
//                .map(ImageEntity::getImageUrl)
//                .orElse("/img/default.png");
//    }
//
//    public void saveImage(String name, String type, String imageUrl) {
//        ImageEntity image = ImageEntity.builder()
//                .name(name)
//                .type(type)
//                .imageUrl(imageUrl)
//                .build();
//        imageRepository.save(image);
//    }
//}
//
