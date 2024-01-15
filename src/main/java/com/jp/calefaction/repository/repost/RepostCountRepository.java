package com.jp.calefaction.repository.repost;

import com.jp.calefaction.dto.repost.TopRepostersDTO;
import com.jp.calefaction.entity.repost.RepostCount;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface RepostCountRepository extends ReactiveCrudRepository<RepostCount, String> {

    Flux<RepostCount> findAllBySnowflakeId(String snowflakeId);

    @Query("SELECT new com.jp.calefaction.dto.repost.TopRepostersDTO(r.snowflakeId, COUNT(r)) "
            + "FROM RepostCount r GROUP BY r.snowflakeId ORDER BY COUNT(r) DESC")
    Flux<TopRepostersDTO> findTopReposters();
}
