package com.jp.calefaction.service.repost;

import com.jp.calefaction.dto.repost.TopRepostersDTO;
import com.jp.calefaction.entity.repost.RepostCount;
import com.jp.calefaction.repository.repost.RepostCountRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class RepostCountService {

    private final RepostCountRepository repostCountRepository;

    public Mono<List<TopRepostersDTO>> getTopReposters(int num) {
        log.info("Getting the top reposters");
        return repostCountRepository
                .findTopReposters()
                .take(num) // Take only the first 'num' elements from the flux
                .collectList(); // Convert the flux to a Mono<List>
    }

    public Mono<RepostCount> save(RepostCount repostCount) {
        log.info("Attempting to save the repostCount - {}", repostCount);
        return repostCountRepository.save(repostCount);
    }
}
