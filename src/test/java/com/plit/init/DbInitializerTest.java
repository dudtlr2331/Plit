package com.plit.init;

import com.plit.common.init.DbInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DbInitializerTest {

    @Autowired
    private DbInitializer dbInitializer;

    @Test
    void testDropAllTables() {
        dbInitializer.dropAllTables();
        System.out.println("테이블 삭제 테스트 통과");
    }

    @Test
    void testCreateAllTables() {
        dbInitializer.createAllTables();
        System.out.println("테이블 생성 테스트 통과");
    }

    @Test
    void testResetAllTables() {
        dbInitializer.resetAllTables();
        System.out.println("전체 초기화 테스트 통과");
    }
}