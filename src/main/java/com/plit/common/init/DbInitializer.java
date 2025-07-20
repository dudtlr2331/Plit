package com.plit.common.init;

import com.plit.common.schema.SchemaSql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DbInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 전체 테이블 삭제 + 생성
    public void resetAllTables() {
        dropAllTables();
        createAllTables();
        System.out.println("DB 초기화 완료");
    }

    // 전체 테이블 삭제
    public void dropAllTables() {
        System.out.println("Dropping all tables...");
        executeMultiSql(SchemaSql.DROP_ALL);
        System.out.println("테이블 삭제 완료");
    }

    // 전체 테이블 생성
    public void createAllTables() {
        System.out.println("Creating all tables...");
        executeMultiSql(SchemaSql.CREATE_ALL);
        System.out.println("테이블 생성 완료");
    }

    // SQL 블럭을 세미콜론 기준으로 나눠 순차 실행
    private void executeMultiSql(String sqlBlock) {
        for (String sql : sqlBlock.split(";")) {
            String trimmed = sql.trim();
            if (!trimmed.isEmpty()) {
                try {
                    jdbcTemplate.execute(trimmed);
                } catch (Exception e) {
                    System.err.println("SQL 실행 실패: " + trimmed);
                    e.printStackTrace();
                }
            }
        }
    }
}