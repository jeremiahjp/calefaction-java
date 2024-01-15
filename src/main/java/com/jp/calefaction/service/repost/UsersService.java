package com.jp.calefaction.service.repost;

import com.jp.calefaction.entity.Users;
import com.jp.calefaction.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersService {

    private final UsersRepository usersRepository;

    public Mono<Users> save(Users users) {
        log.info("Saving the User into the database: {}", users);
        return usersRepository.save(users);
    }

    public Mono<Users> findById(String userId) {
        return usersRepository.findById(userId);
    }
}
